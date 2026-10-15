package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
fun AnalyticsTab(
    songs: List<Song>,
    onResetAllScores: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    val totalSongs = songs.size
    val totalPlays = songs.sumOf { it.playCount }
    val totalSkips = songs.sumOf { it.skipCount }
    val unplayedCount = songs.count { it.isUnplayed }
    val avgScore = if (songs.isNotEmpty()) songs.map { it.score }.average().toFloat() else 50f

    val diamondCount = songs.count { !it.isUnplayed && it.score >= 80f }
    val goldCount = songs.count { !it.isUnplayed && it.score in 65f..79.9f }
    val neutralCount = songs.count { !it.isUnplayed && it.score in 45f..64.9f }
    val dislikedCount = songs.count { !it.isUnplayed && it.score < 45f }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "داشبورد تحلیلی الگوریتم هوشمند",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "ارزیابی زنده رفتار گوش دادن، نرخ اسکیپ و شانس پخش",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )
        }

        // Summary Statistics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "کل قطعات",
                    value = "$totalSongs",
                    icon = Icons.Default.Headphones,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "میانگین رتبه",
                    value = "${avgScore.toInt()}/100",
                    icon = Icons.Default.Equalizer,
                    color = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "تکمیل پخش",
                    value = "$totalPlays بار",
                    icon = Icons.Default.TrendingUp,
                    color = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "اسکیپ زودهنگام",
                    value = "$totalSkips بار",
                    icon = Icons.Default.TrendingDown,
                    color = NeonRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tier Breakdown Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "دسته‌بندی آهنگ‌ها بر اساس رتبه:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TierProgressRow("الماس و محبوب (۸۰+)", diamondCount, totalSongs, ElectricBlue)
                    TierProgressRow("طلایی و مطلوب (۶۵ - ۷۹)", goldCount, totalSongs, NeonAmber)
                    TierProgressRow("نشنیده‌ها (امتیاز پایه ۵۰)", unplayedCount, totalSongs, NeonViolet)
                    TierProgressRow("معمولی / خنثی (۴۵ - ۶۴)", neutralCount, totalSongs, Color(0xFF94A3B8))
                    TierProgressRow("اسکیپ‌شده / کم‌علاقه (< ۴۵)", dislikedCount, totalSongs, NeonRose)
                }
            }
        }

        // Mathematical Formula Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "فرمول انتخاب و امتیازدهی:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "۱. آهنگ‌های نشنیده: با ۵۰ امتیاز وارد سیستم می‌شوند تا در شافل فرصت پخش داشته باشند.\n" +
                                "۲. اولین پخش: بلافاصله امتیاز پیش‌فرض ریست می‌شود؛ اگر زیر ۲۰ ثانیه اسکیپ کنید، امتیاز به ۲۰ تنزل می‌یابد؛ اگر کامل گوش دهید به ۸۰ ارتقا می‌یابد.\n" +
                                "۳. پخش‌های بعدی: اسکیپ ۲۰- امتیاز کسر می‌کند و پخش کامل ۲۰+ امتیاز می‌افزاید.\n" +
                                "۴. شافل هوشمند وزنی: شانس انتخاب هر آهنگ متناسب با (امتیاز)^۱.۳ است؛ بنابراین آهنگ محبوب با امتیاز ۹۰ شانس بسیار بیشتری نسبت به آهنگ اسکیپ‌شده با امتیاز ۵ دارد.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE2E8F0),
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }

        // Reset Button
        item {
            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRose),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                Text(
                    text = "ریست تمام امتیازات به حالت اولیه (۵۰ امتیاز نشنیده)",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = "ریست امتیازات", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید رتبه و آمار تمام آهنگ‌ها را ریست کنید؟ تمام قطعات مجدداً به عنوان نشنیده با امتیاز اولیه ۵۰ ثبت خواهند شد.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllScores()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                ) {
                    Text("بله، ریست شود", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("انصراف", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}

@Composable
private fun TierProgressRow(
    title: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = if (total > 0) count.toFloat() / total.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
            Text(text = "$count قطعه", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF26213B), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}
