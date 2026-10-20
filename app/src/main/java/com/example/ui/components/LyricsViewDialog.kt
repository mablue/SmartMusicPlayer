package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.lyrics.LyricsResult
import com.example.data.model.Song
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet

@Composable
fun LyricsViewDialog(
    song: Song?,
    lyricsResult: LyricsResult?,
    isLoading: Boolean,
    isTranslating: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onRetryFetch: () -> Unit,
    onTranslate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showPersianTranslation by remember { mutableStateOf(java.util.Locale.getDefault().language == "fa") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            border = BorderStroke(1.5.dp, Brush.verticalGradient(listOf(NeonCyan, NeonViolet)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.lyrics_dialog_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${song?.title ?: ""} • ${song?.artist ?: ""}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_dismiss),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Controls & Translation Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Synced / Plain Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (lyricsResult?.isSynced == true) NeonEmerald.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (lyricsResult?.isSynced == true) NeonEmerald else DarkSurfaceBorder)
                    ) {
                        Text(
                            text = if (lyricsResult?.isSynced == true) stringResource(R.string.lyrics_synced_hud) else stringResource(R.string.lyrics_plain_text),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lyricsResult?.isSynced == true) NeonEmerald else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Translate button / chip
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonAmber)
                        }

                        FilterChip(
                            selected = showPersianTranslation,
                            onClick = {
                                showPersianTranslation = !showPersianTranslation
                                if (showPersianTranslation) {
                                    val targetLang = if (song?.detectedLanguage == "Persian") "en" else "fa"
                                    onTranslate(targetLang)
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (showPersianTranslation) stringResource(R.string.lyrics_translation_active) else stringResource(R.string.lyrics_translation_toggle),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonViolet,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurface,
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(DarkSurface.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.lyrics_loading),
                                    style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan)
                                )
                            }
                        }

                        lyricsResult == null || lyricsResult.lines.isEmpty() -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.lyrics_not_found),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onRetryFetch,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.lyrics_retry), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            val lines = lyricsResult.lines
                            val listState = rememberLazyListState()

                            // Auto-scroll to active line
                            val activeIndex = remember(currentPositionMs, lines) {
                                val idx = lines.indexOfLast { it.timestampMs <= currentPositionMs }
                                if (idx >= 0) idx else 0
                            }

                            LaunchedEffect(activeIndex) {
                                if (lyricsResult.isSynced && activeIndex in lines.indices) {
                                    listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(lines, key = { idx, l -> "${idx}_${l.timestampMs}" }) { idx, line ->
                                    val isActive = lyricsResult.isSynced && (idx == activeIndex)
                                    val bgAnimated by animateColorAsState(
                                        targetValue = if (isActive) NeonCyan.copy(alpha = 0.18f) else Color.Transparent,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bgAnimated)
                                            .clickable {
                                                if (lyricsResult.isSynced) {
                                                    onSeekTo(line.timestampMs)
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = line.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                                color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.8f),
                                                fontSize = if (isActive) 16.sp else 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        )

                                        if (showPersianTranslation && !line.translatedText.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = line.translatedText,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Light,
                                                    color = if (isActive) NeonAmber else Color(0xFF94A3B8),
                                                    fontSize = if (isActive) 13.sp else 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
