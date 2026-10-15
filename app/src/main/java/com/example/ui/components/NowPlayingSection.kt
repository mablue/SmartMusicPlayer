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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RankTier
import com.example.data.model.Song
import com.example.engine.PlaybackMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
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
    modifier: Modifier = Modifier
) {
    var showModeMenu by remember { mutableStateOf(false) }

    val safeDuration = durationMs.coerceAtLeast(song?.durationMs ?: 180000L).coerceAtLeast(1000L)
    val progressFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

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
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Minimize handle / button, Title, Info, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "کوچک کردن پلیر",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "صفحه پخش موسیقی",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song != null) {
                    IconButton(
                        onClick = { onDeleteClick(song) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف آهنگ",
                            tint = NeonRose
                        )
                    }
                }

                IconButton(onClick = onInfoClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "فرمول رتبه",
                        tint = Color.White.copy(alpha = 0.85f)
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
                contentDescription = "کاور آلبوم",
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
            text = song?.title ?: "آهنگی در حال پخش نیست",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = song?.artist ?: "یک قطعه را برای پخش انتخاب کنید",
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
                    Text(
                        text = if (song.isUnplayed) "امتیاز شانس اولیه: ۵۰" else "امتیاز: ${song.score.toInt()}/100 • ${song.rankTier.labelFa}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentSec = (currentPositionMs / 1000).coerceAtLeast(0)
            val totalSec = (safeDuration / 1000).coerceAtLeast(0)
            val curTimeStr = "%02d:%02d".format(currentSec / 60, currentSec % 60)
            val totTimeStr = "%02d:%02d".format(totalSec / 60, totalSec % 60)

            Text(
                text = curTimeStr,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
            )
            Text(
                text = totTimeStr,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Mode Chip with Dropdown
        Box {
            Surface(
                onClick = { showModeMenu = true },
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = playbackMode.labelFa,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                                text = mode.labelFa,
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

        Spacer(modifier = Modifier.height(20.dp))

        // Main Controls: Previous, Play/Pause, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "قبلی",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

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
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Next / Skip Button
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "بعدی",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Algorithm Simulator Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceVariant,
            border = BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚡ آزمایش زنده رفتار الگوریتم:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSimulateQuickSkip,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonRose.copy(alpha = 0.2f),
                            contentColor = NeonRose
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اسکیپ سریع (-۲۰)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onSimulateModerateListen,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonAmber.copy(alpha = 0.2f),
                            contentColor = NeonAmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("شنیدن ۵۰٪ (+۵)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onSimulateFullListen,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonEmerald.copy(alpha = 0.2f),
                            contentColor = NeonEmerald
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("پخش کامل (+۲۰)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
