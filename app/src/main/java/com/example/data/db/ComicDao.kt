package com.example.data.db

import androidx.room.*
import com.example.data.model.Chapter
import com.example.data.model.Comic
import com.example.data.model.ZipArchive
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics ORDER BY updatedAt DESC")
    fun getAllComics(): Flow<List<Comic>>

    @Query("SELECT * FROM comics WHERE id = :id LIMIT 1")
    suspend fun getComicById(id: Long): Comic?

    @Query("SELECT * FROM comics WHERE id = :id LIMIT 1")
    fun observeComicById(id: Long): Flow<Comic?>

    @Query("SELECT * FROM comics WHERE sourceUrl = :url LIMIT 1")
    suspend fun getComicByUrl(url: String): Comic?

    @Query("SELECT * FROM comics WHERE lastReadChapterId IS NOT NULL ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentReadingComics(): Flow<List<Comic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComic(comic: Comic): Long

    @Update
    suspend fun updateComic(comic: Comic)

    @Delete
    suspend fun deleteComic(comic: Comic)

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteComicById(id: Long)

    @Query("SELECT COUNT(*) FROM comics")
    fun getComicCount(): Flow<Int>
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE comicId = :comicId ORDER BY chapterNumber ASC")
    fun getChaptersForComic(comicId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE comicId = :comicId ORDER BY chapterNumber ASC")
    suspend fun getChaptersListForComic(comicId: Long): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: Long): Chapter?

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun observeChapterById(id: Long): Flow<Chapter?>

    @Query("SELECT * FROM chapters WHERE downloadStatus IN ('QUEUED', 'DOWNLOADING') ORDER BY id ASC")
    fun getActiveDownloads(): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE downloadStatus != 'NONE' ORDER BY updatedAt DESC")
    fun getAllQueueChapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE downloadStatus = 'DOWNLOADING' LIMIT 1")
    suspend fun getCurrentDownloadingChapter(): Chapter?

    @Query("SELECT * FROM chapters WHERE downloadStatus = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueuedChapter(): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>): List<Long>

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Query("UPDATE chapters SET downloadStatus = :status, downloadProgress = :progress, errorMessage = :error WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: String, progress: Float, error: String? = null)

    @Query("UPDATE chapters SET isDownloaded = 1, downloadStatus = 'COMPLETED', downloadProgress = 1.0, folderPath = :folder, sizeBytes = :size, pageCount = :pages, updatedAt = :time WHERE id = :id")
    suspend fun markChapterCompleted(id: Long, folder: String, size: Long, pages: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE chapters SET isRead = :isRead, lastReadPage = :page, lastReadOffset = :offset, updatedAt = :time WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, isRead: Boolean, page: Int, offset: Int, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("SELECT COUNT(*) FROM chapters WHERE isDownloaded = 1")
    fun getDownloadedChapterCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chapters WHERE downloadStatus IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveDownloadsCount(): Flow<Int>

    @Query("SELECT * FROM chapters WHERE comicId = :comicId AND chapterNumber > :currentNumber ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getNextChapter(comicId: Long, currentNumber: Float): Chapter?

    @Query("SELECT * FROM chapters WHERE comicId = :comicId AND chapterNumber < :currentNumber ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getPreviousChapter(comicId: Long, currentNumber: Float): Chapter?
}

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives ORDER BY createdAt DESC")
    fun getAllArchives(): Flow<List<ZipArchive>>

    @Query("SELECT * FROM archives WHERE id = :id LIMIT 1")
    suspend fun getArchiveById(id: Long): ZipArchive?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: ZipArchive): Long

    @Delete
    suspend fun deleteArchive(archive: ZipArchive)

    @Query("DELETE FROM archives WHERE id = :id")
    suspend fun deleteArchiveById(id: Long)

    @Query("SELECT COUNT(*) FROM archives")
    fun getArchiveCount(): Flow<Int>
}
