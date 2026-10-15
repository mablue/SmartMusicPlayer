package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet

@Composable
fun VisualizerWaves(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    width: Dp = 140.dp,
    height: Dp = 36.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.width(width).height(height)) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = totalWidth / (barCount * 1.5f)
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            colors = listOf(NeonCyan, NeonViolet),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            val factor = if (isPlaying) {
                val wave = kotlin.math.sin(phase + i * 0.45).toFloat()
                val normalized = ((wave + 1f) / 2f).coerceIn(0.15f, 1f)
                normalized
            } else {
                0.15f
            }

            val barH = canvasHeight * factor
            val x = i * (barWidth + spacing)
            val y = canvasHeight - barH

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
