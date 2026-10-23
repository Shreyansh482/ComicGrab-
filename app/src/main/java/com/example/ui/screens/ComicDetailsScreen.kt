package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chapter
import com.example.data.model.DownloadStatus
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.ComicGrabTopBar
import com.example.ui.components.formatBytes
import com.example.ui.theme.*
import java.io.File

@Composable
fun ComicDetailsScreen(
    comicId: Long,
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val comic by viewModel.comicRepository.observeComic(comicId).collectAsState(initial = null)
    val chapters by viewModel.comicRepository.getChaptersForComic(comicId).collectAsState(initial = emptyList())

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveProgress by remember { mutableStateOf(false) }
    var archiveProgress by remember { mutableFloatStateOf(0f) }
    var archiveMessage by remember { mutableStateOf<String?>(null) }

    if (comic == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealAccent)
        }
        return
    }

    val currentComic = comic!!
    val downloadedChapters = chapters.filter { it.isDownloaded }
    val firstReadableChapter = chapters.firstOrNull { it.id == currentComic.lastReadChapterId }
        ?: downloadedChapters.firstOrNull()
        ?: chapters.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComicGrabTopBar(
                title = currentComic.title,
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Comic", tint = ErrorRed)
                    }
                }
            )
        },
        containerColor = DeepForestBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardForest)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Cover
                        if (currentComic.coverPath != null && File(currentComic.coverPath).exists()) {
                            AsyncImage(
                                model = File(currentComic.coverPath),
                                contentDescription = currentComic.title,
                                modifier = Modifier
                                    .size(90.dp, 125.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(90.dp, 125.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = SageSurface, modifier = Modifier.size(40.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentComic.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PaleCream
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${currentComic.downloadedChaptersCount} downloaded of ${currentComic.totalChapters} chapters",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SageSurface)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Size: ${formatBytes(currentComic.totalSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TealAccent)
                            )

                            if (currentComic.lastReadChapterTitle != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Last read: ${currentComic.lastReadChapterTitle} (Page ${currentComic.lastReadPage + 1})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PaleCream)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (firstReadableChapter != null) {
                                viewModel.navigateTo(Screen.Reader(currentComic.id, firstReadableChapter.id))
                            }
                        },
                        enabled = firstReadableChapter != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_reading_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealAccent,
                            contentColor = DeepForestDarker
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentComic.lastReadChapterId != null) "Continue Reading" else "Start Reading",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (downloadedChapters.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                showArchiveProgress = true
                                val ids = downloadedChapters.map { it.id }
                                viewModel.createArchive(
                                    comicId = currentComic.id,
                                    chapterIds = ids,
                                    onProgress = { archiveProgress = it },
                                    onDone = { res ->
                                        showArchiveProgress = false
                                        res.onSuccess {
                                            archiveMessage = "Created ZIP: ${it.fileName}"
                                        }.onFailure {
                                            archiveMessage = "Archive error: ${it.message}"
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("create_zip_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PaleCream),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkGreen)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create ZIP")
                        }
                    }
                }
            }

            if (archiveMessage != null) {
                item {
                    Text(
                        text = archiveMessage!!,
                        color = TealAccent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Chapters List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters (${chapters.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )

                    val notDownloadedCount = chapters.count { !it.isDownloaded }
                    if (notDownloadedCount > 0) {
                        TextButton(
                            onClick = {
                                val toDownload = chapters.filter { !it.isDownloaded }.map { it.id }
                                viewModel.downloadManager.enqueueChapters(toDownload)
                                viewModel.navigateTo(Screen.DownloadQueue)
                            }
                        ) {
                            Text("Download All ($notDownloadedCount)", color = TealAccent)
                        }
                    }
                }
            }

            items(chapters, key = { it.id }) { chapter ->
                ChapterRowItem(
                    chapter = chapter,
                    onRead = {
                        viewModel.navigateTo(Screen.Reader(currentComic.id, chapter.id))
                    },
                    onDownload = {
                        viewModel.downloadManager.enqueueChapter(chapter.id)
                        viewModel.navigateTo(Screen.DownloadQueue)
                    },
                    onDelete = {
                        viewModel.deleteChapter(chapter)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Comic?", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will delete '${currentComic.title}' and all its downloaded chapter images from your private library. Saved ZIP archives will not be deleted.",
                    color = SageSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteComic(currentComic)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = PaleCream)
                }
            },
            containerColor = CardForest
        )
    }

    // Archive creation progress dialog
    if (showArchiveProgress) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Creating ZIP Archive", color = PaleCream) },
            text = {
                Column {
                    Text("Packaging downloaded chapters into ZIP format...", color = SageSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { archiveProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = TealAccent
                    )
                }
            },
            confirmButton = {},
            containerColor = CardForest
        )
    }
}

@Composable
fun ChapterRowItem(
    chapter: Chapter,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (chapter.isDownloaded) onRead() else onDownload()
            }
            .border(1.dp, CardForestBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardForest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chapter ${chapter.chapterNumber.toInt().toString().padStart(3, '0')}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    if (chapter.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Read",
                            tint = TealAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium.copy(color = SageSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (chapter.isDownloaded) {
                        "${chapter.pageCount} pages • ${formatBytes(chapter.sizeBytes)}"
                    } else if (chapter.downloadStatus == DownloadStatus.DOWNLOADING.name) {
                        "Downloading ${(chapter.downloadProgress * 100).toInt()}%"
                    } else {
                        "Not downloaded"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (chapter.isDownloaded) TealAccent else MutedText
                    )
                )
            }

            // Action
            if (chapter.isDownloaded) {
                IconButton(onClick = onRead) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Read Chapter", tint = TealAccent)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete files", tint = MutedText)
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download Chapter", tint = SageSurface)
                }
            }
        }
    }
}
