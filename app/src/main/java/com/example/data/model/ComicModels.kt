package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comics",
    indices = [Index(value = ["sourceUrl"], unique = true)]
)
data class Comic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val coverPath: String? = null,
    val totalChapters: Int = 0,
    val downloadedChaptersCount: Int = 0,
    val lastReadChapterId: Long? = null,
    val lastReadChapterTitle: String? = null,
    val lastReadPage: Int = 0,
    val lastReadOffset: Int = 0,
    val preferredReadingMode: String? = null, // "webtoon" or "manga"
    val preferredReadingDirection: String? = null, // "ltr" or "rtl"
    val totalSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Comic::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["comicId"]),
        Index(value = ["comicId", "chapterNumber"], unique = true)
    ]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val comicId: Long,
    val chapterNumber: Float,
    val title: String,
    val sourceUrl: String,
    val folderPath: String = "",
    val pageCount: Int = 0,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val sizeBytes: Long = 0L,
    val downloadStatus: String = DownloadStatus.NONE.name,
    val errorMessage: String? = null,
    val isRead: Boolean = false,
    val lastReadPage: Int = 0,
    val lastReadOffset: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "archives",
    indices = [Index(value = ["filePath"], unique = true)]
)
data class ZipArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val comicId: Long? = null,
    val comicTitle: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long = 0L,
    val chapterCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    NONE,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class ReadingMode(val label: String) {
    WEBTOON("Webtoon (Continuous)"),
    MANGA("Manga (Page by Page)")
}

enum class ReadingDirection(val label: String) {
    LTR("Left to Right"),
    RTL("Right to Left")
}

data class DiscoveredChapter(
    val number: Float,
    val title: String,
    val url: String,
    val estimatedImages: Int = 0
)

data class DiscoveredComic(
    val title: String,
    val sourceUrl: String,
    val coverUrl: String? = null,
    val chapters: List<DiscoveredChapter> = emptyList()
)
