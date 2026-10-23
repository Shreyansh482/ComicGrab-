package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicGrabTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaleCream
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PaleCream
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepForestBg,
            titleContentColor = PaleCream,
            navigationIconContentColor = PaleCream,
            actionIconContentColor = PaleCream
        )
    )
}

@Composable
fun ComicGrabBottomNav(
    currentScreen: Screen,
    activeDownloadsCount: Int = 0,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = DeepForestDarker,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepForestDarker,
                selectedTextColor = PaleCream,
                indicatorColor = TealAccent,
                unselectedIconColor = MutedGreen,
                unselectedTextColor = MutedGreen
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Library,
            onClick = { onNavigate(Screen.Library) },
            icon = { Icon(Icons.Filled.AutoStories, contentDescription = "Library") },
            label = { Text("Library") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepForestDarker,
                selectedTextColor = PaleCream,
                indicatorColor = TealAccent,
                unselectedIconColor = MutedGreen,
                unselectedTextColor = MutedGreen
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.DownloadQueue,
            onClick = { onNavigate(Screen.DownloadQueue) },
            icon = {
                BadgedBox(badge = {
                    if (activeDownloadsCount > 0) {
                        Badge(containerColor = TealAccent, contentColor = DeepForestDarker) {
                            Text("$activeDownloadsCount")
                        }
                    }
                }) {
                    Icon(Icons.Filled.Download, contentDescription = "Queue")
                }
            },
            label = { Text("Queue") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepForestDarker,
                selectedTextColor = PaleCream,
                indicatorColor = TealAccent,
                unselectedIconColor = MutedGreen,
                unselectedTextColor = MutedGreen
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Archives,
            onClick = { onNavigate(Screen.Archives) },
            icon = { Icon(Icons.Filled.FolderZip, contentDescription = "Archives") },
            label = { Text("Archives") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepForestDarker,
                selectedTextColor = PaleCream,
                indicatorColor = TealAccent,
                unselectedIconColor = MutedGreen,
                unselectedTextColor = MutedGreen
            )
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Settings || currentScreen is Screen.Storage || currentScreen is Screen.SecuritySettings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = "More") },
            label = { Text("More") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepForestDarker,
                selectedTextColor = PaleCream,
                indicatorColor = TealAccent,
                unselectedIconColor = MutedGreen,
                unselectedTextColor = MutedGreen
            )
        )
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    countText: String,
    subText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardForest)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = TealAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PaleCream
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = PaleCream
                )
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SageSurface,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PaleCream
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(color = MutedText),
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent,
                    contentColor = DeepForestDarker
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
