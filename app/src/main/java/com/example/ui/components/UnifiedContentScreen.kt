package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlaylistEntity
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab

@Composable
fun UnifiedContentScreen(
    selectedTab: AppTab,
    songs: List<Song>,
    folders: Map<String, List<Song>>,
    podcasts: List<Song>,
    favorites: List<Song>,
    userPlaylists: List<PlaylistEntity>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onFolderClick: (String) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "🧪 Unified View (Experimental)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        when (selectedTab) {
            AppTab.LIBRARY -> {
                items(songs, key = { "lib_${it.id}" }, contentType = { "music" }) { song ->
                    UnifiedRow(
                        icon = Icons.Default.MusicNote,
                        iconTint = if (currentSong?.id == song.id) NeonCyan else Color(0xFF94A3B8),
                        title = song.title,
                        subtitle = "${song.artist} • ${song.formattedDuration}"
                    ) { onSongClick(song) }
                }
            }
            AppTab.FOLDERS -> {
                items(folders.keys.toList(), key = { "fld_$it" }, contentType = { "folder" }) { folderName ->
                    val folderSongs = folders[folderName] ?: return@items
                    UnifiedRow(
                        icon = Icons.Default.Folder,
                        iconTint = NeonEmerald,
                        title = folderName,
                        subtitle = "${folderSongs.size} tracks"
                    ) { onFolderClick(folderName) }
                }
            }
            AppTab.PODCASTS -> {
                items(podcasts, key = { "pod_${it.id}" }, contentType = { "podcast" }) { item ->
                    UnifiedRow(
                        icon = Icons.Default.Headphones,
                        iconTint = if (currentSong?.id == item.id) ElectricBlue else Color(0xFF94A3B8),
                        title = item.title,
                        subtitle = "${item.genre} • ${item.formattedDuration}"
                    ) { onSongClick(item) }
                }
            }
            AppTab.PLAYLISTS -> {
                items(favorites, key = { "fav_${it.id}" }, contentType = { "favorite" }) { song ->
                    UnifiedRow(
                        icon = Icons.Default.MusicNote,
                        iconTint = NeonAmber,
                        title = "★ ${song.title}",
                        subtitle = "${song.artist} • ${song.formattedDuration}"
                    ) { onSongClick(song) }
                }
                items(userPlaylists, key = { "pl_${it.id}" }, contentType = { "playlist" }) { pl ->
                    UnifiedRow(
                        icon = Icons.Default.MusicNote,
                        iconTint = NeonAmber,
                        title = pl.name,
                        subtitle = "${pl.getSongIds().size} tracks"
                    ) { onPlaylistClick(pl) }
                }
            }
        }
    }
}

@Composable
private fun UnifiedRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
