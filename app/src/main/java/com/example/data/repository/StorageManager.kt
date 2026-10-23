package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageStats(
    val comicImagesBytes: Long,
    val zipArchivesBytes: Long,
    val tempFilesBytes: Long,
    val totalUsedBytes: Long,
    val availableBytes: Long,
    val totalDeviceBytes: Long
)

class StorageManager(private val context: Context) {

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val libraryDir = File(context.filesDir, "library")
        val archivesDir = File(context.filesDir, "archives")
        val cacheDir = context.cacheDir
        val tempDir = File(context.filesDir, "temp")

        val comicImagesBytes = calculateDirSize(libraryDir)
        val zipArchivesBytes = calculateDirSize(archivesDir)
        val tempFilesBytes = calculateDirSize(cacheDir) + calculateDirSize(tempDir)
        val totalUsedBytes = comicImagesBytes + zipArchivesBytes + tempFilesBytes

        var availableBytes = 0L
        var totalDeviceBytes = 0L

        try {
            val stat = StatFs(context.filesDir.absolutePath)
            availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            totalDeviceBytes = stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {}

        StorageStats(
            comicImagesBytes = comicImagesBytes,
            zipArchivesBytes = zipArchivesBytes,
            tempFilesBytes = tempFilesBytes,
            totalUsedBytes = totalUsedBytes,
            availableBytes = availableBytes,
            totalDeviceBytes = totalDeviceBytes
        )
    }

    suspend fun cleanTemporaryFiles(): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        val cacheDir = context.cacheDir
        val tempDir = File(context.filesDir, "temp")

        reclaimed += deleteContents(cacheDir)
        reclaimed += deleteContents(tempDir)
        reclaimed
    }

    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }

    private fun deleteContents(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var reclaimed = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isDirectory) {
                reclaimed += deleteContents(f)
                f.delete()
            } else {
                val len = f.length()
                if (f.delete()) {
                    reclaimed += len
                }
            }
        }
        return reclaimed
    }
}
