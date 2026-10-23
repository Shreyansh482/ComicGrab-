package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Comic
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatBytes
import com.example.ui.theme.*
import java.io.File

@Composable
fun LibraryScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Recent") } // "Recent", "Title", "Chapters"
    var showSortMenu by remember { mutableStateOf(false) }

    val allComics by viewModel.allComics.collectAsState()

    val filteredComics = remember(allComics, searchQuery, sortBy) {
        val filtered = if (searchQuery.isBlank()) {
            allComics
        } else {
            allComics.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

        when (sortBy) {
            "Title" -> filtered.sortedBy { it.title.lowercase() }
            "Chapters" -> filtered.sortedByDescending { it.downloadedChaptersCount }
            else -> filtered.sortedByDescending { it.updatedAt }
        }
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

            // Title
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaleCream
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_search_input"),
                    placeholder = { Text("Search comics...", color = MutedText) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SageSurface)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = DarkGreen,
                        focusedTextColor = PaleCream,
                        unfocusedTextColor = PaleCream,
                        focusedContainerColor = CardForest,
                        unfocusedContainerColor = CardForest
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardForest)
                            .border(1.dp, CardForestBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = TealAccent)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(CardForest)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Added / Read", color = PaleCream) },
                            onClick = { sortBy = "Recent"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Comic Title (A-Z)", color = PaleCream) },
                            onClick = { sortBy = "Title"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloaded Chapters", color = PaleCream) },
                            onClick = { sortBy = "Chapters"; showSortMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredComics.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.AutoStories,
                        title = if (searchQuery.isBlank()) "Your library is empty" else "No matching comics",
                        description = if (searchQuery.isBlank())
                            "Add comics by pasting a web link or using quick scan."
                        else "No comic found matching '$searchQuery'.",
                        actionLabel = if (searchQuery.isBlank()) "Add Comic" else null,
                        onAction = { viewModel.navigateTo(Screen.AddComic) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredComics, key = { it.id }) { comic ->
                        LibraryComicCard(
                            comic = comic,
                            onClick = {
                                viewModel.navigateTo(Screen.ComicDetails(comic.id))
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
}

@Composable
fun LibraryComicCard(
    comic: Comic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardForest)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            if (comic.coverPath != null && File(comic.coverPath).exists()) {
                AsyncImage(
                    model = File(comic.coverPath),
                    contentDescription = comic.title,
                    modifier = Modifier
                        .size(70.dp, 95.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp, 95.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = SageSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${comic.downloadedChaptersCount} / ${comic.totalChapters} chapters",
                    style = MaterialTheme.typography.bodySmall.copy(color = SageSurface)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatBytes(comic.totalSizeBytes),
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )
            }

            // Status Badge
            Surface(
                color = when {
                    comic.downloadedChaptersCount > 0 -> DarkGreen
                    else -> DeepForestDarker
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when {
                        comic.downloadedChaptersCount >= comic.totalChapters && comic.totalChapters > 0 -> "Completed"
                        comic.downloadedChaptersCount > 0 -> "Downloaded"
                        else -> "Not downloaded"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (comic.downloadedChaptersCount > 0) TealAccent else SageSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
