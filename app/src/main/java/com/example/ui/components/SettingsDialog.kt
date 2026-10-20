package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.util.FileLogger
import com.example.ui.theme.DarkSurfaceBorder
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet

@Composable
fun SettingsDialog(
    isBluetoothAutoPlay: Boolean,
    onToggleBluetoothAutoPlay: () -> Unit,
    isCarModeAutoLaunch: Boolean,
    onToggleCarModeAutoLaunch: () -> Unit,
    isLyricsTranslation: Boolean,
    onToggleLyricsTranslation: () -> Unit,
    targetLanguage: String,
    onSelectTargetLanguage: (String) -> Unit,
    onRescanStorage: () -> Unit,
    onResetScores: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLangDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = listOf(
        "fa" to "فارسی (Persian)",
        "en" to "English (انگلیسی)",
        "fr" to "Français (French)",
        "es" to "Español (Spanish)",
        "de" to "Deutsch (German)",
        "ar" to "العربية (Arabic)",
        "tr" to "Türkçe (Turkish)",
        "ru" to "Русский (Russian)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Playback & Automation
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_sec_playback),
                    color = NeonCyan
                )

                // Bluetooth Auto-Play
                SettingsToggleItem(
                    icon = Icons.Default.Bluetooth,
                    title = stringResource(R.string.settings_bt_autoplay_title),
                    subtitle = stringResource(R.string.settings_bt_autoplay_sub),
                    checked = isBluetoothAutoPlay,
                    onCheckedChange = { onToggleBluetoothAutoPlay() },
                    tint = ElectricBlue
                )

                // Auto Car Mode
                SettingsToggleItem(
                    icon = Icons.Default.DirectionsCar,
                    title = stringResource(R.string.settings_car_mode_title),
                    subtitle = stringResource(R.string.settings_car_mode_sub),
                    checked = isCarModeAutoLaunch,
                    onCheckedChange = { onToggleCarModeAutoLaunch() },
                    tint = NeonAmber
                )

                // Section 2: Lyrics & AI Translation
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_sec_lyrics),
                    color = NeonViolet
                )

                SettingsToggleItem(
                    icon = Icons.Default.Translate,
                    title = stringResource(R.string.settings_lyrics_trans_title),
                    subtitle = stringResource(R.string.settings_lyrics_trans_sub),
                    checked = isLyricsTranslation,
                    onCheckedChange = { onToggleLyricsTranslation() },
                    tint = NeonViolet
                )

                // Target Translation Language Picker
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(0.5.dp, Color(0xFF475569))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_target_lang),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                                val currentLabel = languages.firstOrNull { it.first == targetLanguage }?.second ?: targetLanguage
                                Text(
                                    text = currentLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontSize = 11.sp)
                                )
                            }
                        }

                        Box {
                            OutlinedButton(
                                onClick = { showLangDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                            ) {
                                Text(stringResource(R.string.btn_change), fontSize = 11.sp, color = NeonCyan)
                            }

                            DropdownMenu(
                                expanded = showLangDropdown,
                                onDismissRequest = { showLangDropdown = false },
                                modifier = Modifier.background(DarkSurfaceVariant)
                            ) {
                                languages.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                color = if (code == targetLanguage) NeonCyan else Color.White,
                                                fontWeight = if (code == targetLanguage) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectTargetLanguage(code)
                                            showLangDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Library & Database
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_sec_library),
                    color = NeonEmerald
                )

                // Rescan Storage Button
                OutlinedButton(
                    onClick = {
                        onRescanStorage()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_rescan_btn), color = Color.White, fontSize = 12.sp)
                }

                // Reset Ranking Scores Button
                OutlinedButton(
                    onClick = {
                        onResetScores()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonRose, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_reset_ranks_btn), color = NeonRose, fontSize = 12.sp)
                }

                // Share Logs button
                OutlinedButton(
                    onClick = {
                        try {
                            val file = FileLogger.getLogFile()
                            if (file == null || !file.exists()) {
                                android.widget.Toast.makeText(context, "لاگ هنوز ساخته نشده", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "SmartMusic logs")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "ارسال لاگ‌ها"))
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "خطا: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ارسال لاگ‌های خطا", color = Color.White, fontSize = 12.sp)
                }

                // App Info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SmartMusic Player v2.5",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Adaptive AI Ranking • Live Lyrics • Per-Track Resume",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(stringResource(R.string.btn_done), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(0.5.dp, Color(0xFF475569))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tint.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = tint,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}
