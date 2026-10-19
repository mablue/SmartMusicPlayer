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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioEffectManager
import com.example.player.EqPreset
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet

@Composable
fun EqualizerDialog(
    audioEffectManager: AudioEffectManager,
    onDismiss: () -> Unit
) {
    val isEnabled by audioEffectManager.isEnabled.collectAsState()
    val currentPreset by audioEffectManager.currentPreset.collectAsState()
    val bands by audioEffectManager.bands.collectAsState()
    val bassBoost by audioEffectManager.bassBoostStrength.collectAsState()
    val virtualizer by audioEffectManager.virtualizerStrength.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NeonCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "اکولایزر و تنظیمات صدا",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "پریست‌های حرفه‌ای و تقویت بیس",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { audioEffectManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.35f),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Selector Chips
                Text(
                    text = "سبک‌های صوتی پیش‌فرض",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonAmber
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(EqPreset.values()) { preset ->
                        FilterChip(
                            selected = currentPreset == preset,
                            onClick = { audioEffectManager.applyPreset(preset) },
                            enabled = isEnabled,
                            label = { Text(preset.titleFa, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // 5 Frequency Bands (Graphic Equalizer)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "بندهای فرکانسی (Graphic EQ)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )

                        bands.forEach { band ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = band.formattedFreq,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Medium
                                    )
                                    val dbValue = band.currentLevelMb / 100.0f
                                    Text(
                                        text = "%+.1f dB".format(dbValue),
                                        fontSize = 11.sp,
                                        color = if (band.currentLevelMb > 0) NeonEmerald else if (band.currentLevelMb < 0) NeonAmber else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = band.currentLevelMb.toFloat(),
                                    onValueChange = { newVal ->
                                        audioEffectManager.setBandLevel(band.index, newVal.toInt().toShort())
                                    },
                                    valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                    enabled = isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = NeonCyan,
                                        inactiveTrackColor = Color(0xFF334155)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }

                // Deep Bass Boost Slider
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تقویت بیس عمیق (Bass Boost)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${bassBoost / 10}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
                        }

                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { audioEffectManager.setBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonViolet,
                                activeTrackColor = NeonViolet,
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // 3D Virtualizer / Surround Sound Slider
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SurroundSound, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("صدای فراگیر سه‌بعدی (3D Virtualizer)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${virtualizer / 10}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                        }

                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { audioEffectManager.setVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonEmerald,
                                activeTrackColor = NeonEmerald,
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأیید و ذخیره", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
