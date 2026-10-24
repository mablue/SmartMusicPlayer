package com.example.engine

import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

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
 * Smart ranking (v3 — smooth continuous).
 * Three zones with NO discontinuities, plus time decay and reward saturation.
 */
object SmartRankingEngine {

    private const val PEAK_NEGATIVE = -15f
    private const val PEAK_POSITIVE = 50f
    private const val AVERSION_PEAK_X = 0.30f
    private const val INDIFFERENT_X = 0.50f
    private const val DECAY_HALF_LIFE_DAYS = 1095.0
    private const val SATURATION_PER_PLAY = 0.15f

    const val MIN_SCORE = -100.0f
    const val MAX_SCORE = 100.0f

    const val EARLY_SKIP_MS_THRESHOLD = 20_000L
    const val RECENT_PLAY_PROTECTION_THRESHOLD_MS = 8 * 60 * 1000L

    data class DeltaResult(val delta: Float, val zone: String)

    fun computeDelta(x: Float): DeltaResult {
        val c = x.coerceIn(0f, 1f)
        val delta = when {
            c <= AVERSION_PEAK_X -> {
                val t = c / AVERSION_PEAK_X
                PEAK_NEGATIVE * (3f * t * t - 2f * t * t * t)
            }
            c <= INDIFFERENT_X -> {
                val s = (c - AVERSION_PEAK_X) / (INDIFFERENT_X - AVERSION_PEAK_X)
                PEAK_NEGATIVE * (1f - 3f * s * s + 2f * s * s * s)
            }
            else -> {
                val s = (c - INDIFFERENT_X) / (1f - INDIFFERENT_X)
                PEAK_POSITIVE * (3f * s * s - 2f * s * s * s)
            }
        }
        val zone = when {
            c < 0.10f -> "SEARCH"
            c < INDIFFERENT_X -> "AVERSION"
            else -> "SATISFACTION"
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

        val deltaResult = computeDelta(x)
        val rawDelta = deltaResult.delta
        val zone = deltaResult.zone

        val now = System.currentTimeMillis()
        val daysSince = if (song.lastPlayedTimestamp > 0 && !wasUnplayed) {
            ((now - song.lastPlayedTimestamp) / (24L * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        } else 0
        val decayFactor = if (daysSince > 0) exp(-daysSince / DECAY_HALF_LIFE_DAYS).toFloat() else 1f
        val decayedScore = oldScore * decayFactor

        val effectiveDelta = if (rawDelta > 0f) {
            rawDelta / sqrt(1f + song.playCount * SATURATION_PER_PLAY)
        } else rawDelta

        val newScore = (decayedScore + effectiveDelta).coerceIn(MIN_SCORE, MAX_SCORE)

        val pct = (x * 100).toInt()
        val deltaPts = effectiveDelta.toInt()
        val reasonFa: String
        val reasonEn: String
        when (zone) {
            "SEARCH" -> {
                reasonFa = "جستجو ($pct%): احتمالاً دنبال آهنگ دیگری بودید. ${fmtSigned(deltaPts)} امتیاز."
                reasonEn = "Search ($pct%): likely seeking something else. ${fmtSigned(deltaPts)} pts."
            }
            "AVERSION" -> {
                reasonFa = "رد سریع ($pct%): هرچه زودتر، جریمه بیشتر. ${fmtSigned(deltaPts)} امتیاز."
                reasonEn = "Aversion ($pct%): earlier skip = heavier penalty. ${fmtSigned(deltaPts)} pts."
            }
            else -> {
                reasonFa = if (isConsecutiveReplay)
                    "پخش کامل + تکرار ($pct%): ${fmtSigned(deltaPts)} پاداش ویژه!"
                else
                    "رضایت ($pct%): ${fmtSigned(deltaPts)} امتیاز."
                reasonEn = if (isConsecutiveReplay)
                    "Full listen + replay ($pct%): ${fmtSigned(deltaPts)} bonus!"
                else
                    "Satisfaction ($pct%): ${fmtSigned(deltaPts)} pts."
            }
        }

        val updatedSong = song.copy(
            isUnplayed = false,
            score = newScore,
            playCount = song.playCount + if (x >= 0.75f) 1 else 0,
            skipCount = song.skipCount + if (isManualSkip && x < 0.20f) 1 else 0,
            lastListenRatio = x,
            lastPlayedTimestamp = now
        )

        val evaluation = SessionEvaluation(
            songId = song.id,
            songTitle = song.title,
            oldScore = oldScore,
            newScore = newScore,
            scoreDelta = effectiveDelta,
            reasonFa = reasonFa,
            reasonEn = reasonEn,
            isPositive = effectiveDelta >= 0f,
            listenedRatio = x,
            wasUnplayed = wasUnplayed
        )

        return Pair(updatedSong, evaluation)
    }

    private fun fmtSigned(v: Int): String = if (v >= 0) "+$v" else "$v"

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
        val weights = pool.map { computeWeight(it) }
        val total = weights.sum()
        if (total <= 0.0) return pool.random()
        val r = kotlin.random.Random.nextDouble(0.0, total)
        var acc = 0.0
        for (i in pool.indices) {
            acc += weights[i]
            if (acc >= r) return pool[i]
        }
        return pool.last()
    }

    private fun computeWeight(s: Song): Double {
        if (s.isUnplayed) return 0.30
        val normalized = ((s.score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE))
            .coerceIn(0.001f, 1f).toDouble()
        return normalized.pow(3.0)
    }
}
