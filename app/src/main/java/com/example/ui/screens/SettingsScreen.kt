package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReadingMode
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    var jpegQuality by remember { mutableIntStateOf(viewModel.downloadManager.getJpegQuality()) }
    var maxConcurrency by remember { mutableIntStateOf(viewModel.downloadManager.getMaxConcurrency()) }
    val defaultReadingMode by viewModel.defaultReadingMode.collectAsState()
    val keepAwake by viewModel.keepScreenAwake.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showConcurrencyDialog by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaleCream
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Download Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Download Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Image Quality Row
                    SettingsActionRow(
                        title = "Image Quality (JPEG)",
                        value = "$jpegQuality%",
                        onClick = { showQualityDialog = true }
                    )

                    HorizontalDivider(color = CardForestBorder, modifier = Modifier.padding(vertical = 8.dp))

                    // Concurrency Row
                    SettingsActionRow(
                        title = "Max Concurrent Downloads",
                        value = "$maxConcurrency jobs",
                        onClick = { showConcurrencyDialog = true }
                    )
                }
            }

            // Reader Experience Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reader Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Default Reading Mode", color = PaleCream, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = defaultReadingMode == ReadingMode.WEBTOON,
                            onClick = { viewModel.setDefaultReadingMode(ReadingMode.WEBTOON) },
                            label = { Text("Webtoon (Continuous)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealAccent,
                                selectedLabelColor = DeepForestDarker,
                                containerColor = DeepForestDarker,
                                labelColor = PaleCream
                            )
                        )
                        FilterChip(
                            selected = defaultReadingMode == ReadingMode.MANGA,
                            onClick = { viewModel.setDefaultReadingMode(ReadingMode.MANGA) },
                            label = { Text("Manga (Page-by-page)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealAccent,
                                selectedLabelColor = DeepForestDarker,
                                containerColor = DeepForestDarker,
                                labelColor = PaleCream
                            )
                        )
                    }

                    HorizontalDivider(color = CardForestBorder, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Keep Screen Awake", color = PaleCream)
                            Text("Prevent display sleep while reading", color = SageSurface, fontSize = 12.sp)
                        }
                        Switch(
                            checked = keepAwake,
                            onCheckedChange = { viewModel.setKeepScreenAwake(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepForestDarker,
                                checkedTrackColor = TealAccent
                            )
                        )
                    }
                }
            }

            // Navigation to Security & Storage
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.SecuritySettings) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = TealAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Security & PIN Settings", color = PaleCream, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = SageSurface, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(color = CardForestBorder, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.Storage) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = TealAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Storage Management", color = PaleCream, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = SageSurface, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // About Brand Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DeepForestDarker),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ComicGrab v1.0", fontWeight = FontWeight.Bold, color = PaleCream)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your Comics. Your Library. Your Privacy.",
                        color = SageSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // JPEG Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Select JPEG Quality", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(75, 85, 90, 95).forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    jpegQuality = q
                                    viewModel.downloadManager.setJpegQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = jpegQuality == q,
                                onClick = {
                                    jpegQuality = q
                                    viewModel.downloadManager.setJpegQuality(q)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TealAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$q% ${if (q == 90) "(Recommended)" else ""}", color = PaleCream)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = CardForest
        )
    }

    // Concurrency Dialog
    if (showConcurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showConcurrencyDialog = false },
            title = { Text("Max Concurrent Downloads", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(1, 2, 3, 5).forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    maxConcurrency = c
                                    viewModel.downloadManager.setMaxConcurrency(c)
                                    showConcurrencyDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = maxConcurrency == c,
                                onClick = {
                                    maxConcurrency = c
                                    viewModel.downloadManager.setMaxConcurrency(c)
                                    showConcurrencyDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TealAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$c parallel downloads ${if (c == 3) "(Recommended)" else ""}", color = PaleCream)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = CardForest
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = PaleCream)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = TealAccent, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = SageSurface,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
