package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.MusicDatabase
import com.example.data.lyrics.LyricsResult
import com.example.data.model.RankTier
import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import com.example.data.preferences.PlaybackPreferences
import com.example.data.repository.SongRepository
import com.example.engine.PlaybackMode
import com.example.player.SmartAudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String, val titleEn: String) {
    LIBRARY("موزیک‌ها", "Music"),
    FOLDERS("پوشه‌ها", "Folders"),
    PODCASTS("پادکست‌ها", "Podcasts"),
    PLAYLISTS("پلی‌لیست‌ها", "Playlists")
}

@Immutable
data class PlayerUiState(
    val searchQuery: String = "",
    val selectedTierFilter: RankTier? = null,
    val selectedFolder: String? = null,
    val sortOrder: com.example.data.model.SongSortOrder = com.example.data.model.SongSortOrder.DEFAULT,
    val isSelectionMode: Boolean = false,
    val selectedSongIds: Set<Long> = emptySet(),
    val selectedTab: AppTab = AppTab.LIBRARY,
    val inspectingSong: Song? = null,
    val songToDelete: Song? = null,
    val editingSongTags: Song? = null,
    val pendingDeleteIntentSender: android.content.IntentSender? = null,
    val showFullPlayer: Boolean = false,
    val showLyricsSheet: Boolean = false,
    val showSettings: Boolean = false,
    val showCarMode: Boolean = false,
    val isScanning: Boolean = false,
    val scanResultMsg: String? = null,
    val isBluetoothAutoPlay: Boolean = true,
    val isCarModeAutoLaunch: Boolean = false,
    val isLyricsTranslation: Boolean = true,
    val targetLanguage: String = "fa",
    val showEqualizer: Boolean = false,
    val showQueue: Boolean = false,
    val showAddToPlaylist: Boolean = false,
    val showUserPlaylists: Boolean = false,
    val songsToAddToPlaylist: List<Long> = emptyList(),
    val autoSearchLyrics: Boolean = true,
    val autoTranslateLyrics: Boolean = true,
    val showLyricsPanel: Boolean = false
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = SongRepository(database.songDao(), application)
    val playlistRepository = com.example.data.repository.PlaylistRepository(database.playlistDao(), database.songDao())
    private val prefs = PlaybackPreferences.getInstance(application)

    val player = SmartAudioPlayer(application, repository, viewModelScope)

    val userPlaylists: StateFlow<List<com.example.data.db.PlaylistEntity>> = playlistRepository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rankedSongs: StateFlow<List<Song>> = repository.rankedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unplayedSongs: StateFlow<List<Song>> = repository.unplayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            isBluetoothAutoPlay = prefs.isBluetoothAutoPlayEnabled,
            isCarModeAutoLaunch = prefs.isCarModeAutoLaunchEnabled,
            isLyricsTranslation = prefs.isLyricsTranslationEnabled,
            targetLanguage = prefs.targetTranslationLanguage,
            autoSearchLyrics = prefs.isLyricsAutoSearchEnabled,
            autoTranslateLyrics = prefs.isLyricsTranslationEnabled
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val lyricsResult: StateFlow<LyricsResult?> = player.lyricsResult
    val isFetchingLyrics: StateFlow<Boolean> = player.isFetchingLyrics
    val isTranslatingLyrics: StateFlow<Boolean> = player.isTranslatingLyrics
    val playbackSpeed: StateFlow<Float> = player.playbackSpeed
    val sleepTimerMinutes: StateFlow<Int> = player.sleepTimerMinutes
    val abLoopActive: StateFlow<Boolean> = player.abLoopActive
    val abLoopStartMs: StateFlow<Long?> = player.abLoopStartMs
    val abLoopEndMs: StateFlow<Long?> = player.abLoopEndMs

    // Distinct folders available in library
    val availableFolders: StateFlow<List<String>> = combine(allSongs) { songsArray ->
        val songs = songsArray[0]
        songs.map { it.folderName }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Music songs (excluding Podcasts & Audiobooks) with filtering, folder selection, and sorting
    val filteredLibrarySongs: StateFlow<List<Song>> = combine(allSongs, _uiState) { songs, state ->
        val query = state.searchQuery.trim().lowercase()
        val tier = state.selectedTierFilter
        val folder = state.selectedFolder

        var list = songs.filter { !it.isPodcastOrAudiobook }

        if (folder != null) {
            list = list.filter { it.folderName.equals(folder, ignoreCase = true) }
        }

        if (tier != null) {
            list = list.filter { it.rankTier == tier }
        }

        if (query.isNotEmpty()) {
            list = list.filter { song ->
                song.title.lowercase().contains(query) ||
                        song.artist.lowercase().contains(query) ||
                        song.album.lowercase().contains(query) ||
                        song.genre.lowercase().contains(query) ||
                        song.folderName.lowercase().contains(query)
            }
        }

        when (state.sortOrder) {
            com.example.data.model.SongSortOrder.DEFAULT -> list // already sorted by smart affinity / score
            com.example.data.model.SongSortOrder.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            com.example.data.model.SongSortOrder.DURATION_ASC -> list.sortedBy { it.durationMs }
            com.example.data.model.SongSortOrder.SIZE_DESC -> list.sortedByDescending { it.fileSizeBytes }
            com.example.data.model.SongSortOrder.SIZE_ASC -> list.sortedBy { it.fileSizeBytes }
            com.example.data.model.SongSortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            com.example.data.model.SongSortOrder.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dedicated Podcasts & Audiobooks list
    val podcastAndAudiobooks: StateFlow<List<Song>> = combine(allSongs, _uiState) { songs, state ->
        val query = state.searchQuery.trim().lowercase()
        var list = songs.filter { it.isPodcastOrAudiobook }
        if (query.isNotEmpty()) {
            list = list.filter { song ->
                song.title.lowercase().contains(query) ||
                        song.artist.lowercase().contains(query) ||
                        song.album.lowercase().contains(query) ||
                        song.folderName.lowercase().contains(query)
            }
        }
        list.sortedByDescending { it.lastPlayedTimestamp }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites / Top Loved songs (excluding Podcasts)
    val favoriteSongs: StateFlow<List<Song>> = combine(allSongs, _uiState) { songs, state ->
        val query = state.searchQuery.trim().lowercase()
        var list = songs.filter { !it.isPodcastOrAudiobook && (it.score >= 65.0f || it.playCount >= 2) }
        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
            }
        }
        list.sortedByDescending { it.score }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.removeDefaultSeedSongs()
        }

        // Keep player's playlist synchronized with current library and restore last played track
        viewModelScope.launch {
            allSongs.collect { songs ->
                val musicOnly = songs.filter { !it.isPodcastOrAudiobook }
                player.currentPlaylist = musicOnly
                player.ensureQueueInitialized(musicOnly)
                player.restoreLastSong(musicOnly)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
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

    fun setShowLyricsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLyricsSheet = show)
    }

    fun toggleBluetoothAutoPlay() {
        val newVal = !prefs.isBluetoothAutoPlayEnabled
        prefs.isBluetoothAutoPlayEnabled = newVal
        _uiState.value = _uiState.value.copy(isBluetoothAutoPlay = newVal)
    }

    fun fetchLyricsForCurrent() {
        player.currentSong.value?.let { player.fetchLyricsForSong(it, force = true) }
    }

    fun setShowSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }

    fun setShowCarMode(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCarMode = show)
    }

    fun setEditingSongTags(song: Song?) {
        _uiState.value = _uiState.value.copy(editingSongTags = song)
    }

    fun saveSongTags(updatedSong: Song) {
        viewModelScope.launch {
            repository.updateSong(updatedSong)
            player.updateCurrentSongMetadata(updatedSong)
            _uiState.value = _uiState.value.copy(
                editingSongTags = null,
                scanResultMsg = getApplication<Application>().getString(com.example.R.string.tags_updated_msg, updatedSong.title)
            )
        }
    }

    fun toggleCarModeAutoLaunch() {
        val newVal = !prefs.isCarModeAutoLaunchEnabled
        prefs.isCarModeAutoLaunchEnabled = newVal
        _uiState.value = _uiState.value.copy(isCarModeAutoLaunch = newVal)
    }

    fun toggleLyricsTranslation() {
        val newVal = !prefs.isLyricsTranslationEnabled
        prefs.isLyricsTranslationEnabled = newVal
        _uiState.value = _uiState.value.copy(isLyricsTranslation = newVal)
        if (newVal) {
            translateLyrics(prefs.targetTranslationLanguage)
        }
    }

    fun setTargetLanguage(lang: String) {
        prefs.targetTranslationLanguage = lang
        _uiState.value = _uiState.value.copy(targetLanguage = lang)
        translateLyrics(lang)
    }

    fun setABLoopStart() = player.setABLoopStart()
    fun setABLoopEnd() = player.setABLoopEnd()
    fun clearABLoop() = player.clearABLoop()
    fun restartCurrentSong() = player.restartCurrentSong()

    fun translateLyrics(targetLang: String) {
        player.translateCurrentLyrics(targetLang)
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
            val pendingIntent = repository.getDeletePendingIntent(song)
            if (pendingIntent != null) {
                _uiState.value = _uiState.value.copy(
                    pendingDeleteIntentSender = pendingIntent.intentSender
                )
            } else {
                finalizeSongDeletion(song)
            }
        }
    }

    fun finalizeSongDeletion(song: Song) {
        viewModelScope.launch {
            player.onSongDeleted(song.id)
            val fileDeleted = repository.deleteSongFileAndDb(song)
            if (_uiState.value.inspectingSong?.id == song.id) {
                _uiState.value = _uiState.value.copy(inspectingSong = null)
            }
            _uiState.value = _uiState.value.copy(
                songToDelete = null,
                pendingDeleteIntentSender = null,
                scanResultMsg = getApplication<Application>().getString(
                    com.example.R.string.song_deleted_msg,
                    song.title
                )
            )
        }
    }

    fun clearPendingDeleteIntent() {
        _uiState.value = _uiState.value.copy(pendingDeleteIntentSender = null)
    }

    fun playPodcast(song: Song) {
        if (player.currentSong.value?.id == song.id) {
            player.togglePlayPause(); return
        }
        val podcasts = podcastAndAudiobooks.value
        player.currentPlaylist = podcasts
        player.setQueue(podcasts)
        player.playSong(song)
    }

    fun playFavorites() {
        val songs = favoriteSongs.value
        if (songs.isNotEmpty()) {
            player.currentPlaylist = songs
            player.setQueue(songs)
            player.playSong(songs.first())
        }
    }

    fun playSong(song: Song) {
        if (player.currentSong.value?.id == song.id) {
            player.togglePlayPause(); return
        }
        if (player.currentPlaylist.none { it.id == song.id }) {
            player.currentPlaylist = allSongs.value.filter { !it.isPodcastOrAudiobook }
        }
        player.playSong(song)
    }

    fun togglePlayPause() {
        if (player.currentSong.value == null) {
            val fallback = player.currentPlaylist.firstOrNull()
                ?: allSongs.value.firstOrNull { !it.isPodcastOrAudiobook }
            if (fallback != null) player.playSong(fallback)
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

    fun seekRelative(deltaMs: Long) {
        player.seekRelative(deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        player.setSleepTimer(minutes)
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
                scanResultMsg = getApplication<Application>().getString(com.example.R.string.scan_result_ranks_reset)
            )
        }
    }

    fun scanDeviceStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanResultMsg = null)
            val result = repository.scanDeviceAudioFiles()
            val app = getApplication<Application>()
            val message = when {
                result.addedCount > 0 && result.skippedDuplicates > 0 ->
                    app.getString(com.example.R.string.scan_result_added_and_skipped, result.addedCount, result.skippedDuplicates)
                result.addedCount > 0 ->
                    app.getString(com.example.R.string.scan_result_added, result.addedCount)
                result.skippedDuplicates > 0 ->
                    app.getString(com.example.R.string.scan_result_all_duplicates, result.skippedDuplicates)
                else ->
                    app.getString(com.example.R.string.scan_result_none_found)
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

    fun setSortOrder(order: com.example.data.model.SongSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    fun setSelectedFolder(folder: String?) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder)
    }

    fun toggleSelectionMode(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isSelectionMode
        _uiState.value = _uiState.value.copy(
            isSelectionMode = next,
            selectedSongIds = if (next) _uiState.value.selectedSongIds else emptySet()
        )
    }

    fun toggleSongSelection(songId: Long) {
        val current = _uiState.value.selectedSongIds
        val next = if (current.contains(songId)) current - songId else current + songId
        _uiState.value = _uiState.value.copy(selectedSongIds = next)
    }

    fun selectAllSongs(songs: List<Song>) {
        _uiState.value = _uiState.value.copy(selectedSongIds = songs.map { it.id }.toSet())
    }

    fun clearSongSelection() {
        _uiState.value = _uiState.value.copy(selectedSongIds = emptySet())
    }

    fun playSelectedSongs() {
        val selected = allSongs.value.filter { _uiState.value.selectedSongIds.contains(it.id) }
        if (selected.isNotEmpty()) {
            player.currentPlaylist = selected
            player.setQueue(selected)
            player.playSong(selected.first())
            toggleSelectionMode(false)
        }
    }

    fun deleteSelectedSongs() {
        val selectedIds = _uiState.value.selectedSongIds
        viewModelScope.launch {
            val toDelete = allSongs.value.filter { selectedIds.contains(it.id) }
            toDelete.forEach { song ->
                repository.deleteSong(song.id)
            }
            toggleSelectionMode(false)
        }
    }

    fun setSongCategory(songId: Long, isPodcast: Boolean) {
        viewModelScope.launch {
            repository.updateSongGenre(songId, if (isPodcast) "Podcast" else "Music")
        }
    }

    fun setSelectedSongsCategory(isPodcast: Boolean) {
        val selectedIds = _uiState.value.selectedSongIds.toList()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                repository.updateSongsGenre(selectedIds, if (isPodcast) "Podcast" else "Music")
                toggleSelectionMode(false)
            }
        }
    }

    fun setEqualizerVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showEqualizer = visible)
    }

    fun setQueueVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showQueue = visible)
    }

    fun setAddToPlaylistVisible(visible: Boolean, songIds: List<Long> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            showAddToPlaylist = visible,
            songsToAddToPlaylist = songIds
        )
    }

    fun setUserPlaylistsVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showUserPlaylists = visible)
    }

    fun createPlaylist(name: String, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, songIds)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun playPlaylist(playlist: com.example.data.db.PlaylistEntity) {
        viewModelScope.launch {
            val songs = playlistRepository.getSongsForPlaylist(playlist)
            if (songs.isNotEmpty()) {
                player.currentPlaylist = songs
                player.setQueue(songs)
                player.playSong(songs.first())
            }
        }
    }

    fun playNext(song: Song) {
        player.playNext(song)
    }

    fun addToQueue(song: Song) {
        player.addToQueue(song)
    }

    fun removeFromQueue(songId: Long) {
        player.removeFromQueue(songId)
    }

    fun clearQueue() {
        player.clearQueue()
    }


    // --- Wave-to-Control (Proximity Sensor) ---
    private var waveControl: com.example.player.WaveControlManager? = null

    fun enableWaveControl() {
        if (waveControl == null) {
            waveControl = com.example.player.WaveControlManager(
                getApplication(),
                onWaveNext = { player.skipNext(isManual = true) },
                onWavePrevious = { player.skipPrevious() },
                onWavePlayPause = { player.togglePlayPause() }
            )
            waveControl?.start()
        }
    }

    fun disableWaveControl() {
        waveControl?.stop()
        waveControl = null
    }

    fun toggleAutoSearchLyrics() {
        val next = !_uiState.value.autoSearchLyrics
        prefs.isLyricsAutoSearchEnabled = next
        _uiState.value = _uiState.value.copy(autoSearchLyrics = next)
    }

    fun toggleAutoTranslateLyrics() {
        val next = !_uiState.value.autoTranslateLyrics
        prefs.isLyricsTranslationEnabled = next
        _uiState.value = _uiState.value.copy(autoTranslateLyrics = next, isLyricsTranslation = next)
    }

    fun toggleLyricsPanel() {
        val next = !_uiState.value.showLyricsPanel
        _uiState.value = _uiState.value.copy(showLyricsPanel = next)
        if (next) {
            currentSongForLyrics()?.let { fetchLyricsForCurrent() }
        }
    }

    private fun currentSongForLyrics() = player.currentSong.value

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
