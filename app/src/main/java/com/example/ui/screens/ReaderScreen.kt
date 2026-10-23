package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Chapter
import com.example.data.model.Comic
import com.example.data.model.ReadingDirection
import com.example.data.model.ReadingMode
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    comicId: Long,
    chapterId: Long,
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentChapterId by remember(chapterId) { mutableStateOf(chapterId) }
    val comic by viewModel.comicRepository.observeComic(comicId).collectAsState(initial = null)
    val chapter by viewModel.comicRepository.observeChapter(currentChapterId).collectAsState(initial = null)
    val allChapters by viewModel.comicRepository.getChaptersForComic(comicId).collectAsState(initial = emptyList())

    val keepAwake by viewModel.keepScreenAwake.collectAsState()
    val defaultMode by viewModel.defaultReadingMode.collectAsState()

    // Reading mode & direction: per-comic override if set, else global default
    var readingMode by remember {
        mutableStateOf(
            comic?.preferredReadingMode?.let {
                try { ReadingMode.valueOf(it) } catch (_: Exception) { defaultMode }
            } ?: defaultMode
        )
    }

    var readingDirection by remember {
        mutableStateOf(
            comic?.preferredReadingDirection?.let {
                try { ReadingDirection.valueOf(it) } catch (_: Exception) { ReadingDirection.LTR }
            } ?: ReadingDirection.LTR
        )
    }

    // Keep screen awake effect
    DisposableEffect(keepAwake) {
        val window = (context as? Activity)?.window
        if (keepAwake && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Image files
    val imageFiles = remember(chapter?.folderPath) {
        val folder = chapter?.folderPath?.let { File(it) }
        if (folder != null && folder.exists() && folder.isDirectory) {
            folder.listFiles { f ->
                f.extension.equals("jpg", ignoreCase = true) || f.extension.equals("jpeg", ignoreCase = true)
            }?.sortedBy { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Controls visibility
    var controlsVisible by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Back handler
    BackHandler {
        viewModel.navigateBack()
    }

    if (chapter == null || comic == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReaderDarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TealAccent)
        }
        return
    }

    val currentChapter = chapter!!
    val currentComic = comic!!

    // Reading progress tracking
    val initialPage = remember(currentChapter.id) { currentChapter.lastReadPage.coerceIn(0, (imageFiles.size - 1).coerceAtLeast(0)) }

    // Setup LazyListState for Webtoon mode
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    // Setup PagerState for Manga mode
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageFiles.size.coerceAtLeast(1) }
    )

    // Save progress periodically as user scrolls/swipes
    val currentPageIndex by remember {
        derivedStateOf {
            if (readingMode == ReadingMode.WEBTOON) {
                lazyListState.firstVisibleItemIndex
            } else {
                pagerState.currentPage
            }
        }
    }

    LaunchedEffect(currentPageIndex, imageFiles.size) {
        if (imageFiles.isNotEmpty()) {
            val isCompleted = currentPageIndex >= imageFiles.size - 1
            viewModel.comicRepository.saveReadingProgress(
                comicId = currentComic.id,
                chapterId = currentChapter.id,
                page = currentPageIndex,
                offset = 0,
                isCompleted = isCompleted
            )
        }
    }

    // Find Previous & Next chapters
    val prevChapter = remember(allChapters, currentChapter) {
        allChapters.filter { it.chapterNumber < currentChapter.chapterNumber && it.isDownloaded }
            .maxByOrNull { it.chapterNumber }
    }
    val nextChapter = remember(allChapters, currentChapter) {
        allChapters.filter { it.chapterNumber > currentChapter.chapterNumber && it.isDownloaded }
            .minByOrNull { it.chapterNumber }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReaderNavy)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Content Area
        if (imageFiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = SageSurface,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No images downloaded for this chapter",
                    style = MaterialTheme.typography.titleMedium.copy(color = PaleCream)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download this chapter from the Comic Details page to read offline.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.downloadManager.enqueueChapter(currentChapter.id)
                        viewModel.navigateTo(Screen.DownloadQueue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = DeepForestDarker)
                ) {
                    Text("Download Now")
                }
            }
        } else {
            when (readingMode) {
                ReadingMode.WEBTOON -> {
                    // Continuous Vertical Webtoon Scrolling
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("webtoon_scroll_container"),
                        verticalArrangement = Arrangement.spacedBy(0.dp) // Zero gap for continuous webtoon flow
                    ) {
                        itemsIndexed(imageFiles, key = { _, file -> file.absolutePath }) { _, file ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(file)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Comic Page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentScale = ContentScale.FillWidth
                            )
                        }

                        // Next Chapter prompt at bottom of chapter
                        item {
                            Surface(
                                color = ReaderDarkBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "End of Chapter ${currentChapter.chapterNumber.toInt()}",
                                        color = PaleCream,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (nextChapter != null) {
                                        Button(
                                            onClick = { currentChapterId = nextChapter.id },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TealAccent,
                                                contentColor = DeepForestDarker
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.SkipNext, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Next: ${nextChapter.title}", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("You have reached the latest downloaded chapter.", color = SageSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                ReadingMode.MANGA -> {
                    // Page by Page Manga Horizontal Pager
                    val isRtl = readingDirection == ReadingDirection.RTL

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("manga_pager_container"),
                        reverseLayout = isRtl
                    ) { pageIdx ->
                        if (pageIdx in imageFiles.indices) {
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 3.5f)
                                            if (scale > 1f) {
                                                offset += pan
                                            } else {
                                                offset = androidx.compose.ui.geometry.Offset.Zero
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageFiles[pageIdx])
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Manga Page ${pageIdx + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }

        // Minimal Overlays (Auto-Hide)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = ReaderDarkBg.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PaleCream
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentComic.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PaleCream
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ch. ${currentChapter.chapterNumber.toInt()} • ${currentChapter.title}",
                            style = MaterialTheme.typography.bodySmall.copy(color = SageSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Page counter indicator
                    if (imageFiles.isNotEmpty()) {
                        Surface(
                            color = CardForest,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${currentPageIndex + 1} / ${imageFiles.size}",
                                color = TealAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Reader Settings", tint = PaleCream)
                    }
                }
            }
        }

        // Bottom Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = ReaderDarkBg.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Slider if multiple pages
                    if (imageFiles.size > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1", color = SageSurface, fontSize = 11.sp)
                            Slider(
                                value = (currentPageIndex + 1).toFloat(),
                                onValueChange = { newVal ->
                                    val targetIdx = (newVal.toInt() - 1).coerceIn(0, imageFiles.size - 1)
                                    coroutineScope.launch {
                                        if (readingMode == ReadingMode.WEBTOON) {
                                            lazyListState.scrollToItem(targetIdx)
                                        } else {
                                            pagerState.scrollToPage(targetIdx)
                                        }
                                    }
                                },
                                valueRange = 1f..imageFiles.size.toFloat(),
                                steps = (imageFiles.size - 2).coerceAtLeast(0),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = TealAccent,
                                    activeTrackColor = TealAccent,
                                    inactiveTrackColor = DarkGreen
                                )
                            )
                            Text("${imageFiles.size}", color = SageSurface, fontSize = 11.sp)
                        }
                    }

                    // Navigation & Mode Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Chapter
                        IconButton(
                            onClick = {
                                if (prevChapter != null) {
                                    currentChapterId = prevChapter.id
                                }
                            },
                            enabled = prevChapter != null
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous Chapter",
                                tint = if (prevChapter != null) PaleCream else DarkGreen
                            )
                        }

                        // Mode Switcher Toggle: Webtoon vs Manga
                        Button(
                            onClick = {
                                val nextMode = if (readingMode == ReadingMode.WEBTOON) ReadingMode.MANGA else ReadingMode.WEBTOON
                                readingMode = nextMode
                                coroutineScope.launch {
                                    viewModel.comicRepository.setComicReadingPreferences(
                                        comicId = currentComic.id,
                                        mode = nextMode.name,
                                        direction = readingDirection.name
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardForest,
                                contentColor = PaleCream
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (readingMode == ReadingMode.WEBTOON) Icons.Default.ViewStream else Icons.Default.ViewCarousel,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (readingMode == ReadingMode.WEBTOON) "Webtoon" else "Manga",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Reading Direction (for Manga mode)
                        if (readingMode == ReadingMode.MANGA) {
                            Button(
                                onClick = {
                                    val nextDir = if (readingDirection == ReadingDirection.LTR) ReadingDirection.RTL else ReadingDirection.LTR
                                    readingDirection = nextDir
                                    coroutineScope.launch {
                                        viewModel.comicRepository.setComicReadingPreferences(
                                            comicId = currentComic.id,
                                            mode = readingMode.name,
                                            direction = nextDir.name
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CardForest,
                                    contentColor = PaleCream
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (readingDirection == ReadingDirection.LTR) "LTR" else "RTL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent
                                )
                            }
                        }

                        // Next Chapter
                        IconButton(
                            onClick = {
                                if (nextChapter != null) {
                                    currentChapterId = nextChapter.id
                                }
                            },
                            enabled = nextChapter != null
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next Chapter",
                                tint = if (nextChapter != null) PaleCream else DarkGreen
                            )
                        }
                    }
                }
            }
        }
    }

    // Reader Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Reader Settings", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Reading Mode Choice
                    Column {
                        Text("Reading Mode", color = SageSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = readingMode == ReadingMode.WEBTOON,
                                onClick = { readingMode = ReadingMode.WEBTOON },
                                label = { Text("Webtoon (Vertical)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealAccent,
                                    selectedLabelColor = DeepForestDarker,
                                    containerColor = DeepForestDarker,
                                    labelColor = PaleCream
                                )
                            )
                            FilterChip(
                                selected = readingMode == ReadingMode.MANGA,
                                onClick = { readingMode = ReadingMode.MANGA },
                                label = { Text("Manga (Pages)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealAccent,
                                    selectedLabelColor = DeepForestDarker,
                                    containerColor = DeepForestDarker,
                                    labelColor = PaleCream
                                )
                            )
                        }
                    }

                    // Reading Direction
                    if (readingMode == ReadingMode.MANGA) {
                        Column {
                            Text("Page Direction", color = SageSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = readingDirection == ReadingDirection.LTR,
                                    onClick = { readingDirection = ReadingDirection.LTR },
                                    label = { Text("Left to Right") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealAccent,
                                        selectedLabelColor = DeepForestDarker,
                                        containerColor = DeepForestDarker,
                                        labelColor = PaleCream
                                    )
                                )
                                FilterChip(
                                    selected = readingDirection == ReadingDirection.RTL,
                                    onClick = { readingDirection = ReadingDirection.RTL },
                                    label = { Text("Right to Left (Manga)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealAccent,
                                        selectedLabelColor = DeepForestDarker,
                                        containerColor = DeepForestDarker,
                                        labelColor = PaleCream
                                    )
                                )
                            }
                        }
                    }

                    // Keep Screen Awake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Screen Awake", color = PaleCream)
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
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done", color = TealAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardForest
        )
    }
}
