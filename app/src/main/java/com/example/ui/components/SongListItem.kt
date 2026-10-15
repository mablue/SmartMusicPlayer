package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RankTier
import com.example.data.model.Song
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
fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = when (song.rankTier) {
        RankTier.DIAMOND -> ElectricBlue
        RankTier.GOLD -> NeonAmber
        RankTier.UNPLAYED -> NeonViolet
        RankTier.SILVER -> Color(0xFF94A3B8)
        RankTier.DISLIKED -> NeonRose
    }

    val containerColor = if (isCurrentSong) {
        DarkSurfaceVariant
    } else {
        DarkSurface
    }

    val borderColor = if (isCurrentSong) {
        tierColor.copy(alpha = 0.8f)
    } else {
        DarkSurfaceBorder.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSongClick)
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isCurrentSong) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song Thumbnail / Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tierColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCurrentSong && isPlaying) {
                        VisualizerWaves(
                            isPlaying = true,
                            barCount = 4,
                            width = 22.dp,
                            height = 18.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Artist & Metrics
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrentSong) NeonCyan else Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Rank Tier Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, tierColor)
                    ) {
                        Text(
                            text = if (song.isUnplayed) "نشنیده: ۵۰" else "${song.score.toInt()} رنک",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(text = "•", color = Color(0xFF64748B), fontSize = 10.sp)

                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Stats row: Play Count, Skip Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎧", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${song.playCount} پخش کامل",
                            fontSize = 11.sp,
                            color = if (song.playCount > 0) NeonEmerald else Color(0xFF64748B)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏭️", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${song.skipCount} اسکیپ",
                            fontSize = 11.sp,
                            color = if (song.skipCount > 0) NeonRose else Color(0xFF64748B)
                        )
                    }
                }
            }

            // Action Buttons: Delete, Info, Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("delete_song_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف آهنگ",
                        tint = NeonRose.copy(alpha = 0.8f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Info Button
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "جزئیات رنک",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = onSongClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isCurrentSong) tierColor else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .testTag("play_song_${song.id}")
                ) {
                    Icon(
                        imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentSong && isPlaying) "توقف" else "پخش",
                        tint = if (isCurrentSong) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
