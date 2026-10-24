package com.example.engine

import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import kotlin.math.pow

enum class PlaybackMode(
    @androidx.annotation.StringRes val stringRes: Int,
    val labelEn: String,
    val labelFa: String,
    val iconName: String
) {
    SMART_SHUFFLE(com.example.R.string.mode_smart_shuffle, "Smart Weighted Shuffle", "شافل هوشمند (وزن‌دار)", "AutoAwesome"),
    RANKED_FIRST(com.example.R.string.mode_ranked_first, "Top Ranked First", "برترین‌ها به ترتیب رتبه", "TrendingUp"),
    DISCOVERY(com.example.R.string.mode_discovery, "Discovery (Unplayed First)", "اکتشاف آهنگ‌های نشنیده", "Explore"),
    SEQUENTIAL(com.example.R.string.mode_sequential, "Sequential", "پخش عادی ترتیبی", "FormatListNumbered"),
    REPEAT_ONE(com.example.R.string.mode_repeat_one, "Repeat One", "تکرار همین آهنگ", "RepeatOne");

    fun localizedLabel(isFa: Boolean = java.util.Locale.getDefault().language == "fa"): String =
        if (isFa) labelFa else labelEn
}

/**
 * Continuous fuzzy ranking (v2).
 *
 * All songs start at score = 0.0. Each play session adds a continuous Delta
 * based on how much of the track was listened to (X in [0..1]):
 *
 *   ZONE 1 — SEARCH     (X < 0.10):  Delta = -0.1 * X * 100
 *   ZONE 2 — AVERSION   (0.10..0.5): Delta = -0.5 * (1 - X)^2 * 100
 *   ZONE 3 — SATISFACTION(X >= 0.5): Delta = +1.0 * (X - 0.5) * 100
 *
 * Total = old_score + Delta, clamped to [-100, +100].
 */
object SmartRankingEngine {

    const val SEARCH_WINDOW = 0.10f
    const val SATISFACTION_THRESHOLD = 0.50f

    const val C_SEARCH = 0.1f
    const val C_AVERSION = 0.5f
    const val C_REWARD = 1.0f
    const val SCALE = 100f

    const val MIN_SCORE = -100.0f
    const val MAX_SCORE = 100.0f

    const val EARLY_SKIP_MS_THRESHOLD = 20_000L
    const val RECENT_PLAY_PROTECTION_THRESHOLD_MS = 8 * 60 * 1000L

    data class DeltaResult(val delta: Float, val zone: String)

    fun computeDelta(x: Float): DeltaResult {
        val c = x.coerceIn(0f, 1f)
        val (delta, zone) = when {
            c < SEARCH_WINDOW ->
                -C_SEARCH * c * SCALE to "SEARCH"
            c < SATISFACTION_THRESHOLD ->
                -C_AVERSION * (1f - c).pow(2) * SCALE to "AVERSION"
            else ->
                C_REWARD * (c - SATISFACTION_THRESHOLD) * SCALE to "SATISFACTION"
        }
        return DeltaResult(delta, zone)
    }

    fun evaluateSession(
        song: Song,
        listenedMs: Long,
        totalMs: Long,
        isManualSkip: Boolean = false,
        isConsecutiveReplay: Boolean = false
    ): Pair<Song, SessionEvaluation> {
        val safeTotal = if (totalMs > 0) totalMs else song.durationMs.coerceAtLeast(1000L)
        val x = (listenedMs.toFloat() / safeTotal).coerceIn(0f, 1f)
        val oldScore = song.score
        val wasUnplayed = song.isUnplayed

        val result = computeDelta(x)
        val newScore = (oldScore + result.delta).coerceIn(MIN_SCORE, MAX_SCORE)
        val pct = (x * 100).toInt()
        val deltaPts = result.delta.toInt()

        val reasonFa: String
        val reasonEn: String
        when (result.zone) {
            "SEARCH" -> {
                reasonFa = "جستجو ($pct%): احتمالاً دنبال آهنگ دیگری بودید. $deltaPts امتیاز."
                reasonEn = "Search ($pct%): likely seeking something else. $deltaPts pts."
            }
            "AVERSION" -> {
                reasonFa = "رد سریع ($pct%): هرچه زودتر، جریمه بیشتر. $deltaPts امتیاز."
                reasonEn = "Aversion ($pct%): earlier skip = heavier penalty. $deltaPts pts."
            }
            else -> {
                reasonFa = if (isConsecutiveReplay)
                    "پخش کامل + تکرار ($pct%): +$deltaPts پاداش ویژه!"
                else
                    "رضایت ($pct%): +$deltaPts امتیاز."
                reasonEn = if (isConsecutiveReplay)
                    "Full listen + replay ($pct%): +$deltaPts bonus!"
                else
                    "Satisfaction ($pct%): +$deltaPts pts."
            }
        }

        val updatedSong = song.copy(
            isUnplayed = false,
            score = newScore,
            playCount = song.playCount + if (x >= 0.75f) 1 else 0,
            skipCount = song.skipCount + if (isManualSkip && x < 0.20f) 1 else 0,
            lastListenRatio = x,
            lastPlayedTimestamp = System.currentTimeMillis()
        )

        val evaluation = SessionEvaluation(
            songId = song.id,
            songTitle = song.title,
            oldScore = oldScore,
            newScore = newScore,
            scoreDelta = result.delta,
            reasonFa = reasonFa,
            reasonEn = reasonEn,
            isPositive = result.delta >= 0f,
            listenedRatio = x,
            wasUnplayed = wasUnplayed
        )

        return Pair(updatedSong, evaluation)
    }

    fun selectNextSong(
        playlist: List<Song>,
        currentSongId: Long?,
        mode: PlaybackMode
    ): Song? {
        if (playlist.isEmpty()) return null
        if (playlist.size == 1) return playlist.first()

        return when (mode) {
            PlaybackMode.REPEAT_ONE ->
                playlist.find { it.id == currentSongId } ?: playlist.first()
            PlaybackMode.SEQUENTIAL -> {
                val i = playlist.indexOfFirst { it.id == currentSongId }
                if (i >= 0 && i + 1 < playlist.size) playlist[i + 1] else playlist.first()
            }
            PlaybackMode.RANKED_FIRST -> {
                val sorted = playlist.sortedByDescending { it.score }
                sorted.filter { it.id != currentSongId }.firstOrNull() ?: sorted.first()
            }
            PlaybackMode.DISCOVERY -> {
                val unplayed = playlist.filter { it.isUnplayed && it.id != currentSongId }
                if (unplayed.isNotEmpty()) unplayed.random()
                else selectWeightedRandom(playlist, currentSongId)
            }
            PlaybackMode.SMART_SHUFFLE -> selectWeightedRandom(playlist, currentSongId)
        }
    }

    private fun selectWeightedRandom(playlist: List<Song>, currentSongId: Long?): Song {
        val candidates = playlist.filter { it.id != currentSongId }
        val pool = if (candidates.isNotEmpty()) candidates else playlist
        val weights = pool.map { s ->
            val shifted = ((s.score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE)).coerceIn(0.05f, 1f)
            (shifted.toDouble() + 0.5).pow(2.0)
        }
        val total = weights.sum()
        if (total <= 0.0) return pool.random()
        val r = kotlin.random.Random.nextDouble(0.0, total)
        var acc = 0.0
        for (i in pool.indices) { acc += weights[i]; if (acc >= r) return pool[i] }
        return pool.last()
    }
}
