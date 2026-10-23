package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ComicDatabase
import com.example.data.model.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    data object Lock : Screen()
    data object Home : Screen()
    data object Library : Screen()
    data class ComicDetails(val comicId: Long) : Screen()
    data class Reader(val comicId: Long, val chapterId: Long) : Screen()
    data object AddComic : Screen()
    data class SelectChapters(val discovered: DiscoveredComic) : Screen()
    data object DownloadQueue : Screen()
    data object Archives : Screen()
    data object Storage : Screen()
    data object SecuritySettings : Screen()
    data object Settings : Screen()
}

class ComicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ComicDatabase.getInstance(application)
    private val comicDao = database.comicDao()
    private val chapterDao = database.chapterDao()
    private val archiveDao = database.archiveDao()

    val webScraper = WebScraper()
    val securityManager = SecurityManager(application)
    val storageManager = StorageManager(application)
    val comicRepository = ComicRepository(application, comicDao, chapterDao, archiveDao)
    val downloadManager = DownloadManager(application, comicDao, chapterDao, webScraper)
    val archiveManager = ArchiveManager(application, comicDao, chapterDao, archiveDao)

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(
        if (securityManager.hasPinConfigured() && !securityManager.isUnlocked.value) Screen.Lock else Screen.Home
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>()

    // Data flows
    val allComics: StateFlow<List<Comic>> = comicRepository.allComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentReading: StateFlow<List<Comic>> = comicRepository.recentReadingComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queueChapters: StateFlow<List<Chapter>> = comicRepository.allQueueChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloadsCount: StateFlow<Int> = comicRepository.activeDownloadsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allArchives: StateFlow<List<ZipArchive>> = comicRepository.allArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalComicCount: StateFlow<Int> = comicRepository.totalComicCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Discovery state
    val currentInputUrl = MutableStateFlow("")
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzeError = MutableStateFlow<String?>(null)
    val analyzeError: StateFlow<String?> = _analyzeError.asStateFlow()

    val diagnosticLogs = DownloadDiagnostics.logs

    fun getDiagnosticSummary(): String = DownloadDiagnostics.getFormattedSummary()

    // Storage stats
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    // General App Settings
    private val appPrefs = application.getSharedPreferences("comicgrab_prefs", Application.MODE_PRIVATE)
    private val _keepScreenAwake = MutableStateFlow(appPrefs.getBoolean("keep_screen_awake", true))
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    private val _defaultReadingMode = MutableStateFlow(
        ReadingMode.valueOf(appPrefs.getString("default_reading_mode", ReadingMode.WEBTOON.name) ?: ReadingMode.WEBTOON.name)
    )
    val defaultReadingMode: StateFlow<ReadingMode> = _defaultReadingMode.asStateFlow()

    init {
        // Observe security unlock
        viewModelScope.launch {
            securityManager.isUnlocked.collect { unlocked ->
                if (!unlocked && securityManager.hasPinConfigured()) {
                    _currentScreen.value = Screen.Lock
                }
            }
        }

        refreshStorageStats()
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value !is Screen.Lock) {
            screenStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value !is Screen.Home && _currentScreen.value !is Screen.Lock) {
            _currentScreen.value = Screen.Home
            return true
        }
        return false
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _keepScreenAwake.value = enabled
        appPrefs.edit().putBoolean("keep_screen_awake", enabled).apply()
    }

    fun setDefaultReadingMode(mode: ReadingMode) {
        _defaultReadingMode.value = mode
        appPrefs.edit().putString("default_reading_mode", mode.name).apply()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = storageManager.getStorageStats()
        }
    }

    fun cleanTemporaryFiles(onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val reclaimed = storageManager.cleanTemporaryFiles()
            refreshStorageStats()
            onResult(reclaimed)
        }
    }

    fun analyzeComicUrl(url: String, onSuccess: (DiscoveredComic) -> Unit) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analyzeError.value = null
            val result = webScraper.analyzeUrl(url)
            _isAnalyzing.value = false
            result.onSuccess { discovered ->
                onSuccess(discovered)
            }.onFailure { error ->
                _analyzeError.value = error.message ?: "Failed to analyze webpage"
            }
        }
    }

    fun startDownloadingChapters(discovered: DiscoveredComic, selectedChapterNumbers: Set<Float>) {
        viewModelScope.launch {
            val (comicId, savedChapters) = comicRepository.saveDiscoveredComic(discovered)
            val toEnqueue = savedChapters.filter { selectedChapterNumbers.contains(it.chapterNumber) }.map { it.id }
            downloadManager.enqueueChapters(toEnqueue)
            navigateTo(Screen.DownloadQueue)
        }
    }

    fun pauseDownload(chapterId: Long) = downloadManager.pauseDownload(chapterId)
    fun resumeDownload(chapterId: Long) = downloadManager.resumeDownload(chapterId)
    fun retryDownload(chapterId: Long) = downloadManager.retryDownload(chapterId)
    fun cancelDownload(chapterId: Long) = downloadManager.cancelDownload(chapterId)

    fun createArchive(comicId: Long, chapterIds: List<Long>, onProgress: (Float) -> Unit, onDone: (Result<ZipArchive>) -> Unit) {
        viewModelScope.launch {
            val result = archiveManager.createZipArchive(comicId, chapterIds, onProgress)
            refreshStorageStats()
            onDone(result)
        }
    }

    fun deleteArchive(archive: ZipArchive) {
        viewModelScope.launch {
            archiveManager.deleteArchive(archive)
            refreshStorageStats()
        }
    }

    fun deleteComic(comic: Comic) {
        viewModelScope.launch {
            comicRepository.deleteComic(comic)
            refreshStorageStats()
            if (_currentScreen.value is Screen.ComicDetails) {
                navigateBack()
            }
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            comicRepository.deleteChapterFiles(chapter)
            refreshStorageStats()
        }
    }
}
