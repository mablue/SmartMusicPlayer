package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditTagsClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onConvertToPodcast: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tierColor = when (song.rankTier) {
        RankTier.DIAMOND -> ElectricBlue
        RankTier.GOLD -> NeonAmber
        RankTier.UNPLAYED -> NeonViolet
        RankTier.SILVER -> Color(0xFF94A3B8)
        RankTier.DISLIKED -> NeonRose
    }

    val containerColor = when {
        isSelected -> NeonCyan.copy(alpha = 0.18f)
        isCurrentSong -> DarkSurfaceVariant
        else -> DarkSurface
    }
    val borderColor = when {
        isSelected -> NeonCyan
        isCurrentSong -> tierColor.copy(alpha = 0.8f)
        else -> DarkSurfaceBorder.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && onToggleSelect != null) onToggleSelect() else onSongClick()
                },
                onLongClick = {
                    onLongClick?.invoke() ?: onToggleSelect?.invoke()
                }
            )
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isCurrentSong || isSelected) 1.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCyan,
                        uncheckedColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

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
                        style = MaterialTheme.typography.bodyLarge.copy(
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (song.hasResumePosition) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonCyan.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = "⏱️ ${song.formattedResumePosition}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(text = "•", color = Color(0xFF64748B), fontSize = 11.sp)

                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    )

                    if (song.playCount > 0) {
                        Text(text = "•", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = "🎧${song.playCount}",
                            fontSize = 11.sp,
                            color = NeonEmerald
                        )
                    }

                    if (song.skipCount > 0) {
                        Text(text = "•", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = "⏭️${song.skipCount}",
                            fontSize = 11.sp,
                            color = NeonRose
                        )
                    }
                }
            }

            // Compact Action Buttons: Options Menu, Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        // Edit Tags
                        // Play Next
                        if (onPlayNext != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                },
                                text = { Text("پخش به عنوان آهنگ بعدی", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onPlayNext()
                                }
                            )
                        }

                        // Add to Queue
                        if (onAddToQueue != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                },
                                text = { Text("افزودن به صف پخش", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onAddToQueue()
                                }
                            )
                        }

                        // Add to Playlist
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                                },
                                text = { Text("افزودن به پلی‌لیست", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onAddToPlaylist()
                                }
                            )
                        }

                        // Edit Tags
                        if (onEditTagsClick != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                                },
                                text = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_edit_tags), color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onEditTagsClick()
                                }
                            )
                        }

                        // Share Track
                        if (onShareClick != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                },
                                text = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_share_song), color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onShareClick()
                                }
                            )
                        }

                        // Convert to Podcast
                        if (onConvertToPodcast != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                                },
                                text = { Text("انتقال به پادکست‌ها", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onConvertToPodcast()
                                }
                            )
                        }

                        // Info / Rank details
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                            },
                            text = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_rank_details), color = Color.White) },
                            onClick = {
                                showMenu = false
                                onInfoClick()
                            }
                        )

                        // Delete Song
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = NeonRose, modifier = Modifier.size(18.dp))
                            },
                            text = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.action_delete_song), color = NeonRose) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }

                // Playback status indicator (non-interactive; tap row toggles)
                Box(
                    modifier = Modifier.size(20.dp).testTag("playback_indicator_${song.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentSong) {
                        Box(
                            modifier = Modifier.size(10.dp).background(
                                if (isPlaying) NeonEmerald else Color(0xFF64748B),
                                CircleShape
                            )
                        )
                    }
                }
            }
        }
    }
}
