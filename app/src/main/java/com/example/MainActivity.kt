package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.ComicGrabBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.ComicGrabTheme
import com.example.ui.theme.DeepForestBg

class MainActivity : FragmentActivity() {

    private val viewModel: ComicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ComicGrabTheme(darkTheme = true) {
                ComicGrabApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ComicGrabApp(viewModel: ComicViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeDownloadsCount by viewModel.activeDownloadsCount.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle observer for automatic relocking on background/foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.securityManager.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.securityManager.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle back button outside the reader
    BackHandler(enabled = currentScreen !is Screen.Home && currentScreen !is Screen.Lock && currentScreen !is Screen.Reader) {
        viewModel.navigateBack()
    }

    val hideBottomNav = currentScreen is Screen.Lock || currentScreen is Screen.Reader

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForestBg),
        bottomBar = {
            if (!hideBottomNav) {
                ComicGrabBottomNav(
                    currentScreen = currentScreen,
                    activeDownloadsCount = activeDownloadsCount,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        },
        containerColor = DeepForestBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Lock -> {
                    LockScreen(
                        viewModel = viewModel,
                        onUnlocked = {
                            viewModel.navigateTo(Screen.Home)
                        }
                    )
                }

                is Screen.Home -> {
                    HomeScreen(viewModel = viewModel)
                }

                is Screen.Library -> {
                    LibraryScreen(viewModel = viewModel)
                }

                is Screen.ComicDetails -> {
                    ComicDetailsScreen(
                        comicId = screen.comicId,
                        viewModel = viewModel
                    )
                }

                is Screen.Reader -> {
                    ReaderScreen(
                        comicId = screen.comicId,
                        chapterId = screen.chapterId,
                        viewModel = viewModel
                    )
                }

                is Screen.AddComic -> {
                    AddComicScreen(viewModel = viewModel)
                }

                is Screen.SelectChapters -> {
                    SelectChaptersScreen(
                        discovered = screen.discovered,
                        viewModel = viewModel
                    )
                }

                is Screen.DownloadQueue -> {
                    DownloadQueueScreen(viewModel = viewModel)
                }

                is Screen.Archives -> {
                    ArchivesScreen(viewModel = viewModel)
                }

                is Screen.Storage -> {
                    StorageScreen(viewModel = viewModel)
                }

                is Screen.SecuritySettings -> {
                    SecuritySettingsScreen(viewModel = viewModel)
                }

                is Screen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
