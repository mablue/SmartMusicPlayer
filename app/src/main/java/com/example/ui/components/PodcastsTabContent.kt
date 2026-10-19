package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
fun PodcastsTabContent(
    podcasts: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onSongClick: (Song) -> Unit,
    onInfoClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onEditTagsClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onConvertToMusic: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) } // null = All, "Podcast", "Audiobook"

    val displayedList = remember(podcasts, selectedCategoryFilter) {
        val base = if (selectedCategoryFilter == null) {
            podcasts
        } else {
            podcasts.filter { it.genre.equals(selectedCategoryFilter, ignoreCase = true) }
        }
        // Natural mathematical episode & chapter sequence ordering
        base.sortedWith { a, b ->
            val numA = extractEpisodeOrder(a.title)
            val numB = extractEpisodeOrder(b.title)
            if (numA != numB) numA.compareTo(numB) else a.title.compareTo(b.title, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricBlue.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پادکست‌ها و کتب صوتی",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "جداسازی هوشمند از موزیک‌ها | ذخیره دقیق زمان و سرعت پخش",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Filter chips (All / Podcasts / Audiobooks) + Speed Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = { Text("همه (${podcasts.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricBlue,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = Color.White
                )
            )

            FilterChip(
                selected = selectedCategoryFilter == "Podcast",
                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "Podcast") null else "Podcast" },
                label = { Text("پادکست‌ها", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = Color.White
                )
            )

            FilterChip(
                selected = selectedCategoryFilter == "Audiobook",
                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "Audiobook") null else "Audiobook" },
                label = { Text("کتب صوتی", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonAmber,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurface,
                    labelColor = Color.White
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Speed chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeonViolet.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                    val nextSpeed = when (playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 1.75f
                        1.75f -> 2.0f
                        else -> 1.0f
                    }
                    onSpeedChange(nextSpeed)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("%.2fx".format(playbackSpeed), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
                }
            }
        }

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.podcasts_empty_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.podcasts_empty_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedList, key = { it.id }) { item ->
                    val isCurrent = currentSong?.id == item.id
                    PodcastEpisodeCard(
                        song = item,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onPlayClick = { onSongClick(item) },
                        onRewind15 = { if (isCurrent) onSeekRelative(-15_000L) },
                        onForward30 = { if (isCurrent) onSeekRelative(30_000L) },
                        onInfoClick = { onInfoClick(item) },
                        onEditTagsClick = { onEditTagsClick(item) },
                        onShareClick = { onShareClick(item) },
                        onDeleteClick = { onDeleteClick(item) },
                        onConvertToMusic = { onConvertToMusic(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun extractEpisodeOrder(title: String): Long {
    val clean = title.lowercase()
    val match = Regex("""(?:ep|episode|قسمت|فصل|ch|chapter|part|بخش)\s*[-_:]?\s*(\d+)""").find(clean)
    if (match != null) {
        return match.groupValues[1].toLongOrNull() ?: Long.MAX_VALUE
    }
    val leadingNum = Regex("""^(\d+)""").find(clean.trim())
    if (leadingNum != null) {
        return leadingNum.groupValues[1].toLongOrNull() ?: Long.MAX_VALUE
    }
    return Long.MAX_VALUE
}

@Composable
private fun PodcastEpisodeCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRewind15: () -> Unit,
    onForward30: () -> Unit,
    onInfoClick: () -> Unit,
    onEditTagsClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConvertToMusic: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) DarkSurfaceVariant else DarkSurface
        ),
        border = BorderStroke(
            if (isCurrent) 1.dp else 0.5.dp,
            if (isCurrent) ElectricBlue else DarkSurfaceBorder.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricBlue.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = onPlayClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.folderName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("انتقال به لیست موزیک‌ها") },
                            leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                showMenu = false
                                onConvertToMusic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit_tags)) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditTagsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share_song)) },
                            leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShareClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_song), color = NeonRose) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-row: Duration, File size, Resume badge, and Fast Skip buttons if playing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = (if (song.genre.equals("Audiobook", ignoreCase = true)) NeonAmber else ElectricBlue).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = song.genre,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (song.genre.equals("Audiobook", ignoreCase = true)) NeonAmber else ElectricBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${song.formattedDuration} • ${song.formattedFileSize}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                if (song.hasResumePosition) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonViolet.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "⏱️ ${song.formattedResumePosition}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isCurrent) {
                    // Quick skip buttons
                    IconButton(onClick = onRewind15, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "-15s",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onForward30, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "+30s",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
