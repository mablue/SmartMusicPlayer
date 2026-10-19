package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlaylistEntity
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose

@Composable
fun AddToPlaylistDialog(
    songIds: List<Long>,
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: (String, List<Long>) -> Unit,
    onAddToPlaylist: (Long, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "افزودن به پلی‌لیست (${songIds.size} قطعه)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isCreatingNew) {
                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ساخت پلی‌لیست جدید", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }

                    if (playlists.isEmpty()) {
                        Text(
                            text = "هنوز پلی‌لیستی نساخته‌اید. با دکمه بالا اولین پلی‌لیست خود را بسازید.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                    border = BorderStroke(1.dp, DarkSurfaceBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddToPlaylist(playlist.id, songIds)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(playlist.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${playlist.getSongIds().size} قطعه", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("نام پلی‌لیست") },
                        placeholder = { Text("مثلاً: آهنگ‌های شبانه") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCreatingNew = false }) {
                            Text("انصراف", color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName, songIds)
                                    onDismiss()
                                }
                            },
                            enabled = newPlaylistName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ایجاد و ذخیره", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isCreatingNew) {
                TextButton(onClick = onDismiss) {
                    Text("بستن", color = Color(0xFF94A3B8))
                }
            }
        }
    )
}

@Composable
fun UserPlaylistsDialog(
    playlists: List<PlaylistEntity>,
    onPlayPlaylist: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پلی‌لیست‌های من",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    text = "هیچ پلی‌لیستی ایجاد نشده است.\nبرای ساخت پلی‌لیست روی علامت سه نقطه آهنگ‌ها یا انتخاب چندگانه کلیک کنید و «افزودن به پلی‌لیست» را انتخاب نمایید.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonAmber.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.getSongIds().size} قطعه",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    )
                                }

                                IconButton(onClick = {
                                    onPlayPlaylist(playlist)
                                    onDismiss()
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "پخش پلی‌لیست", tint = NeonCyan)
                                }

                                IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف پلی‌لیست", tint = NeonRose)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = Color.White)
            }
        }
    )
}
