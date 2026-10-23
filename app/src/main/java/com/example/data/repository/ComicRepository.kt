package com.example.data.repository

import android.content.Context
import com.example.data.db.ArchiveDao
import com.example.data.db.ChapterDao
import com.example.data.db.ComicDao
import com.example.data.model.Chapter
import com.example.data.model.Comic
import com.example.data.model.DiscoveredComic
import com.example.data.model.ZipArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ComicRepository(
    private val context: Context,
    private val comicDao: ComicDao,
    private val chapterDao: ChapterDao,
    private val archiveDao: ArchiveDao
) {
    val allComics: Flow<List<Comic>> = comicDao.getAllComics()
    val recentReadingComics: Flow<List<Comic>> = comicDao.getRecentReadingComics()
    val activeDownloads: Flow<List<Chapter>> = chapterDao.getActiveDownloads()
    val allQueueChapters: Flow<List<Chapter>> = chapterDao.getAllQueueChapters()
    val allArchives: Flow<List<ZipArchive>> = archiveDao.getAllArchives()

    val totalComicCount: Flow<Int> = comicDao.getComicCount()
    val downloadedChapterCount: Flow<Int> = chapterDao.getDownloadedChapterCount()
    val activeDownloadsCount: Flow<Int> = chapterDao.getActiveDownloadsCount()
    val archiveCount: Flow<Int> = archiveDao.getArchiveCount()

    fun getChaptersForComic(comicId: Long): Flow<List<Chapter>> = chapterDao.getChaptersForComic(comicId)

    fun observeComic(comicId: Long): Flow<Comic?> = comicDao.observeComicById(comicId)

    fun observeChapter(chapterId: Long): Flow<Chapter?> = chapterDao.observeChapterById(chapterId)

    suspend fun getComic(comicId: Long): Comic? = comicDao.getComicById(comicId)

    suspend fun getChapter(chapterId: Long): Chapter? = chapterDao.getChapterById(chapterId)

    suspend fun saveDiscoveredComic(discovered: DiscoveredComic): Pair<Long, List<Chapter>> = withContext(Dispatchers.IO) {
        val existing = comicDao.getComicByUrl(discovered.sourceUrl)
        val comicId: Long
        if (existing != null) {
            comicId = existing.id
            comicDao.updateComic(
                existing.copy(
                    title = discovered.title,
                    totalChapters = discovered.chapters.size,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val newComic = Comic(
                title = discovered.title,
                sourceUrl = discovered.sourceUrl,
                coverPath = discovered.coverUrl,
                totalChapters = discovered.chapters.size,
                downloadedChaptersCount = 0
            )
            comicId = comicDao.insertComic(newComic)
        }

        val existingChapters = chapterDao.getChaptersListForComic(comicId)
        val existingMap = existingChapters.associateBy { it.chapterNumber }

        val chaptersToInsert = mutableListOf<Chapter>()
        for (dc in discovered.chapters) {
            val exist = existingMap[dc.number]
            if (exist == null) {
                chaptersToInsert.add(
                    Chapter(
                        comicId = comicId,
                        chapterNumber = dc.number,
                        title = dc.title,
                        sourceUrl = dc.url
                    )
                )
            }
        }

        if (chaptersToInsert.isNotEmpty()) {
            chapterDao.insertChapters(chaptersToInsert)
        }

        val finalChapters = chapterDao.getChaptersListForComic(comicId)
        Pair(comicId, finalChapters)
    }

    suspend fun saveReadingProgress(
        comicId: Long,
        chapterId: Long,
        page: Int,
        offset: Int,
        isCompleted: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val chapter = chapterDao.getChapterById(chapterId) ?: return@withContext
        val comic = comicDao.getComicById(comicId) ?: return@withContext

        chapterDao.updateReadingProgress(
            id = chapterId,
            isRead = isCompleted || chapter.isRead,
            page = page,
            offset = offset
        )

        comicDao.updateComic(
            comic.copy(
                lastReadChapterId = chapterId,
                lastReadChapterTitle = chapter.title,
                lastReadPage = page,
                lastReadOffset = offset,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setComicReadingPreferences(
        comicId: Long,
        mode: String?,
        direction: String?
    ) = withContext(Dispatchers.IO) {
        val comic = comicDao.getComicById(comicId) ?: return@withContext
        comicDao.updateComic(
            comic.copy(
                preferredReadingMode = mode,
                preferredReadingDirection = direction
            )
        )
    }

    suspend fun deleteComic(comic: Comic) = withContext(Dispatchers.IO) {
        // Delete comic folder from library
        val sanitizedComic = comic.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val comicDir = File(File(context.filesDir, "library"), sanitizedComic)
        if (comicDir.exists()) {
            comicDir.deleteRecursively()
        }
        comicDao.deleteComic(comic)
    }

    suspend fun deleteChapterFiles(chapter: Chapter) = withContext(Dispatchers.IO) {
        val folder = File(chapter.folderPath)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        chapterDao.updateChapter(
            chapter.copy(
                isDownloaded = false,
                downloadStatus = "NONE",
                downloadProgress = 0f,
                sizeBytes = 0L,
                folderPath = ""
            )
        )

        // Update comic count
        val allChapters = chapterDao.getChaptersListForComic(chapter.comicId)
        val downloaded = allChapters.count { it.isDownloaded && it.id != chapter.id }
        val totalSize = allChapters.filter { it.id != chapter.id }.sumOf { it.sizeBytes }
        val comic = comicDao.getComicById(chapter.comicId)
        if (comic != null) {
            comicDao.updateComic(comic.copy(downloadedChaptersCount = downloaded, totalSizeBytes = totalSize))
        }
    }

    suspend fun getNextChapter(comicId: Long, currentNumber: Float): Chapter? =
        chapterDao.getNextChapter(comicId, currentNumber)

    suspend fun getPreviousChapter(comicId: Long, currentNumber: Float): Chapter? =
        chapterDao.getPreviousChapter(comicId, currentNumber)
}
