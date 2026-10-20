package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.data.model.SongSortOrder
import com.example.ui.components.FoldersTabContent
import com.example.ui.components.PlaylistsTabContent
import com.example.ui.components.PodcastsTabContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.RankTier
import com.example.data.model.Song
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AlgorithmInspectorDialog
import com.example.ui.components.AnalyticsTab
import com.example.ui.components.CarModeDialog
import com.example.ui.components.EditSongTagsDialog
import com.example.ui.components.FullPlayerContent
import com.example.ui.components.LyricsViewDialog
import com.example.ui.components.LyricsPanel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SongListItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.util.SongShareHelper
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.QueueSheet
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.UserPlaylistsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val rankedSongs by viewModel.rankedSongs.collectAsStateWithLifecycle()
    val unplayedSongs by viewModel.unplayedSongs.collectAsStateWithLifecycle()
    val filteredLibrarySongs by viewModel.filteredLibrarySongs.collectAsStateWithLifecycle()
    val podcastAndAudiobooks by viewModel.podcastAndAudiobooks.collectAsStateWithLifecycle()
    val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val queueState by viewModel.player.queue.collectAsStateWithLifecycle()
    val userPlaylistsState by viewModel.userPlaylists.collectAsStateWithLifecycle()

    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val currentSong by viewModel.player.currentSong.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.player.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.player.durationMs.collectAsStateWithLifecycle()
    val playbackMode by viewModel.player.playbackMode.collectAsStateWithLifecycle()
    val lastEvaluation by viewModel.player.lastEvaluation.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val abLoopActive by viewModel.abLoopActive.collectAsStateWithLifecycle()
    val abLoopStartMs by viewModel.abLoopStartMs.collectAsStateWithLifecycle()
    val abLoopEndMs by viewModel.abLoopEndMs.collectAsStateWithLifecycle()
    val lyricsResult by viewModel.lyricsResult.collectAsStateWithLifecycle()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsStateWithLifecycle()
    val isTranslatingLyrics by viewModel.isTranslatingLyrics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showGeneralInfoDialog by remember { mutableStateOf(false) }

    // Media permission for scanning device audio
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.scanDeviceStorage()
    }

    // System delete launcher for Android 11+ Scoped Storage physical deletion
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            uiState.songToDelete?.let { song ->
                viewModel.finalizeSongDeletion(song)
            }
        } else {
            viewModel.clearPendingDeleteIntent()
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.pendingDeleteIntentSender) {
        val sender = uiState.pendingDeleteIntentSender
        if (sender != null) {
            deleteIntentLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            )
        }
    }

    // Back handler to collapse full player if open
    // Back handler: exit selection mode first (do NOT close app)
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.toggleSelectionMode(false)
    }

    // Predictive Back for Full Player (developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
    PredictiveBackHandler(enabled = uiState.showFullPlayer) { progress ->
        try {
            progress.collect { backEvent ->
                // Animate the player card scale/alpha based on backEvent.progress (0.0 to 1.0)
                // This gives the user a preview of the dismissal.
                val scale = 1f - (backEvent.progress * 0.15f)
                // The actual animation is handled by the system's Predictive Back.
                // For custom UI, you can observe progress here.
            }
            // Gesture committed: close full player
            viewModel.setShowFullPlayer(false)
        } catch (e: CancellationException) {
            // Gesture cancelled: do nothing, player stays open
        }
    }

    // If user opened full player, show full player view
    if (uiState.showFullPlayer) {
        FullPlayerContent(
            song = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            playbackMode = playbackMode,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onModeChange = { viewModel.setPlaybackMode(it) },
            onSimulateQuickSkip = { viewModel.simulateQuickSkip() },
            onSimulateFullListen = { viewModel.simulateFullListen() },
            onSimulateModerateListen = { viewModel.simulateModerateListen() },
            onInfoClick = {
                currentSong?.let { viewModel.setInspectingSong(it) } ?: run { showGeneralInfoDialog = true }
            },
            onDeleteClick = { song -> viewModel.requestDeleteSong(song) },
            onCollapse = { viewModel.setShowFullPlayer(false) },
            onLyricsClick = { viewModel.toggleLyricsPanel() },
            isBluetoothAutoPlay = uiState.isBluetoothAutoPlay,
            onToggleBluetoothAutoPlay = { viewModel.toggleBluetoothAutoPlay() },
            playbackSpeed = playbackSpeed,
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            sleepTimerMinutes = sleepTimerMinutes,
            onSleepTimerChange = { viewModel.setSleepTimer(it) },
            onSeekRelative = { viewModel.seekRelative(it) },
            onShareClick = { currentSong?.let { SongShareHelper.shareSong(context, it) } },
            onEditTagsClick = { currentSong?.let { viewModel.setEditingSongTags(it) } },
            onCarModeClick = { viewModel.setShowCarMode(true) },
            onRestartFromBeginning = { viewModel.restartCurrentSong() },
            isABLoopActive = abLoopActive,
            abLoopStartMs = abLoopStartMs,
            abLoopEndMs = abLoopEndMs,
            onSetABLoopStart = { viewModel.setABLoopStart() },
            onSetABLoopEnd = { viewModel.setABLoopEnd() },
            onClearABLoop = { viewModel.clearABLoop() },
            onEqualizerClick = { viewModel.setEqualizerVisible(true) },
            onQueueClick = { viewModel.setQueueVisible(true) }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (uiState.isSelectionMode) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                            }
                        },
                        title = {
                            Text(
                                text = "${uiState.selectedSongIds.size} مورد انتخاب شد",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        },
                        actions = {
                            // Select All
                            IconButton(onClick = { viewModel.selectAllSongs(filteredLibrarySongs) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "انتخاب همه", tint = NeonCyan)
                            }

                            if (uiState.selectedSongIds.isNotEmpty()) {
                                // Play Selected
                                IconButton(onClick = { viewModel.playSelectedSongs() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "پخش انتخاب‌ها", tint = NeonEmerald)
                                }

                                // Add Selected to Playlist
                                IconButton(onClick = {
                                    viewModel.setAddToPlaylistVisible(true, uiState.selectedSongIds.toList())
                                }) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = "افزودن به پلی‌لیست", tint = NeonAmber)
                                }

                                // Toggle category between Music and Podcast
                                IconButton(onClick = {
                                    val isCurrentlyInPodcasts = uiState.selectedTab == AppTab.PODCASTS
                                    viewModel.setSelectedSongsCategory(!isCurrentlyInPodcasts)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "تغییر دسته‌بندی",
                                        tint = NeonViolet
                                    )
                                }

                                // Share Selected
                                IconButton(onClick = {
                                    val selected = allSongs.filter { uiState.selectedSongIds.contains(it.id) }
                                    if (selected.isNotEmpty()) {
                                        com.example.util.SongShareHelper.shareMultipleSongs(context, selected)
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = Color.White)
                                }

                                // Delete Selected
                                IconButton(onClick = { viewModel.deleteSelectedSongs() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = NeonRose)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonViolet.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = NeonViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "SmartMusic",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        },
                        actions = {
                            // User Playlists Button
                            IconButton(onClick = { viewModel.setUserPlaylistsVisible(true) }) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "پلی‌لیست‌های من",
                                    tint = NeonAmber
                                )
                            }

                            // Equalizer & Audio FX Button
                            IconButton(onClick = { viewModel.setEqualizerVisible(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "اکولایزر و تنظیمات صدا",
                                    tint = NeonEmerald
                                )
                            }

                            // Multi-select toggle button
                            IconButton(onClick = { viewModel.toggleSelectionMode(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "انتخاب چندتایی",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            // Scan Storage Button
                            IconButton(
                                onClick = { permissionLauncher.launch(permissionToRequest) },
                                modifier = Modifier.testTag("scan_storage_button")
                            ) {
                                if (uiState.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = stringResource(R.string.scan_storage_tooltip),
                                        tint = NeonCyan
                                    )
                                }
                            }

                            // Settings Button
                            IconButton(
                                onClick = { viewModel.setShowSettings(true) },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkBackground
                        )
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Compact MiniPlayer Bar sitting right above Navigation Bar
                    if (currentSong != null) {
                        MiniPlayerBar(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onOpenFullPlayer = { viewModel.setShowFullPlayer(true) }
                        )
                    }

                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar")
                    ) {
                        // 1. Music Library
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.LIBRARY,
                            onClick = { viewModel.setSelectedTab(AppTab.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = stringResource(R.string.tab_library)) },
                            label = { Text(stringResource(R.string.tab_library), fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        // 2. Folders / File Manager
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.FOLDERS,
                            onClick = { viewModel.setSelectedTab(AppTab.FOLDERS) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.tab_folders)) },
                            label = { Text(stringResource(R.string.tab_folders), fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonEmerald,
                                selectedTextColor = NeonEmerald,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        // 3. Podcasts & Audiobooks
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.PODCASTS,
                            onClick = { viewModel.setSelectedTab(AppTab.PODCASTS) },
                            icon = { Icon(Icons.Default.Headphones, contentDescription = stringResource(R.string.tab_podcasts)) },
                            label = { Text(stringResource(R.string.tab_podcasts), fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        // 4. Playlists
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.PLAYLISTS,
                            onClick = { viewModel.setSelectedTab(AppTab.PLAYLISTS) },
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = "پلی‌لیست‌ها") },
                            label = { Text("پلی‌لیست‌ها", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonAmber,
                                selectedTextColor = NeonAmber,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                // Live evaluation feedback
                // Scan result feedback banner
                uiState.scanResultMsg?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearScanMessage() }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.btn_dismiss),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // If library has no songs at all, show empty prompt
                if (allSongs.isEmpty()) {
                    EmptyLibraryPrompt(onScanClick = { permissionLauncher.launch(permissionToRequest) })
                } else {
                    // Tab Content (takes full screen space now!)
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.selectedTab) {
                            AppTab.LIBRARY -> {
                                LibraryTabContent(
                                    songs = filteredLibrarySongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    searchQuery = uiState.searchQuery,
                                    selectedTier = uiState.selectedTierFilter,
                                    sortOrder = uiState.sortOrder,
                                    isSelectionMode = uiState.isSelectionMode,
                                    selectedSongIds = uiState.selectedSongIds,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onTierSelect = { viewModel.setTierFilter(it) },
                                    onSortChange = { viewModel.setSortOrder(it) },
                                    onSongClick = { viewModel.playSong(it) },
                                    onToggleSelectSong = { viewModel.toggleSongSelection(it) },
                                    onLongClickSong = { song ->
                                        viewModel.toggleSelectionMode(true)
                                        viewModel.toggleSongSelection(song.id)
                                    },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) },
                                    onEditTagsClick = { viewModel.setEditingSongTags(it) },
                                    onShareClick = { SongShareHelper.shareSong(context, it) },
                                    onConvertToPodcast = { viewModel.setSongCategory(it.id, isPodcast = true) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { viewModel.setAddToPlaylistVisible(true, listOf(it.id)) }
                                )
                            }

                            AppTab.FOLDERS -> {
                                FoldersTabContent(
                                    songs = allSongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongClick = { viewModel.playSong(it) },
                                    onPlayFolder = { folderSongs ->
                                        viewModel.player.currentPlaylist = folderSongs
                                        if (folderSongs.isNotEmpty()) viewModel.player.playSong(folderSongs.first())
                                    },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) },
                                    onEditTagsClick = { viewModel.setEditingSongTags(it) },
                                    onShareClick = { SongShareHelper.shareSong(context, it) }
                                )
                            }

                            AppTab.PODCASTS -> {
                                PodcastsTabContent(
                                    podcasts = podcastAndAudiobooks,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    playbackSpeed = playbackSpeed,
                                    onSongClick = { viewModel.playPodcast(it) },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) },
                                    onEditTagsClick = { viewModel.setEditingSongTags(it) },
                                    onShareClick = { SongShareHelper.shareSong(context, it) },
                                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                    onSeekRelative = { viewModel.seekRelative(it) },
                                    onConvertToMusic = { viewModel.setSongCategory(it.id, isPodcast = false) }
                                )
                            }

                            AppTab.PLAYLISTS -> {
                                PlaylistsTabContent(
                                    favoriteSongs = favoriteSongs,
                                    userPlaylists = userPlaylistsState,
                                    onPlayFavorites = { viewModel.playFavorites() },
                                    onPlayPlaylist = { viewModel.playPlaylist(it) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                    onCreatePlaylist = { viewModel.setUserPlaylistsVisible(true) }
                                )
                            }
                            else -> {}
                                                }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    uiState.songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_msg, song.title),
                    color = Color(0xFFCBD5E1),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteSong() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.btn_delete_confirm), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Inspecting Single Song Dialog
    uiState.inspectingSong?.let { song ->
        AlgorithmInspectorDialog(
            song = song,
            onDismiss = { viewModel.setInspectingSong(null) }
        )
    }

    // General Algorithm Explainer Dialog
    if (showGeneralInfoDialog) {
        val dummySeedSong = currentSong ?: allSongs.firstOrNull() ?: Song(
            id = 0,
            title = "Smart Algorithm Guide",
            artist = "Rank Guide",
            isUnplayed = true,
            score = 50.0f
        )
        AlgorithmInspectorDialog(
            song = dummySeedSong,
            onDismiss = { showGeneralInfoDialog = false }
        )
    }

    // Back handler closes lyrics panel first
    BackHandler(enabled = uiState.showLyricsPanel) {
        viewModel.toggleLyricsPanel()
    }

    // Full-screen Lyrics Panel (Apple Music style)
    if (uiState.showLyricsPanel) {
        LyricsPanel(
            songTitle = currentSong?.title ?: "—",
            artist = currentSong?.artist ?: "—",
            lyricsResult = lyricsResult,
            isLoading = isFetchingLyrics,
            isTranslating = isTranslatingLyrics,
            currentPositionMs = currentPositionMs,
            autoSearchLyrics = uiState.autoSearchLyrics,
            autoTranslateLyrics = uiState.autoTranslateLyrics,
            onToggleAutoSearch = { viewModel.toggleAutoSearchLyrics() },
            onToggleAutoTranslate = { viewModel.toggleAutoTranslateLyrics() },
            onSeekTo = { viewModel.seekTo(it) },
            onRetryFetch = { viewModel.fetchLyricsForCurrent() },
            onDismiss = { viewModel.toggleLyricsPanel() }
        )
    }

    // Live Lyrics and Translation Dialog
    if (uiState.showLyricsSheet) {
        LyricsViewDialog(
            song = currentSong,
            lyricsResult = lyricsResult,
            isLoading = isFetchingLyrics,
            isTranslating = isTranslatingLyrics,
            currentPositionMs = currentPositionMs,
            onSeekTo = { viewModel.seekTo(it) },
            onRetryFetch = { viewModel.fetchLyricsForCurrent() },
            onTranslate = { targetLang -> viewModel.translateLyrics(targetLang) },
            onDismiss = { viewModel.toggleLyricsPanel() }
        )
    }

    // Edit Song Tags Dialog
    uiState.editingSongTags?.let { song ->
        EditSongTagsDialog(
            song = song,
            onSave = { updated -> viewModel.saveSongTags(updated) },
            onDismiss = { viewModel.setEditingSongTags(null) }
        )
    }

    // App Settings Dialog
    if (uiState.showSettings) {
        SettingsDialog(
            isBluetoothAutoPlay = uiState.isBluetoothAutoPlay,
            onToggleBluetoothAutoPlay = { viewModel.toggleBluetoothAutoPlay() },
            isCarModeAutoLaunch = uiState.isCarModeAutoLaunch,
            onToggleCarModeAutoLaunch = { viewModel.toggleCarModeAutoLaunch() },
            isLyricsTranslation = uiState.isLyricsTranslation,
            onToggleLyricsTranslation = { viewModel.toggleLyricsTranslation() },
            targetLanguage = uiState.targetLanguage,
            onSelectTargetLanguage = { viewModel.setTargetLanguage(it) },
            onRescanStorage = {
                viewModel.setShowSettings(false)
                permissionLauncher.launch(permissionToRequest)
            },
            onResetScores = {
                viewModel.setShowSettings(false)
                viewModel.resetAllScores()
            },
            onDismiss = { viewModel.setShowSettings(false) }
        )
    }

    // Driving Car Mode Dialog
    if (uiState.showCarMode) {
        CarModeDialog(
            song = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekRelative = { viewModel.seekRelative(it) },
            onDismiss = { viewModel.setShowCarMode(false) }
        )
    }

    if (uiState.showEqualizer) {
        EqualizerDialog(
            audioEffectManager = viewModel.player.audioEffectManager,
            onDismiss = { viewModel.setEqualizerVisible(false) }
        )
    }

    if (uiState.showQueue) {
        QueueSheet(
            currentSong = currentSong,
            queue = queueState,
            isPlaying = isPlaying,
            onSongClick = { viewModel.playSong(it) },
            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
            onClearQueue = { viewModel.clearQueue() },
            onDismiss = { viewModel.setQueueVisible(false) }
        )
    }

    if (uiState.showAddToPlaylist) {
        AddToPlaylistDialog(
            songIds = uiState.songsToAddToPlaylist,
            playlists = userPlaylistsState,
            onCreatePlaylist = { n, ids -> viewModel.createPlaylist(n, ids) },
            onAddToPlaylist = { id, ids -> viewModel.addSongsToPlaylist(id, ids) },
            onDismiss = { viewModel.setAddToPlaylistVisible(false) }
        )
    }

    if (uiState.showUserPlaylists) {
        UserPlaylistsDialog(
            playlists = userPlaylistsState,
            onPlayPlaylist = { viewModel.playPlaylist(it) },
            onDeletePlaylist = { viewModel.deletePlaylist(it) },
            onDismiss = { viewModel.setUserPlaylistsVisible(false) }
        )
    }

}

