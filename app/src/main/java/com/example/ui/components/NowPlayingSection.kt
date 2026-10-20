package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RankTier
import com.example.data.model.Song
import com.example.engine.PlaybackMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet

@Composable
fun FullPlayerContent(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackMode: PlaybackMode,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onSimulateQuickSkip: () -> Unit,
    onSimulateFullListen: () -> Unit,
    onSimulateModerateListen: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: (Song) -> Unit,
    onCollapse: () -> Unit,
    onLyricsClick: () -> Unit = {},
    isBluetoothAutoPlay: Boolean = true,
    onToggleBluetoothAutoPlay: () -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    sleepTimerMinutes: Int = 0,
    onSleepTimerChange: (Int) -> Unit = {},
    onSeekRelative: (Long) -> Unit = {},
    onShareClick: (() -> Unit)? = null,
    onEditTagsClick: (() -> Unit)? = null,
    onCarModeClick: (() -> Unit)? = null,
    onRestartFromBeginning: (() -> Unit)? = null,
    isABLoopActive: Boolean = false,
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null,
    onSetABLoopStart: (() -> Unit)? = null,
    onSetABLoopEnd: (() -> Unit)? = null,
    onClearABLoop: (() -> Unit)? = null,
    onEqualizerClick: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showModeMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var showABLoopMenu by remember { mutableStateOf(false) }

    val safeDuration = durationMs.coerceAtLeast(song?.durationMs ?: 180000L).coerceAtLeast(1000L)
    val progressFraction by remember(currentPositionMs, safeDuration) {
        derivedStateOf { (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
    }

    val tierColor = when (song?.rankTier) {
        RankTier.DIAMOND -> ElectricBlue
        RankTier.GOLD -> NeonAmber
        RankTier.UNPLAYED -> NeonViolet
        RankTier.SILVER -> Color(0xFF94A3B8)
        RankTier.DISLIKED -> NeonRose
        null -> NeonViolet
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Minimize handle / button, Title, Car Mode, Share, Edit, Bluetooth, Lyrics, Info, Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("full_player_collapse")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.btn_collapse),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = stringResource(R.string.now_playing_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Car Driving Mode Button
                if (onCarModeClick != null) {
                    IconButton(
                        onClick = onCarModeClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = stringResource(R.string.car_mode_title),
                            tint = NeonAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Share Song Button
                if (song != null && onShareClick != null) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_share_song),
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Edit Song Tags Button
                if (song != null && onEditTagsClick != null) {
                    IconButton(
                        onClick = onEditTagsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_edit_tags),
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (song != null) {
                    // Lyrics HUD Dialog Trigger
                    IconButton(
                        onClick = onLyricsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("full_player_lyrics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = stringResource(R.string.action_lyrics),
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Equalizer Button
                    if (onEqualizerClick != null) {
                        IconButton(
                            onClick = onEqualizerClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "اکولایزر",
                                tint = NeonEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Queue (Up Next) Button
                    if (onQueueClick != null) {
                        IconButton(
                            onClick = onQueueClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "صف پخش",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Delete Song Button
                    IconButton(
                        onClick = { onDeleteClick(song) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("full_player_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete_song),
                            tint = NeonRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Rank Details Explainer
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("full_player_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.action_rank_details),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Album Art
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(tierColor.copy(alpha = 0.35f), Color(0xFF161324)),
                        radius = 400f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.default_album_art_1792079403842),
                contentDescription = stringResource(R.string.now_playing_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visualizer Waves
        VisualizerWaves(
            isPlaying = isPlaying,
            barCount = 20,
            width = 160.dp,
            height = 24.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Song Title, Artist, and Rank Badge
        Text(
            text = song?.title ?: stringResource(R.string.no_song_playing),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = song?.artist ?: stringResource(R.string.select_song_prompt),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF94A3B8)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rank Badge + Stats Row
        if (song != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tierColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, tierColor)
                ) {
                    val tierLabel = song.rankTier.localizedLabel()
                    val badgeText = if (song.isUnplayed) {
                        stringResource(R.string.tier_unplayed_badge)
                    } else {
                        "${song.score.toInt()} pts • $tierLabel"
                    }
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "🎧 ${song.playCount} | ⏭️ ${song.skipCount}",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata HUD Chips: Genre, Language, Folder, Audio specs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Genre
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(0.5.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "🎵 ${song.genre}",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Language
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(0.5.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "🌐 ${song.detectedLanguage}",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Folder
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(0.5.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "📁 ${song.folderName}",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }

                // Size
                if (song.fileSizeBytes > 0) {
                    val sizeMb = song.fileSizeBytes.toDouble() / (1024.0 * 1024.0)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(0.5.dp, Color(0xFF475569))
                    ) {
                        Text(
                            text = "💾 " + stringResource(R.string.meta_size, sizeMb),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Slider
        Slider(
            value = progressFraction,
            onValueChange = { frac ->
                val newMs = (frac * safeDuration).toLong()
                onSeekTo(newMs)
            },
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color(0xFF332D4F)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentSec = (currentPositionMs / 1000).coerceAtLeast(0)
            val totalSec = (safeDuration / 1000).coerceAtLeast(0)
            val curTimeStr = "%02d:%02d".format(currentSec / 60, currentSec % 60)
            val totTimeStr = "%02d:%02d".format(totalSec / 60, totalSec % 60)

            Text(
                text = curTimeStr,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
            )

            if (currentPositionMs > 5000L && onRestartFromBeginning != null) {
                Surface(
                    onClick = onRestartFromBeginning,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                        Text(stringResource(R.string.action_restart_beginning), fontSize = 11.sp, color = NeonCyan)
                    }
                }
            }

            Text(
                text = totTimeStr,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chips Row: Playback Mode, Speed, and Sleep Timer
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playback Mode Chip with Dropdown
            Box {
                Surface(
                    onClick = { showModeMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modeIcon = when (playbackMode) {
                            PlaybackMode.SMART_SHUFFLE -> Icons.Default.AutoAwesome
                            PlaybackMode.RANKED_FIRST -> Icons.Default.TrendingUp
                            PlaybackMode.DISCOVERY -> Icons.Default.Explore
                            PlaybackMode.SEQUENTIAL -> Icons.Default.FormatListNumbered
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        }
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(playbackMode.stringRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                    }
                }

                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    PlaybackMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(mode.stringRes),
                                    color = if (mode == playbackMode) NeonCyan else Color.White,
                                    fontWeight = if (mode == playbackMode) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onModeChange(mode)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }

            // Playback Speed Chip with Dropdown
            Box {
                Surface(
                    onClick = { showSpeedMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ), maxLines = 1, softWrap = false
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${speed}x",
                                    color = if (speed == playbackSpeed) NeonAmber else Color.White,
                                    fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSpeedChange(speed)
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }

            // Sleep Timer Chip with Dropdown
            Box {
                Surface(
                    onClick = { showSleepMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (sleepTimerMinutes > 0) NeonEmerald else DarkSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (sleepTimerMinutes > 0) NeonEmerald else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Sleep",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (sleepTimerMinutes > 0) NeonEmerald else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSleepMenu,
                    onDismissRequest = { showSleepMenu = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    listOf(0, -1, 15, 30, 45, 60, 90).forEach { mins ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (mins) {
                                        0 -> stringResource(R.string.sleep_timer_off)
                                        -1 -> "پایان همین قطعه"
                                        else -> "$mins دقیقه"
                                    },
                                    color = if (mins == sleepTimerMinutes) NeonEmerald else Color.White,
                                    fontWeight = if (mins == sleepTimerMinutes) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSleepTimerChange(mins)
                                showSleepMenu = false
                            }
                        )
                    }
                }
            }

            // A-B Repeat Loop Chip with Dropdown
            Box {
                Surface(
                    onClick = { showABLoopMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isABLoopActive) Color(0xFFF472B6) else DarkSurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (isABLoopActive) Color(0xFFF472B6) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "A-B",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isABLoopActive) Color(0xFFF472B6) else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showABLoopMenu,
                    onDismissRequest = { showABLoopMenu = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ab_loop_set_a), color = Color.White) },
                        onClick = {
                            onSetABLoopStart?.invoke()
                            showABLoopMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ab_loop_set_b), color = Color.White) },
                        onClick = {
                            onSetABLoopEnd?.invoke()
                            showABLoopMenu = false
                        }
                    )
                    if (isABLoopActive) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ab_loop_clear), color = NeonRose) },
                            onClick = {
                                onClearABLoop?.invoke()
                                showABLoopMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Controls: Previous, -15s, Play/Pause, +15s, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.action_previous),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // -15s Rewind
            IconButton(
                onClick = { onSeekRelative(-15000L) },
                modifier = Modifier.size(48.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = stringResource(R.string.action_rewind_15),
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("15s", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Play / Pause Button
            Surface(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                color = NeonCyan,
                modifier = Modifier.size(68.dp),
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play),
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // +15s Forward
            IconButton(
                onClick = { onSeekRelative(15000L) },
                modifier = Modifier.size(48.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = stringResource(R.string.action_forward_15),
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("15s", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Next / Skip Button
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.action_next),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))
    }
}
