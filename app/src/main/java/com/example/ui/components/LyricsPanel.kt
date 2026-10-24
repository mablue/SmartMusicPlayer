package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lyrics.LyricLine
import com.example.data.lyrics.LyricsResult
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.delay

/**
 * Full-screen lyrics panel.
 *
 * Supports two modes:
 *  - Auto-scroll for synced lyrics (LRC with timestamps)
 *  - Manual sync for plain lyrics: user scrolls, center line is highlighted,
 *    "Sync" FAB appears, tapping it pins current position to that line.
 */
@Composable
fun LyricsPanel(
    songTitle: String,
    artist: String,
    lyricsResult: LyricsResult?,
    isLoading: Boolean,
    isTranslating: Boolean,
    currentPositionMs: Long,
    autoSearchLyrics: Boolean,
    autoTranslateLyrics: Boolean,
    onToggleAutoSearch: () -> Unit,
    onToggleAutoTranslate: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetryFetch: () -> Unit,
    onSyncLine: (index: Int, timestampMs: Long) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var userScrollCooldown by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val isDragging by listState.interactionSource.collectIsDraggedAsState()

    var manualScrollActive by remember { mutableStateOf(false) }
    var lastDragStopTime by remember { mutableStateOf(0L) }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            manualScrollActive = true
            lastDragStopTime = 0L
        } else if (manualScrollActive) {
            lastDragStopTime = System.currentTimeMillis()
            delay(10_000L)
            if (lastDragStopTime > 0L &&
                System.currentTimeMillis() - lastDragStopTime >= 9_500L
            ) {
                manualScrollActive = false
            }
        }
    }

    val lines = lyricsResult?.lines ?: emptyList()
    val activeIndex = remember(currentPositionMs, lines) {
        val idx = lines.indexOfLast { it.timestampMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(activeIndex, userScrollCooldown, isDragging) {
        if (!isDragging && userScrollCooldown <= 0 &&
            lyricsResult?.isSynced == true && activeIndex in lines.indices
        ) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LaunchedEffect(isDragging) {
        if (isDragging) userScrollCooldown = 5
    }

    LaunchedEffect(userScrollCooldown) {
        if (userScrollCooldown > 0) {
            delay(1000L)
            userScrollCooldown -= 1
        }
    }

    val centerItemIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf null
            val vCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - vCenter)
            }?.index
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0A0812),
                        Color(0xFF161324).copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "بستن")
                }

                Text(
                    text = if (isTranslating) "翻译中…" else "متن آهنگ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    LyricsToggleIcon(
                        icon = Icons.Default.Search,
                        active = autoSearchLyrics,
                        tint = NeonCyan,
                        desc = "جستجوی خودکار",
                        onClick = onToggleAutoSearch
                    )
                    LyricsToggleIcon(
                        icon = Icons.Default.Translate,
                        active = autoTranslateLyrics,
                        tint = NeonViolet,
                        desc = "ترجمه‌ی خودکار",
                        onClick = onToggleAutoTranslate
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.55f)
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                lyricsResult == null || lines.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (autoSearchLyrics) "در حال جستجو…" else "متنی یافت نشد",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            if (autoSearchLyrics) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = NeonCyan.copy(alpha = 0.2f),
                                    onClick = onRetryFetch
                                ) {
                                    Text(
                                        text = "تلاش مجدد",
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 28.dp,
                            vertical = screenHeight * 0.4f
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { idx, l -> "${idx}_${l.timestampMs}" }
                        ) { idx, line ->
                            val distance = kotlin.math.abs(idx - activeIndex)
                            LyricRow(
                                line = line,
                                isActive = idx == activeIndex,
                                distance = distance,
                                isCenter = (idx == centerItemIndex) && manualScrollActive,
                                onClick = {
                                    if (lyricsResult.isSynced) onSeekTo(line.timestampMs)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sync FAB — only for plain (unsynced) lyrics when the user has scrolled
        if (manualScrollActive &&
            lyricsResult != null &&
            !lyricsResult.isSynced &&
            lines.isNotEmpty()
        ) {
            FloatingActionButton(
                onClick = {
                    val ci = centerItemIndex
                    if (ci != null && ci in lines.indices) {
                        onSyncLine(ci, currentPositionMs)
                        manualScrollActive = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                containerColor = NeonCyan,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Sync this line")
            }
        }
    }
}

@Composable
private fun LyricRow(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    isCenter: Boolean = false,
    onClick: () -> Unit
) {
    val targetAlpha = when {
        isCenter -> 1.0f
        distance == 0 -> 1.0f
        distance == 1 -> 0.55f
        distance == 2 -> 0.35f
        else -> 0.18f
    }
    val targetScale = when {
        isCenter -> 1.06f
        distance == 0 -> 1.0f
        distance == 1 -> 0.88f
        else -> 0.80f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lyricAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lyricScale"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive || isCenter) Color.White else Color(0xFFCBD5E1),
        animationSpec = tween(300),
        label = "lyricColor"
    )

    val fontSize = if (isActive) 26.sp else 20.sp
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize.value * 1.35f).sp
            )
        )

        line.translatedText?.takeIf { it.isNotBlank() }?.let { translated ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = translated,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isActive) 15.sp else 13.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = if (isActive) NeonCyan.copy(alpha = 0.85f) else Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun LyricsToggleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    tint: Color,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) tint.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (active) tint else Color.White.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(20.dp)
        )
    }
}
