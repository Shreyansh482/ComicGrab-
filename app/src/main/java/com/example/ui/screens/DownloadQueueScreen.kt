package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chapter
import com.example.data.model.DownloadStatus
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatBytes
import com.example.ui.theme.*

@Composable
fun DownloadQueueScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val allQueue by viewModel.queueChapters.collectAsState()

    val filteredList = remember(allQueue, selectedFilter) {
        when (selectedFilter) {
            "Downloading" -> allQueue.filter { it.downloadStatus == DownloadStatus.DOWNLOADING.name }
            "Queued" -> allQueue.filter { it.downloadStatus == DownloadStatus.QUEUED.name }
            "Completed" -> allQueue.filter { it.downloadStatus == DownloadStatus.COMPLETED.name }
            else -> allQueue
        }
    }

    val isDownloadingAny = remember(allQueue) {
        allQueue.any { it.downloadStatus == DownloadStatus.DOWNLOADING.name }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepForestBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Download Queue",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    )
                )

                var showDiagnosticDialog by remember { mutableStateOf(false) }

                IconButton(onClick = { showDiagnosticDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = TealAccent
                    )
                }

                if (showDiagnosticDialog) {
                    val summary = viewModel.getDiagnosticSummary()
                    AlertDialog(
                        onDismissRequest = { showDiagnosticDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = TealAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Diagnostics", color = PaleCream, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Surface(
                                color = DeepForestDarker,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CardForestBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = summary,
                                    color = PaleCream,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDiagnosticDialog = false }) {
                                Text("Close", color = TealAccent)
                            }
                        },
                        containerColor = CardForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Downloading", "Queued", "Completed")
                for (filter in filters) {
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealAccent,
                            selectedLabelColor = DeepForestDarker,
                            containerColor = CardForest,
                            labelColor = PaleCream
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) TealAccent else CardForestBorder,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.CloudDownload,
                        title = "Queue is empty",
                        description = "There are no chapters currently downloading or queued.",
                        actionLabel = "Add Comic",
                        onAction = { viewModel.navigateTo(Screen.AddComic) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { chapter ->
                        DownloadQueueItem(
                            chapter = chapter,
                            onPause = { viewModel.pauseDownload(chapter.id) },
                            onResume = { viewModel.resumeDownload(chapter.id) },
                            onRetry = { viewModel.retryDownload(chapter.id) },
                            onCancel = { viewModel.cancelDownload(chapter.id) },
                            onOpenReader = {
                                viewModel.navigateTo(Screen.Reader(chapter.comicId, chapter.id))
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Bottom background indicator if downloading
            if (isDownloadingAny) {
                Surface(
                    color = CardForest,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, TealAccentDark, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TealAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Downloading in background • Max ${viewModel.downloadManager.getMaxConcurrency()} concurrent",
                            style = MaterialTheme.typography.bodySmall.copy(color = PaleCream)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadQueueItem(
    chapter: Chapter,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onOpenReader: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardForestBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardForest),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (chapter.downloadStatus) {
                                DownloadStatus.DOWNLOADING.name -> DarkGreen
                                DownloadStatus.COMPLETED.name -> DarkGreen
                                DownloadStatus.FAILED.name -> ErrorRed.copy(alpha = 0.2f)
                                else -> DeepForestDarker
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (chapter.downloadStatus) {
                            DownloadStatus.DOWNLOADING.name -> Icons.Default.Download
                            DownloadStatus.COMPLETED.name -> Icons.Default.CheckCircle
                            DownloadStatus.PAUSED.name -> Icons.Default.Pause
                            DownloadStatus.FAILED.name -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (chapter.downloadStatus) {
                            DownloadStatus.DOWNLOADING.name -> TealAccent
                            DownloadStatus.COMPLETED.name -> SuccessGreen
                            DownloadStatus.FAILED.name -> ErrorRed
                            else -> SageSurface
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chapter ${chapter.chapterNumber.toInt().toString().padStart(3, '0')}: ${chapter.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (chapter.downloadStatus) {
                            DownloadStatus.DOWNLOADING.name -> {
                                val percent = (chapter.downloadProgress * 100).toInt()
                                "$percent% • Converting genuine JPEGs"
                            }
                            DownloadStatus.COMPLETED.name -> "Completed • ${formatBytes(chapter.sizeBytes)} • ${chapter.pageCount} pages"
                            DownloadStatus.QUEUED.name -> "Queued • Waiting for available slot"
                            DownloadStatus.PAUSED.name -> "Paused"
                            DownloadStatus.FAILED.name -> "Failed: ${chapter.errorMessage ?: "Network error"}"
                            else -> "Idle"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = when (chapter.downloadStatus) {
                                DownloadStatus.FAILED.name -> ErrorRed
                                DownloadStatus.DOWNLOADING.name -> TealAccent
                                else -> SageSurface
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (chapter.downloadStatus) {
                        DownloadStatus.DOWNLOADING.name -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.PauseCircle, contentDescription = "Pause", tint = TealAccent)
                            }
                        }
                        DownloadStatus.PAUSED.name, DownloadStatus.QUEUED.name -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Resume", tint = TealAccent)
                            }
                        }
                        DownloadStatus.FAILED.name -> {
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Default.Replay, contentDescription = "Retry", tint = TealAccent)
                            }
                        }
                        DownloadStatus.COMPLETED.name -> {
                            IconButton(onClick = onOpenReader) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Read", tint = TealAccent)
                            }
                        }
                    }

                    if (chapter.downloadStatus != DownloadStatus.COMPLETED.name) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = MutedText)
                        }
                    }
                }
            }

            // Progress bar
            if (chapter.downloadStatus == DownloadStatus.DOWNLOADING.name) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { chapter.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = TealAccent,
                    trackColor = DeepForestDarker
                )
            }
        }
    }
}
