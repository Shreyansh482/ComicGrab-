package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoveredComic
import com.example.ui.ComicViewModel
import com.example.ui.components.ComicGrabTopBar
import com.example.ui.theme.*

@Composable
fun SelectChaptersScreen(
    discovered: DiscoveredComic,
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val selectedNumbers = remember { mutableStateMapOf<Float, Boolean>() }

    // Initially select first 3 chapters by default for immediate convenience
    LaunchedEffect(discovered) {
        if (selectedNumbers.isEmpty()) {
            discovered.chapters.take(3).forEach {
                selectedNumbers[it.number] = true
            }
        }
    }

    val selectedCount = selectedNumbers.count { it.value }
    val isAllSelected = discovered.chapters.isNotEmpty() && selectedCount == discovered.chapters.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComicGrabTopBar(
                title = "Select Chapters",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        bottomBar = {
            Surface(
                color = DeepForestDarker,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(1.dp, CardForestBorder)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val selected = selectedNumbers.filter { it.value }.keys
                            if (selected.isNotEmpty()) {
                                viewModel.startDownloadingChapters(discovered, selected)
                            }
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_download_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealAccent,
                            contentColor = DeepForestDarker,
                            disabledContainerColor = DarkGreen,
                            disabledContentColor = MutedText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedCount > 0) "Start Download ($selectedCount selected)" else "Select chapters to download",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = DeepForestBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = discovered.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    )
                )
                Text(
                    text = "Found ${discovered.chapters.size} chapters",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TealAccent)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Batch selection control row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardForestBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardForest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val target = !isAllSelected
                                    discovered.chapters.forEach {
                                        selectedNumbers[it.number] = target
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { checked ->
                                    discovered.chapters.forEach {
                                        selectedNumbers[it.number] = checked
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TealAccent,
                                    uncheckedColor = MutedGreen,
                                    checkmarkColor = DeepForestDarker
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select All", color = PaleCream, fontWeight = FontWeight.SemiBold)
                        }

                        // Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedNumbers.clear()
                                    discovered.chapters.take(5).forEach { selectedNumbers[it.number] = true }
                                },
                                label = { Text("1-5", fontSize = 12.sp, color = PaleCream) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = DarkGreen)
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    selectedNumbers.clear()
                                    discovered.chapters.take(20).forEach { selectedNumbers[it.number] = true }
                                },
                                label = { Text("1-20", fontSize = 12.sp, color = PaleCream) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = DarkGreen)
                            )
                        }
                    }
                }
            }

            // Chapters List
            items(discovered.chapters) { chapter ->
                val isSelected = selectedNumbers[chapter.number] == true
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedNumbers[chapter.number] = !isSelected
                        }
                        .border(
                            1.dp,
                            if (isSelected) TealAccent else CardForestBorder,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CardForest.copy(alpha = 0.9f) else CardForest
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chapter ${chapter.number.toInt().toString().padStart(3, '0')}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TealAccent else PaleCream
                                )
                            )
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium.copy(color = PaleCream)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${chapter.estimatedImages} images",
                                style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                            )
                        }

                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedNumbers[chapter.number] = checked
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TealAccent,
                                uncheckedColor = MutedGreen,
                                checkmarkColor = DeepForestDarker
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
