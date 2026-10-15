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

    val containerColor = if (isCurrentSong) DarkSurfaceVariant else DarkSurface
    val borderColor = if (isCurrentSong) tierColor.copy(alpha = 0.8f) else DarkSurfaceBorder.copy(alpha = 0.4f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSongClick)
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isCurrentSong) 1.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Thumbnail / Icon (36dp)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tierColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCurrentSong && isPlaying) {
                        VisualizerWaves(
                            isPlaying = true,
                            barCount = 3,
                            width = 16.dp,
                            height = 14.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text Info: Line 1 Title + Rank pill, Line 2 Artist + Duration + Stats
            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Title and compact rank badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrentSong) NeonCyan else Color.White,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tierColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.5.dp, tierColor)
                    ) {
                        Text(
                            text = if (song.isUnplayed) {
                                androidx.compose.ui.res.stringResource(com.example.R.string.metric_unplayed_val)
                            } else {
                                "${song.score.toInt()}"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Line 2: Artist • Duration • Play & Skip counters (all in one compact line)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(text = "•", color = Color(0xFF64748B), fontSize = 9.sp)

                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    )

                    if (song.playCount > 0) {
                        Text(text = "•", color = Color(0xFF64748B), fontSize = 9.sp)
                        Text(
                            text = "🎧${song.playCount}",
                            fontSize = 10.sp,
                            color = NeonEmerald
                        )
                    }

                    if (song.skipCount > 0) {
                        Text(text = "•", color = Color(0xFF64748B), fontSize = 9.sp)
                        Text(
                            text = "⏭️${song.skipCount}",
                            fontSize = 10.sp,
                            color = NeonRose
                        )
                    }
                }
            }

            // Compact Action Buttons: Delete, Info, Play
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Delete Button (compact)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_song_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.action_delete_song),
                        tint = NeonRose.copy(alpha = 0.75f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Info Button (compact)
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.action_rank_details),
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Play / Pause Button (compact)
                IconButton(
                    onClick = onSongClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isCurrentSong) tierColor else Color.White.copy(alpha = 0.08f),
                            CircleShape
                        )
                        .testTag("play_song_${song.id}")
                ) {
                    Icon(
                        imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentSong && isPlaying) {
                            androidx.compose.ui.res.stringResource(com.example.R.string.action_pause)
                        } else {
                            androidx.compose.ui.res.stringResource(com.example.R.string.action_play)
                        },
                        tint = if (isCurrentSong) Color.Black else Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
