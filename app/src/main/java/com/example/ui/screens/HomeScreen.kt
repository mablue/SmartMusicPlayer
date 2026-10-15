package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RankTier
import com.example.data.model.Song
import com.example.ui.components.AlgorithmInspectorDialog
import com.example.ui.components.AnalyticsTab
import com.example.ui.components.EvaluationBanner
import com.example.ui.components.FullPlayerContent
import com.example.ui.components.MiniPlayerBar
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

    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val currentSong by viewModel.player.currentSong.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.player.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.player.durationMs.collectAsStateWithLifecycle()
    val playbackMode by viewModel.player.playbackMode.collectAsStateWithLifecycle()
    val lastEvaluation by viewModel.player.lastEvaluation.collectAsStateWithLifecycle()

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

    // Back handler to collapse full player if open
    BackHandler(enabled = uiState.showFullPlayer) {
        viewModel.setShowFullPlayer(false)
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
            onCollapse = { viewModel.setShowFullPlayer(false) }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NeonViolet.copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
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
                                Text(
                                    text = "رتبه‌بندی انطباقی و شافل هوشمند",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonCyan,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    },
                    actions = {
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
                                    contentDescription = "اسکن حافظه دستگاه",
                                    tint = NeonCyan
                                )
                            }
                        }

                        // Algorithm Info Button
                        IconButton(
                            onClick = { showGeneralInfoDialog = true },
                            modifier = Modifier.testTag("algorithm_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "راهنمای الگوریتم",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
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
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.LIBRARY,
                            onClick = { viewModel.setSelectedTab(AppTab.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "کتابخانه") },
                            label = { Text("کتابخانه", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.RANKINGS,
                            onClick = { viewModel.setSelectedTab(AppTab.RANKINGS) },
                            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "رتبه‌بندی") },
                            label = { Text("رتبه‌بندی", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonAmber,
                                selectedTextColor = NeonAmber,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.DISCOVERY,
                            onClick = { viewModel.setSelectedTab(AppTab.DISCOVERY) },
                            icon = {
                                Box {
                                    Icon(Icons.Default.Explore, contentDescription = "نشنیده‌ها")
                                    if (unplayedSongs.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = NeonViolet,
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {}
                                    }
                                }
                            },
                            label = { Text("نشنیده‌ها (${unplayedSongs.size})", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonViolet,
                                selectedTextColor = NeonViolet,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )

                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.ANALYTICS,
                            onClick = { viewModel.setSelectedTab(AppTab.ANALYTICS) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "آمار") },
                            label = { Text("تحلیل و آمار", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = NeonEmerald,
                                selectedTextColor = NeonEmerald,
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
                // Live Evaluation Feedback Banner (shows on skip or complete)
                EvaluationBanner(
                    evaluation = lastEvaluation,
                    onDismiss = { viewModel.dismissEvaluation() }
                )

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
                                Icon(Icons.Default.Clear, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(16.dp))
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
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onTierSelect = { viewModel.setTierFilter(it) },
                                    onSongClick = { viewModel.playSong(it) },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) }
                                )
                            }

                            AppTab.RANKINGS -> {
                                RankingsTabContent(
                                    rankedSongs = rankedSongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongClick = { viewModel.playSong(it) },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) }
                                )
                            }

                            AppTab.DISCOVERY -> {
                                DiscoveryTabContent(
                                    unplayedSongs = unplayedSongs,
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongClick = { viewModel.playSong(it) },
                                    onInfoClick = { viewModel.setInspectingSong(it) },
                                    onDeleteClick = { viewModel.requestDeleteSong(it) }
                                )
                            }

                            AppTab.ANALYTICS -> {
                                AnalyticsTab(
                                    songs = allSongs,
                                    onResetAllScores = { viewModel.resetAllScores() }
                                )
                            }
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
                    text = "حذف قطعه از کتابخانه",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "آیا از حذف آهنگ «${song.title}» مطمئن هستید؟ این قطعه از پلی‌لیست و الگوریتم رتبه‌بندی پاک می‌شود.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteSong() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("حذف شود", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("انصراف", color = Color.White)
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
            title = "نمونه الگوریتم هوشمند",
            artist = "راهنمای رنک",
            isUnplayed = true,
            score = 50.0f
        )
        AlgorithmInspectorDialog(
            song = dummySeedSong,
            onDismiss = { showGeneralInfoDialog = false }
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
                    text = "کتابخانه موسیقی خالی است",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "آهنگ‌های ذخیره‌شده روی گوشی خود را اسکن کنید. تمام فایل‌ها با امتیاز اولیه ۵۰ ثبت می‌شوند تا شانس شنیده شدن داشته باشند و با رفتار گوش دادن شما رتبه‌بندی شوند.",
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
                        text = "اسکن آهنگ‌های گوشی",
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
    onSearchChange: (String) -> Unit,
    onTierSelect: (RankTier?) -> Unit,
    onSongClick: (Song) -> Unit,
    onInfoClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("جستجوی نام آهنگ، خواننده یا سبک...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = Color.White)
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
                    .fillMaxWidth()
                    .testTag("search_input")
            )
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
                    label = { Text("همه (${songs.size})", fontSize = 12.sp) },
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
                    label = { Text(tier.labelFa, fontSize = 12.sp) },
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
                        text = "هیچ آهنگی با این فیلتر یافت نشد",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        onSongClick = { onSongClick(song) },
                        onInfoClick = { onInfoClick(song) },
                        onDeleteClick = { onDeleteClick(song) }
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
    onDeleteClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = NeonAmber.copy(alpha = 0.2f), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "لیدربورد محبوبیت آهنگ‌ها",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "مرتب شده بر اساس رتبه هوشمند و رفتار گوش دادن شما",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }
        }

        itemsIndexed(rankedSongs, key = { _, s -> s.id }) { index, song ->
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
                    modifier = Modifier.size(32.dp)
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

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    SongListItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        onSongClick = { onSongClick(song) },
                        onInfoClick = { onInfoClick(song) },
                        onDeleteClick = { onDeleteClick(song) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
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
    onDeleteClick: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = NeonViolet.copy(alpha = 0.2f), modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "صف اکتشاف آهنگ‌های نشنیده",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text(
                                text = "امتیاز شانس ۵۰ | آماده برای ارزیابی اولین پخش",
                                style = MaterialTheme.typography.bodySmall.copy(color = NeonViolet)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "این قطعات هنوز هیچ بار گوش داده نشده‌اند و با ۵۰ امتیاز در صف شافل قرار دارند تا شانس شنیده شدن داشته باشند. با اولین پخش، امتیاز ۵۰ ریست می‌شود!",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), lineHeight = 18.sp)
                    )
                }
            }
        }

        if (unplayedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "تبریک! تمام قطعات کتابخانه را حداقل یکبار شنیده‌اید.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "همه آهنگ‌ها بر اساس سلیقه و رفتار واقعی‌تان رتبه‌بندی شده‌اند.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }
        } else {
            items(unplayedSongs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying,
                    onSongClick = { onSongClick(song) },
                    onInfoClick = { onInfoClick(song) },
                    onDeleteClick = { onDeleteClick(song) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
