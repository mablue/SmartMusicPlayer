package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
 * 全屏歌词面板 — Apple Music 风格
 *
 * - 活跃行居中、放大、全亮
 * - 相邻行透明度 0.55，隔两行 0.35，再远加模糊
 * - 翻译行在主行下方，0.6× 字号
 * - 手动滚动后 5 秒恢复自动滚动
 * - 顶部工具栏：自动搜索 / 自动翻译开关 + 关闭
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
    onDismiss: () -> Unit
) {
    // 手动滚动 → 暂停自动滚动 5 秒
    var userScrollCooldown by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val lines = lyricsResult?.lines ?: emptyList()
    val activeIndex = remember(currentPositionMs, lines) {
        val idx = lines.indexOfLast { it.timestampMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    // 自动滚动（尊重手动滚动冷却期）
    LaunchedEffect(activeIndex, userScrollCooldown) {
        if (userScrollCooldown <= 0 && lyricsResult?.isSynced == true && activeIndex in lines.indices) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    // 冷却倒计时
    LaunchedEffect(userScrollCooldown) {
        if (userScrollCooldown > 0) {
            delay(1000L)
            userScrollCooldown -= 1
        }
    }

    // 监听手动滚动
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) userScrollCooldown = 5
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            // ─── 顶部工具栏 ───
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
                    Icon(Icons.Default.Close, contentDescription = "关闭歌词")
                }

                Text(
                    text = if (isTranslating) "翻译中…" else "歌词",
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
                        desc = "自动搜索歌词",
                        onClick = onToggleAutoSearch
                    )
                    LyricsToggleIcon(
                        icon = Icons.Default.Translate,
                        active = autoTranslateLyrics,
                        tint = NeonViolet,
                        desc = "自动翻译",
                        onClick = onToggleAutoTranslate
                    )
                }
            }

            // ─── 歌曲信息 ───
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

            // ─── 歌词主体 ───
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
                                text = if (autoSearchLyrics) "正在搜索歌词…" else "未找到歌词",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.7f))
                            )
                            if (autoSearchLyrics) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = NeonCyan.copy(alpha = 0.2f),
                                    onClick = onRetryFetch
                                ) {
                                    Text(
                                        text = "重新搜索",
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
                            vertical = screenHeight * 0.25f
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
                                onClick = {
                                    if (lyricsResult.isSynced) onSeekTo(line.timestampMs)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// 单行歌词 — 核心视觉逻辑
// ─────────────────────────────────────────────────────────
@Composable
private fun LyricRow(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    onClick: () -> Unit
) {
    // 目标状态基于距离
    val targetAlpha = when (distance) {
        0 -> 1.0f
        1 -> 0.55f
        2 -> 0.35f
        else -> 0.18f
    }
    val targetScale = when (distance) {
        0 -> 1.0f
        1 -> 0.88f
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
        targetValue = if (isActive) Color.White else Color(0xFFCBD5E1),
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

        // 翻译行（如果开启且有内容）
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

// ─────────────────────────────────────────────────────────
// 顶部工具栏的 toggle 图标
// ─────────────────────────────────────────────────────────
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
