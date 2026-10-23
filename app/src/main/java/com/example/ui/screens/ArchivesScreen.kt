package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ZipArchive
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatBytes
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchivesScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val archives by viewModel.allArchives.collectAsState()
    var archiveToDelete by remember { mutableStateOf<ZipArchive?>(null) }

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

            Text(
                text = "Archives",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaleCream
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (archives.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.FolderZip,
                        title = "No ZIP archives yet",
                        description = "You can create ZIP archives from any comic in your Library to back up or export images.",
                        actionLabel = "Go to Library",
                        onAction = { viewModel.navigateTo(Screen.Library) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(archives, key = { it.id }) { archive ->
                        ZipArchiveItem(
                            archive = archive,
                            onExport = {
                                viewModel.archiveManager.shareOrExportArchive(context, archive)
                            },
                            onDelete = {
                                archiveToDelete = archive
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (archiveToDelete != null) {
        val target = archiveToDelete!!
        AlertDialog(
            onDismissRequest = { archiveToDelete = null },
            title = { Text("Delete ZIP Archive?", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete '${target.fileName}'? Source chapter images in your library will remain intact.",
                    color = SageSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArchive(target)
                        archiveToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveToDelete = null }) {
                    Text("Cancel", color = PaleCream)
                }
            },
            containerColor = CardForest
        )
    }
}

@Composable
fun ZipArchiveItem(
    archive: ZipArchive,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(archive.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(archive.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardForestBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardForest),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archive.fileName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatBytes(archive.sizeBytes)} • ${archive.chapterCount} chapters • $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(color = SageSurface)
                )
            }

            IconButton(onClick = onExport, modifier = Modifier.testTag("export_archive_button")) {
                Icon(Icons.Default.Share, contentDescription = "Export Archive", tint = TealAccent)
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_archive_button")) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Archive", tint = MutedText)
            }
        }
    }
}
