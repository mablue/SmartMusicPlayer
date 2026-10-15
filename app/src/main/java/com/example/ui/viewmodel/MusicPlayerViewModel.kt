package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.MusicDatabase
import com.example.data.model.RankTier
import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.engine.PlaybackMode
import com.example.player.SmartAudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String, val titleEn: String) {
    LIBRARY("کتابخانه", "Library"),
    RANKINGS("رتبه‌بندی", "Rankings"),
    DISCOVERY("نشنیده‌ها", "Discovery"),
    ANALYTICS("آمار و تحلیل", "Analytics")
}

data class PlayerUiState(
    val searchQuery: String = "",
    val selectedTierFilter: RankTier? = null,
    val selectedTab: AppTab = AppTab.LIBRARY,
    val inspectingSong: Song? = null,
    val songToDelete: Song? = null,
    val showFullPlayer: Boolean = false,
    val isScanning: Boolean = false,
    val scanResultMsg: String? = null
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = SongRepository(database.songDao(), application)
    val player = SmartAudioPlayer(application, repository, viewModelScope)

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rankedSongs: StateFlow<List<Song>> = repository.rankedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unplayedSongs: StateFlow<List<Song>> = repository.unplayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Filtered songs for Library tab
    val filteredLibrarySongs: StateFlow<List<Song>> = combine(allSongs, _uiState) { songs, state ->
        songs.filter { song ->
            val matchesQuery = state.searchQuery.isEmpty() ||
                    song.title.contains(state.searchQuery, ignoreCase = true) ||
                    song.artist.contains(state.searchQuery, ignoreCase = true) ||
                    song.album.contains(state.searchQuery, ignoreCase = true) ||
                    song.genre.contains(state.searchQuery, ignoreCase = true)
            val matchesTier = state.selectedTierFilter == null || song.rankTier == state.selectedTierFilter
            matchesQuery && matchesTier
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Remove any legacy default seed/procedural songs so only user's real music exists
            repository.removeDefaultSeedSongs()
        }

        // Keep player's playlist synchronized with current library
        viewModelScope.launch {
            allSongs.collect { songs ->
                player.currentPlaylist = songs
            }
        }
    }

    fun setSelectedTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setTierFilter(tier: RankTier?) {
        _uiState.value = _uiState.value.copy(selectedTierFilter = tier)
    }

    fun setInspectingSong(song: Song?) {
        _uiState.value = _uiState.value.copy(inspectingSong = song)
    }

    fun setShowFullPlayer(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFullPlayer = show)
    }

    fun requestDeleteSong(song: Song) {
        _uiState.value = _uiState.value.copy(songToDelete = song)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(songToDelete = null)
    }

    fun confirmDeleteSong() {
        val song = _uiState.value.songToDelete ?: return
        viewModelScope.launch {
            player.onSongDeleted(song.id)
            repository.deleteSong(song.id)
            if (_uiState.value.inspectingSong?.id == song.id) {
                _uiState.value = _uiState.value.copy(inspectingSong = null)
            }
            _uiState.value = _uiState.value.copy(
                songToDelete = null,
                scanResultMsg = "قطعه «${song.title}» از لیست حذف شد"
            )
        }
    }

    fun playSong(song: Song) {
        player.playSong(song)
    }

    fun togglePlayPause() {
        if (player.currentSong.value == null && allSongs.value.isNotEmpty()) {
            player.playSong(allSongs.value.first())
        } else {
            player.togglePlayPause()
        }
    }

    fun skipNext() {
        player.skipNext(isManual = true)
    }

    fun skipPrevious() {
        player.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        player.setPlaybackMode(mode)
    }

    fun simulateQuickSkip() {
        player.simulateSession(ratio = 0.10f, isManualSkip = true)
    }

    fun simulateFullListen() {
        player.simulateSession(ratio = 0.95f, isManualSkip = false)
    }

    fun simulateModerateListen() {
        player.simulateSession(ratio = 0.50f, isManualSkip = true)
    }

    fun dismissEvaluation() {
        player.dismissEvaluation()
    }

    fun resetAllScores() {
        viewModelScope.launch {
            repository.resetAllRanks()
            _uiState.value = _uiState.value.copy(
                scanResultMsg = "تمام رتبه‌ها به حالت اولیه نشنیده (۵۰ امتیاز) بازنشانی شدند"
            )
        }
    }

    fun scanDeviceStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanResultMsg = null)
            val result = repository.scanDeviceAudioFiles()
            val message = when {
                result.addedCount > 0 && result.skippedDuplicates > 0 ->
                    "${result.addedCount} آهنگ جدید با امتیاز اولیه ۵۰ افزوده شد (${result.skippedDuplicates} آهنگ تکراری نادیده گرفته شد)"
                result.addedCount > 0 ->
                    "${result.addedCount} آهنگ جدید با امتیاز شانس اولیه (۵۰) به کتابخانه اضافه شد"
                result.skippedDuplicates > 0 ->
                    "همه ${result.skippedDuplicates} آهنگ قبلاً در کتابخانه وجود داشتند و تکراری افزوده نشدند"
                else ->
                    "هیچ فایل صوتی در حافظه دستگاه یافت نشد"
            }
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                scanResultMsg = message
            )
        }
    }

    fun clearScanMessage() {
        _uiState.value = _uiState.value.copy(scanResultMsg = null)
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
