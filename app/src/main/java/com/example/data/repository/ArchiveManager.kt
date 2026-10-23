package com.example.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.db.ArchiveDao
import com.example.data.db.ChapterDao
import com.example.data.db.ComicDao
import com.example.data.model.ZipArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveManager(
    private val context: Context,
    private val comicDao: ComicDao,
    private val chapterDao: ChapterDao,
    private val archiveDao: ArchiveDao
) {
    private val archivesDir = File(context.filesDir, "archives").apply {
        if (!exists()) mkdirs()
    }

    suspend fun createZipArchive(
        comicId: Long,
        chapterIds: List<Long>,
        onProgress: (Float) -> Unit
    ): Result<ZipArchive> = withContext(Dispatchers.IO) {
        try {
            val comic = comicDao.getComicById(comicId)
                ?: return@withContext Result.failure(Exception("Comic not found"))

            val allChapters = chapterDao.getChaptersListForComic(comicId)
            val selectedChapters = allChapters.filter { chapterIds.contains(it.id) && it.isDownloaded }

            if (selectedChapters.isEmpty()) {
                return@withContext Result.failure(Exception("No downloaded chapters selected to archive"))
            }

            val sanitizedComic = comic.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val minChapter = selectedChapters.minOf { it.chapterNumber }.toInt()
            val maxChapter = selectedChapters.maxOf { it.chapterNumber }.toInt()
            val fileName = if (selectedChapters.size == 1) {
                String.format(Locale.US, "%s_Ch_%03d.zip", sanitizedComic, minChapter)
            } else {
                String.format(Locale.US, "%s_Ch_%03d-%03d.zip", sanitizedComic, minChapter, maxChapter)
            }

            val zipFile = File(archivesDir, fileName)
            if (zipFile.exists()) {
                zipFile.delete()
            }

            // Collect all image files
            val filesToZip = mutableListOf<Pair<String, File>>() // zipEntryPath to File
            for (chapter in selectedChapters) {
                val chapterFolder = File(chapter.folderPath)
                if (chapterFolder.exists() && chapterFolder.isDirectory) {
                    val images = chapterFolder.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) || f.extension.equals("jpeg", ignoreCase = true) }
                        ?.sortedBy { it.name } ?: emptyList()

                    val entryFolder = String.format(Locale.US, "Chapter_%03d", chapter.chapterNumber.toInt())
                    for (img in images) {
                        filesToZip.add(Pair("$entryFolder/${img.name}", img))
                    }
                }
            }

            if (filesToZip.isEmpty()) {
                return@withContext Result.failure(Exception("Selected chapters do not contain downloaded JPEG images"))
            }

            val totalFiles = filesToZip.size
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val buffer = ByteArray(8192)
                for ((index, pair) in filesToZip.withIndex()) {
                    val (entryPath, file) = pair
                    val entry = ZipEntry(entryPath)
                    zos.putNextEntry(entry)

                    BufferedInputStream(FileInputStream(file)).use { bis ->
                        var read: Int
                        while (bis.read(buffer).also { read = it } != -1) {
                            zos.write(buffer, 0, read)
                        }
                    }
                    zos.closeEntry()

                    val progress = (index + 1).toFloat() / totalFiles.toFloat()
                    onProgress(progress)
                }
            }

            // Verify ZIP integrity
            val isValid = verifyZipIntegrity(zipFile, filesToZip.size)
            if (!isValid) {
                zipFile.delete()
                return@withContext Result.failure(Exception("Integrity check failed on generated ZIP file"))
            }

            val record = ZipArchive(
                comicId = comic.id,
                comicTitle = comic.title,
                fileName = fileName,
                filePath = zipFile.absolutePath,
                sizeBytes = zipFile.length(),
                chapterCount = selectedChapters.size,
                createdAt = System.currentTimeMillis()
            )

            val archiveId = archiveDao.insertArchive(record)
            Result.success(record.copy(id = archiveId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun verifyZipIntegrity(zipFile: File, expectedEntryCount: Int): Boolean {
        return try {
            ZipFile(zipFile).use { zf ->
                zf.size() >= expectedEntryCount
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteArchive(archive: ZipArchive): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(archive.filePath)
            if (file.exists()) {
                file.delete()
            }
            archiveDao.deleteArchive(archive)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareOrExportArchive(context: Context, archive: ZipArchive) {
        val file = File(archive.filePath)
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, archive.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export ${archive.fileName}"))
        } catch (e: Exception) {
            // Intent fallback
        }
    }
}
