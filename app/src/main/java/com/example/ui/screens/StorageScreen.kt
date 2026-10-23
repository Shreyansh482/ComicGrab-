package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ComicViewModel
import com.example.ui.components.ComicGrabTopBar
import com.example.ui.components.formatBytes
import com.example.ui.theme.*

@Composable
fun StorageScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.storageStats.collectAsState()
    var cleanResult by remember { mutableStateOf<String?>(null) }
    var isCleaning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComicGrabTopBar(
                title = "Storage Management",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        containerColor = DeepForestBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Gauge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(DarkGreen)
                            .border(6.dp, TealAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats?.let { formatBytes(it.totalUsedBytes) } ?: "0 B",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PaleCream
                                )
                            )
                            Text(
                                text = "App Used",
                                style = MaterialTheme.typography.bodySmall.copy(color = SageSurface)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Breakdown items
                    StorageBreakdownRow(
                        label = "Comic Images",
                        sizeStr = stats?.let { formatBytes(it.comicImagesBytes) } ?: "0 B",
                        indicatorColor = TealAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StorageBreakdownRow(
                        label = "ZIP Archives",
                        sizeStr = stats?.let { formatBytes(it.zipArchivesBytes) } ?: "0 B",
                        indicatorColor = MutedGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StorageBreakdownRow(
                        label = "Temporary & Cache Files",
                        sizeStr = stats?.let { formatBytes(it.tempFilesBytes) } ?: "0 B",
                        indicatorColor = SageSurface
                    )
                }
            }

            // Available Device Storage Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Available Storage", color = PaleCream, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = stats?.let { "${formatBytes(it.availableBytes)} free of ${formatBytes(it.totalDeviceBytes)}" } ?: "Calculating...",
                            color = SageSurface,
                            fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Default.Storage, contentDescription = null, tint = TealAccent)
                }
            }

            // Clean Temp Files Action Button
            Button(
                onClick = {
                    isCleaning = true
                    viewModel.cleanTemporaryFiles { reclaimed ->
                        isCleaning = false
                        cleanResult = "Cleaned ${formatBytes(reclaimed)} of cache & temporary files."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clean_temp_files_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = DeepForestDarker),
                shape = RoundedCornerShape(12.dp),
                enabled = !isCleaning
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCleaning) "Cleaning..." else "Clean Temporary Files", fontWeight = FontWeight.Bold)
            }

            if (cleanResult != null) {
                Text(
                    text = cleanResult!!,
                    color = TealAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StorageBreakdownRow(
    label: String,
    sizeStr: String,
    indicatorColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = PaleCream, fontSize = 14.sp)
        }
        Text(sizeStr, color = SageSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
