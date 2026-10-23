package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.StatFs
import com.example.data.db.ChapterDao
import com.example.data.db.ComicDao
import com.example.data.model.Chapter
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val comicDao: ComicDao,
    private val chapterDao: ChapterDao,
    private val webScraper: WebScraper
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private var maxConcurrency = 3
    private var semaphore = Semaphore(maxConcurrency)
    private var jpegQuality = 90

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun setMaxConcurrency(value: Int) {
        val clamped = value.coerceIn(1, 5)
        if (clamped != maxConcurrency) {
            maxConcurrency = clamped
            semaphore = Semaphore(maxConcurrency)
        }
    }

    fun setJpegQuality(quality: Int) {
        jpegQuality = quality.coerceIn(50, 100)
    }

    fun getJpegQuality(): Int = jpegQuality
    fun getMaxConcurrency(): Int = maxConcurrency

    fun enqueueChapter(chapterId: Long) {
        scope.launch {
            val chapter = chapterDao.getChapterById(chapterId) ?: return@launch
            chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.QUEUED.name, 0f)
            DownloadDiagnostics.log("QUEUE", "Enqueued Chapter ${chapter.chapterNumber.toInt()}: ${chapter.title}")
            processQueue()
        }
    }

    fun enqueueChapters(chapterIds: List<Long>) {
        scope.launch {
            for (id in chapterIds) {
                chapterDao.updateDownloadStatus(id, DownloadStatus.QUEUED.name, 0f)
            }
            DownloadDiagnostics.log("QUEUE", "Enqueued ${chapterIds.size} chapters for download")
            processQueue()
        }
    }

    fun pauseDownload(chapterId: Long) {
        val job = activeJobs.remove(chapterId)
        job?.cancel()
        scope.launch {
            chapterDao.updateDownloadStatus(chapterId, DownloadStatus.PAUSED.name, 0f)
            DownloadDiagnostics.log("QUEUE", "Paused download for chapter ID $chapterId")
        }
    }

    fun resumeDownload(chapterId: Long) {
        enqueueChapter(chapterId)
    }

    fun cancelDownload(chapterId: Long) {
        val job = activeJobs.remove(chapterId)
        job?.cancel()
        scope.launch {
            chapterDao.updateDownloadStatus(chapterId, DownloadStatus.NONE.name, 0f)
            DownloadDiagnostics.log("QUEUE", "Cancelled download for chapter ID $chapterId")
        }
    }

    fun retryDownload(chapterId: Long) {
        enqueueChapter(chapterId)
    }

    private fun processQueue() {
        scope.launch {
            val next = chapterDao.getNextQueuedChapter() ?: return@launch
            if (activeJobs.containsKey(next.id)) return@launch

            val job = launch {
                semaphore.acquire()
                try {
                    downloadChapterInternal(next)
                } finally {
                    semaphore.release()
                    activeJobs.remove(next.id)
                    processQueue()
                }
            }
            activeJobs[next.id] = job
        }
    }

    private suspend fun downloadChapterInternal(chapter: Chapter) {
        DownloadDiagnostics.log("DOWNLOAD_START", "Starting download: Chapter ${chapter.chapterNumber.toInt()} (${chapter.title})")
        chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.DOWNLOADING.name, 0.05f)

        val comic = comicDao.getComicById(chapter.comicId)
        if (comic == null) {
            val err = "Comic record not found in database."
            DownloadDiagnostics.log("DOWNLOAD_ERROR", err, isError = true)
            chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.FAILED.name, 0f, err)
            return
        }

        // 1. Verify Storage Space (Minimum 25MB safety buffer)
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            if (availableBytes < 25 * 1024 * 1024L) {
                val err = "Insufficient storage space on device (requires at least 25 MB free)."
                DownloadDiagnostics.log("STORAGE_ERROR", err, isError = true)
                chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.FAILED.name, 0f, err)
                return
            }
        } catch (_: Exception) {}

        try {
            // 2. Discover Image URLs
            chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.DOWNLOADING.name, 0.1f)
            val imageUrls = webScraper.extractChapterImageUrls(chapter.sourceUrl)

            if (imageUrls.isEmpty()) {
                val err = "No comic images found on chapter webpage. The website may require JavaScript or restricts direct image access."
                DownloadDiagnostics.log("IMAGE_EXTRACTION_FAIL", err, isError = true, details = "Chapter URL: ${chapter.sourceUrl}")
                chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.FAILED.name, 0f, err)
                return
            }

            // 3. Prepare Internal Storage Directory: Library/<Comic_Name>/Chapter_001
            val sanitizedComic = sanitizeFileName(comic.title)
            val chapterFolderName = String.format(Locale.US, "Chapter_%03d", chapter.chapterNumber.toInt())
            val libraryDir = File(context.filesDir, "library")
            val comicDir = File(libraryDir, sanitizedComic)
            val chapterDir = File(comicDir, chapterFolderName)

            if (!chapterDir.exists()) {
                chapterDir.mkdirs()
            }

            var totalBytes = 0L
            val totalImages = imageUrls.size
            var downloadedCount = 0
            var lastError: String? = null

            // 4. Download and convert images
            for ((index, imgUrl) in imageUrls.withIndex()) {
                currentCoroutineContext().ensureActive()
                val targetFile = File(chapterDir, String.format(Locale.US, "image_%03d.jpg", index + 1))

                // If already downloaded and valid, reuse
                if (targetFile.exists() && targetFile.length() > 500) {
                    downloadedCount++
                    totalBytes += targetFile.length()
                } else {
                    val result = downloadAndSaveAsJpeg(imgUrl, targetFile, chapter.sourceUrl)
                    if (result.isSuccess) {
                        downloadedCount++
                        totalBytes += targetFile.length()
                    } else {
                        lastError = result.exceptionOrNull()?.message
                        DownloadDiagnostics.log(
                            "IMAGE_DOWNLOAD_FAIL",
                            "Failed image ${index + 1}/$totalImages: $lastError",
                            isError = true,
                            details = "URL: $imgUrl"
                        )
                    }
                }

                val progress = 0.1f + (0.9f * ((index + 1).toFloat() / totalImages.toFloat()))
                chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.DOWNLOADING.name, progress)
            }

            if (downloadedCount == 0) {
                val err = lastError ?: "Failed to download chapter images from the server."
                chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.FAILED.name, 0f, err)
                DownloadDiagnostics.log("DOWNLOAD_FAILED", "Chapter ${chapter.chapterNumber.toInt()} failed: $err", isError = true)
                return
            }

            // 5. Mark Chapter Completed
            chapterDao.markChapterCompleted(
                id = chapter.id,
                folder = chapterDir.absolutePath,
                size = totalBytes,
                pages = downloadedCount
            )

            // Update Comic Cover and total stats
            val firstImage = File(chapterDir, "image_001.jpg")
            val updatedCover = if (comic.coverPath == null && firstImage.exists()) firstImage.absolutePath else comic.coverPath

            val allChapters = chapterDao.getChaptersListForComic(comic.id)
            val downloadedCountTotal = allChapters.count { it.isDownloaded || it.id == chapter.id }
            val totalSizeBytes = allChapters.sumOf { if (it.id == chapter.id) totalBytes else it.sizeBytes }

            comicDao.updateComic(
                comic.copy(
                    coverPath = updatedCover,
                    downloadedChaptersCount = downloadedCountTotal,
                    totalSizeBytes = totalSizeBytes,
                    updatedAt = System.currentTimeMillis()
                )
            )

            DownloadDiagnostics.log(
                "DOWNLOAD_COMPLETE",
                "Successfully saved Chapter ${chapter.chapterNumber.toInt()} ($downloadedCount pages, $totalBytes bytes)"
            )

        } catch (e: CancellationException) {
            chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.PAUSED.name, 0f)
            DownloadDiagnostics.log("DOWNLOAD_PAUSED", "Download paused for Chapter ${chapter.chapterNumber.toInt()}")
        } catch (e: Exception) {
            val err = e.message ?: "Download was interrupted."
            chapterDao.updateDownloadStatus(chapter.id, DownloadStatus.FAILED.name, 0f, err)
            DownloadDiagnostics.log("DOWNLOAD_EXCEPTION", "Error downloading chapter: $err", isError = true, details = e.stackTraceToString())
        }
    }

    private suspend fun downloadAndSaveAsJpeg(
        url: String,
        targetFile: File,
        refererUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", WebScraper.BROWSER_USER_AGENT)
                .header("Referer", refererUrl)
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .header("Sec-Fetch-Dest", "image")
                .header("Sec-Fetch-Mode", "no-cors")
                .header("Sec-Fetch-Site", "cross-site")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${response.code} downloading image"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Server returned empty image body"))
            val contentType = body.contentType()?.toString()?.lowercase().orEmpty()

            // Verify content type is not an error page
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                return@withContext Result.failure(Exception("Server returned an HTML/JSON error page instead of image bytes"))
            }

            // Read into byte array
            val bytes = body.bytes()
            if (bytes.size < 100) {
                return@withContext Result.failure(Exception("Image file is too small or empty (${bytes.size} bytes)"))
            }

            // Decode image
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(Exception("Image could not be decoded. Unsupported or corrupt image format."))

            // Genuine JPEG conversion: composite onto a white background (cleans transparency & converts WebP/PNG/GIF to standard JPEG)
            val width = rawBitmap.width
            val height = rawBitmap.height
            val composited = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(composited)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(rawBitmap, 0f, 0f, null)

            // Write to temporary file first for atomic finalization
            FileOutputStream(tmpFile).use { out ->
                composited.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.flush()
            }

            rawBitmap.recycle()
            composited.recycle()

            // Atomic rename to final target file
            if (tmpFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                val renamed = tmpFile.renameTo(targetFile)
                if (!renamed) {
                    // Fallback copy if rename fails
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (tmpFile.exists()) tmpFile.delete()
            Result.failure(e)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
    }
}
