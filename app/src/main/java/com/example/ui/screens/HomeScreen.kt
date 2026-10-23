package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Comic
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.SummaryStatCard
import com.example.ui.components.formatBytes
import com.example.ui.theme.*
import java.io.File

@Composable
fun HomeScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    val totalComics by viewModel.totalComicCount.collectAsState()
    val activeDownloads by viewModel.activeDownloadsCount.collectAsState()
    val archives by viewModel.allArchives.collectAsState()
    val recentReading by viewModel.recentReading.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!text.isNullOrBlank()) {
            urlInput = text.trim()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepForestBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CollectionsBookmark,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ComicGrab",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                }

                IconButton(
                    onClick = { viewModel.securityManager.lock() },
                    modifier = Modifier.testTag("quick_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Lock App",
                        tint = TealAccent
                    )
                }
            }
        }

        // Greeting Header
        item {
            Column {
                Text(
                    text = "Good morning",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    )
                )
                Text(
                    text = "Your comics, your way",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SageSurface)
                )
            }
        }

        // Quick Scan Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // URL Input Row
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_url_input"),
                        placeholder = { Text("Paste comic URL here...", color = MutedText) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = SageSurface)
                        },
                        trailingIcon = {
                            IconButton(onClick = { pasteFromClipboard() }) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = TealAccent)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = DarkGreen,
                            focusedTextColor = PaleCream,
                            unfocusedTextColor = PaleCream,
                            focusedContainerColor = DeepForestDarker,
                            unfocusedContainerColor = DeepForestDarker
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                viewModel.currentInputUrl.value = urlInput
                                viewModel.navigateTo(Screen.AddComic)
                                viewModel.analyzeComicUrl(urlInput) { discovered ->
                                    viewModel.navigateTo(Screen.SelectChapters(discovered))
                                }
                            } else {
                                viewModel.navigateTo(Screen.AddComic)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scan_comic_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealAccent,
                            contentColor = DeepForestDarker
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Comic", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Demo / Test URL buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Test samples:", style = MaterialTheme.typography.bodySmall.copy(color = SageSurface))
                        FilterChip(
                            selected = false,
                            onClick = {
                                val url = "https://manhwatop.com/manga/solo-leveling"
                                urlInput = url
                                viewModel.currentInputUrl.value = url
                                viewModel.navigateTo(Screen.AddComic)
                                viewModel.analyzeComicUrl(url) { discovered ->
                                    viewModel.navigateTo(Screen.SelectChapters(discovered))
                                }
                            },
                            label = { Text("Solo Leveling", fontSize = 11.sp, color = PaleCream) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = DarkGreen)
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                val url = "https://asuracomic.net/series/the-beginning-after-the-end"
                                urlInput = url
                                viewModel.currentInputUrl.value = url
                                viewModel.navigateTo(Screen.AddComic)
                                viewModel.analyzeComicUrl(url) { discovered ->
                                    viewModel.navigateTo(Screen.SelectChapters(discovered))
                                }
                            },
                            label = { Text("TBATE", fontSize = 11.sp, color = PaleCream) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = DarkGreen)
                        )
                    }
                }
            }
        }

        // 2x2 Summary Stat Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        title = "Library",
                        countText = "$totalComics",
                        subText = "$totalComics comics saved",
                        icon = Icons.Default.AutoStories,
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Downloads",
                        countText = "$activeDownloads",
                        subText = if (activeDownloads > 0) "$activeDownloads active" else "0 active",
                        icon = Icons.Default.Download,
                        onClick = { viewModel.navigateTo(Screen.DownloadQueue) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        title = "Archives",
                        countText = "${archives.size}",
                        subText = "${archives.size} ZIP files",
                        icon = Icons.Default.FolderZip,
                        onClick = { viewModel.navigateTo(Screen.Archives) },
                        modifier = Modifier.weight(1f)
                    )
                    val usedStr = storageStats?.let { formatBytes(it.totalUsedBytes) } ?: "0 B"
                    SummaryStatCard(
                        title = "Storage",
                        countText = usedStr,
                        subText = "Used storage",
                        icon = Icons.Default.PieChart,
                        onClick = { viewModel.navigateTo(Screen.Storage) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Continue Reading Section
        if (recentReading.isNotEmpty()) {
            item {
                Text(
                    text = "Continue Reading",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    )
                )
            }

            items(recentReading) { comic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val chId = comic.lastReadChapterId
                            if (chId != null) {
                                viewModel.navigateTo(Screen.Reader(comic.id, chId))
                            } else {
                                viewModel.navigateTo(Screen.ComicDetails(comic.id))
                            }
                        }
                        .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardForest)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover Thumbnail
                        if (comic.coverPath != null && File(comic.coverPath).exists()) {
                            AsyncImage(
                                model = File(comic.coverPath),
                                contentDescription = comic.title,
                                modifier = Modifier
                                    .size(60.dp, 80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, tint = SageSurface)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = comic.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PaleCream
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = comic.lastReadChapterTitle ?: "Chapter 1",
                                style = MaterialTheme.typography.bodyMedium.copy(color = SageSurface)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Page ${comic.lastReadPage + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TealAccent)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val chId = comic.lastReadChapterId
                                if (chId != null) {
                                    viewModel.navigateTo(Screen.Reader(comic.id, chId))
                                } else {
                                    viewModel.navigateTo(Screen.ComicDetails(comic.id))
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DarkGreen,
                                contentColor = PaleCream
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