@Composable
private fun EmptyLibraryPrompt(
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = NeonCyan.copy(alpha = 0.18f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.empty_library_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_library_desc),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.testTag("empty_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_scan_storage),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTabContent(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    searchQuery: String,
    selectedTier: RankTier?,
    sortOrder: SongSortOrder,
    isSelectionMode: Boolean,
    selectedSongIds: Set<Long>,
    onSearchChange: (String) -> Unit,
    onTierSelect: (RankTier?) -> Unit,
    onSortChange: (SongSortOrder) -> Unit,
    onSongClick: (Song) -> Unit,
    onToggleSelectSong: (Long) -> Unit,
    onLongClickSong: (Song) -> Unit,
    onInfoClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onEditTagsClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit,
    onConvertToPodcast: (Song) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort button with dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, if (sortOrder != SongSortOrder.DEFAULT) NeonCyan else DarkSurfaceBorder),
                    modifier = Modifier.size(52.dp)
                ) {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(R.string.btn_sort),
                            tint = if (sortOrder != SongSortOrder.DEFAULT) NeonCyan else Color(0xFF94A3B8)
                        )
                    }
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SongSortOrder.values().forEach { order ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.localized(),
                                    fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    color = if (order == sortOrder) NeonCyan else Color.White
                                )
                            },
                            onClick = {
                                showSortMenu = false
                                onSortChange(order)
                            }
                        )
                    }
                }
            }
        }

        // Tier Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTier == null,
                    onClick = { onTierSelect(null) },
                    label = { Text("${stringResource(R.string.filter_all)} (${songs.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    )
                )
            }
            items(RankTier.values()) { tier ->
                val chipColor = when (tier) {
                    RankTier.DIAMOND -> ElectricBlue
                    RankTier.GOLD -> NeonAmber
                    RankTier.UNPLAYED -> NeonViolet
                    RankTier.SILVER -> Color(0xFF94A3B8)
                    RankTier.DISLIKED -> NeonRose
                }
                FilterChip(
                    selected = selectedTier == tier,
                    onClick = { onTierSelect(if (selectedTier == tier) null else tier) },
                    label = { Text(tier.localizedLabel(), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    )
                )
            }
        }

        // List of Songs
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_filtered_songs),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songs, key = { it.id }, contentType = { "song" }) { song ->
                    SongListItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        onSongClick = { onSongClick(song) },
                        onInfoClick = { onInfoClick(song) },
                        onDeleteClick = { onDeleteClick(song) },
                        onEditTagsClick = { onEditTagsClick(song) },
                        onShareClick = { onShareClick(song) },
                        onConvertToPodcast = { onConvertToPodcast(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedSongIds.contains(song.id),
                        onToggleSelect = { onToggleSelectSong(song.id) },
                        onLongClick = { onLongClickSong(song) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RankingsTabContent(
    rankedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onInfoClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onEditTagsClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = NeonAmber.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.rankings_header_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = stringResource(R.string.rankings_header_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        itemsIndexed(rankedSongs, key = { _, s -> s.id }, contentType = { _, _ -> "song" }) { index, song ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rankNumber = index + 1
                val rankBadgeColor = when (rankNumber) {
                    1 -> NeonAmber
                    2 -> ElectricBlue
                    3 -> NeonEmerald
                    else -> Color(0xFF64748B)
                }

                Surface(
                    shape = CircleShape,
                    color = rankBadgeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, rankBadgeColor),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rankNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankBadgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    SongListItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        onSongClick = { onSongClick(song) },
                        onInfoClick = { onInfoClick(song) },
                        onDeleteClick = { onDeleteClick(song) },
                        onEditTagsClick = { onEditTagsClick(song) },
                        onShareClick = { onShareClick(song) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiscoveryTabContent(
    unplayedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onInfoClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onEditTagsClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = NeonViolet.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.discovery_header_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text(
                                text = stringResource(R.string.discovery_header_desc),
                                style = MaterialTheme.typography.bodySmall.copy(color = NeonViolet, fontSize = 11.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.discovery_header_info),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 16.sp)
                    )
                }
            }
        }

        if (unplayedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.all_songs_discovered_title),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.all_songs_discovered_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                        )
                    }
                }
            }
        } else {
            items(unplayedSongs, key = { it.id }, contentType = { "song" }) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying,
                    onSongClick = { onSongClick(song) },
                    onInfoClick = { onInfoClick(song) },
                    onDeleteClick = { onDeleteClick(song) },
                    onEditTagsClick = { onEditTagsClick(song) },
                    onShareClick = { onShareClick(song) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
