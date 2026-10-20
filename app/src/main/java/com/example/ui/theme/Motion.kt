package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens for SmartMusicPlayer, aligned with M3 Motion System.
 *
 * All animations in the app should reference these tokens rather than
 * hardcoding duration/easing values. This ensures consistent feel and
 * makes it easy to tune globally.
 *
 * Reference: https://m3.material.io/styles/motion/overview
 */
object Motion {
    // ===== Duration tokens (ms) =====
    const val DURATION_INSTANT = 0
    const val DURATION_FAST = 150
    const val DURATION_MEDIUM_1 = 200
    const val DURATION_MEDIUM_2 = 250
    const val DURATION_LONG_1 = 300
    const val DURATION_LONG_2 = 400
    const val DURATION_EXTRA_LONG = 500

    // ===== Easing tokens =====
    /** Enter screen — decelerate to stop */
    val EasingEnter: Easing = FastOutSlowInEasing
    /** Exit screen — accelerate away */
    val EasingExit: Easing = FastOutLinearInEasing
    /** Content change in place */
    val EasingStandard: Easing = LinearOutSlowInEasing
    /** Playful, expressive — for small delight moments */
    val EasingExpressive: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // ===== Composable-friendly shortcuts =====
    /** Fast fade-in/out for chips, badges (150ms) */
    fun fastFade() = tween<Float>(
        durationMillis = DURATION_FAST,
        easing = EasingStandard
    )

    /** Standard screen transition (250ms) */
    fun standardEnter() = tween<Float>(
        durationMillis = DURATION_MEDIUM_2,
        easing = EasingEnter
    )

    /** Standard exit (200ms) */
    fun standardExit() = tween<Float>(
        durationMillis = DURATION_MEDIUM_1,
        easing = EasingExit
    )

    /** Springy entrance for MiniPlayer / dialogs (M3 Expressive) */
    fun <T> bouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Smooth spring for progress indicators */
    fun <T> smooth() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}
