package com.example.engine

import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import kotlin.math.pow
import kotlin.random.Random

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

object SmartRankingEngine {

    const val DEFAULT_UNPLAYED_SCORE = 50.0f
    const val MIN_SCORE = 5.0f
    const val MAX_SCORE = 100.0f

    // Early skip threshold: less than 20 seconds or less than 20% of duration
    const val EARLY_SKIP_MS_THRESHOLD = 20_000L
    const val EARLY_SKIP_RATIO_THRESHOLD = 0.20f
    const val FULL_LISTEN_RATIO_THRESHOLD = 0.75f

    // Listening fatigue protection: if a song was played within the last 8 minutes,
    // skipping it is treated as natural session fatigue/navigation, NOT dislike!
    const val RECENT_PLAY_PROTECTION_THRESHOLD_MS = 8 * 60 * 1000L

    /**
     * Evaluates user listening session and updates the song's score and statistics.
     */
    fun evaluateSession(
        song: Song,
        listenedMs: Long,
        totalMs: Long,
        isManualSkip: Boolean = false,
        isConsecutiveReplay: Boolean = false
    ): Pair<Song, SessionEvaluation> {
        val safeTotal = if (totalMs > 0) totalMs else song.durationMs.coerceAtLeast(1000L)
        val ratio = (listenedMs.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
        val oldScore = song.score
        val wasUnplayed = song.isUnplayed

        val isEarlySkip = isManualSkip && (listenedMs < EARLY_SKIP_MS_THRESHOLD || ratio < EARLY_SKIP_RATIO_THRESHOLD)
        val isFullListen = ratio >= FULL_LISTEN_RATIO_THRESHOLD || (!isManualSkip && ratio >= 0.70f)

        // Check if track was played very recently
        val isRecentlyPlayed = !wasUnplayed && song.lastPlayedTimestamp > 0 &&
                (System.currentTimeMillis() - song.lastPlayedTimestamp < RECENT_PLAY_PROTECTION_THRESHOLD_MS)

        var newScore: Float
        var newPlayCount = song.playCount
        var newSkipCount = song.skipCount
        val reasonFa: String
        val reasonEn: String
        val isPositive: Boolean

        if (wasUnplayed) {
            // First time this song is ever played: RESET arbitrary 50.0 placeholder to real behavior!
            when {
                isEarlySkip -> {
                    newScore = 20.0f
                    newSkipCount++
                    reasonFa = "اولین پخش: اسکیپ زودهنگام! امتیاز اولیه ۵۰ ریست شد و به عنوان عدم‌علاقه (۲۰) ثبت شد."
                    reasonEn = "First listen: Early skip! Initial 50 pts reset to 20 (Disliked)."
                    isPositive = false
                }
                isFullListen -> {
                    newScore = 80.0f
                    newPlayCount++
                    reasonFa = "اولین پخش: گوش دادن کامل! امتیاز اولیه ۵۰ ریست شد و رتبه طلایی (۸۰) ثبت گردید."
                    reasonEn = "First listen: Full play! Initial 50 pts reset to 80 (Gold affinity)."
                    isPositive = true
                }
                else -> {
                    newScore = 55.0f
                    reasonFa = "اولین پخش: گوش دادن متوسط (${(ratio * 100).toInt()}%). امتیاز اولیه ریست و ۵۵ ثبت شد."
                    reasonEn = "First listen: Moderate play. Score reset and set to 55."
                    isPositive = true
                }
            }
        } else {
            // Song was already played in the past, adjust existing score incrementally
            when {
                isEarlySkip && isRecentlyPlayed -> {
                    // Protected by recency: User recently heard it, skipping is fatigue/navigation, not dislike!
                    newScore = oldScore
                    reasonFa = "رد کردن آهنگ تازه شنیده‌شده: کسر امتیاز اعمال نشد (محافظت در برابر خستگی گوش دادن)."
                    reasonEn = "Skipped recently played track: No penalty applied (fatigue protection)."
                    isPositive = true
                }
                isEarlySkip -> {
                    val penalty = 20.0f
                    newScore = (oldScore - penalty).coerceAtLeast(MIN_SCORE)
                    newSkipCount++
                    reasonFa = "اسکیپ زودهنگام: کسر ۲۰ امتیاز به دلیل رد سریع آهنگ."
                    reasonEn = "Early skip: -20 pts penalty."
                    isPositive = false
                }
                isFullListen -> {
                    val bonus = if (isConsecutiveReplay) 28.0f else 20.0f
                    newScore = (oldScore + bonus).coerceAtMost(MAX_SCORE)
                    newPlayCount++
                    reasonFa = if (isConsecutiveReplay) {
                        "پخش کامل + تکرار مجدد: ۲۸+ امتیاز پاداش ویژه!"
                    } else {
                        "پخش کامل آهنگ: ۲۰+ امتیاز مثبت و افزایش رتبه."
                    }
                    reasonEn = if (isConsecutiveReplay) "Full listen + replay: +28 pts bonus!" else "Full play: +20 pts boost!"
                    isPositive = true
                }
                else -> {
                    val bonus = 5.0f
                    newScore = (oldScore + bonus).coerceAtMost(MAX_SCORE)
                    reasonFa = "گوش دادن رضایت‌بخش (${(ratio * 100).toInt()}%): ۵+ امتیاز افزایشی."
                    reasonEn = "Satisfactory listen: +5 pts boost."
                    isPositive = true
                }
            }
        }

        val updatedSong = song.copy(
            isUnplayed = false,
            score = newScore,
            playCount = newPlayCount,
            skipCount = newSkipCount,
            lastListenRatio = ratio,
            lastPlayedTimestamp = System.currentTimeMillis()
        )

        val evaluation = SessionEvaluation(
            songId = song.id,
            songTitle = song.title,
            oldScore = oldScore,
            newScore = newScore,
            scoreDelta = newScore - oldScore,
            reasonFa = reasonFa,
            reasonEn = reasonEn,
            isPositive = isPositive,
            listenedRatio = ratio,
            wasUnplayed = wasUnplayed
        )

        return Pair(updatedSong, evaluation)
    }

    /**
     * Selects the next song based on the chosen playback mode.
     */
    fun selectNextSong(
        playlist: List<Song>,
        currentSongId: Long?,
        mode: PlaybackMode
    ): Song? {
        if (playlist.isEmpty()) return null
        if (playlist.size == 1) return playlist.first()

        return when (mode) {
            PlaybackMode.REPEAT_ONE -> {
                playlist.find { it.id == currentSongId } ?: playlist.first()
            }
            PlaybackMode.SEQUENTIAL -> {
                val currentIndex = playlist.indexOfFirst { it.id == currentSongId }
                if (currentIndex >= 0 && currentIndex + 1 < playlist.size) {
                    playlist[currentIndex + 1]
                } else {
                    playlist.first()
                }
            }
            PlaybackMode.RANKED_FIRST -> {
                val sorted = playlist.sortedByDescending { it.score }
                val nextCandidates = sorted.filter { it.id != currentSongId }
                nextCandidates.firstOrNull() ?: sorted.first()
            }
            PlaybackMode.DISCOVERY -> {
                // Focus on unplayed tracks first
                val unplayed = playlist.filter { it.isUnplayed && it.id != currentSongId }
                if (unplayed.isNotEmpty()) {
                    unplayed.random()
                } else {
                    // Fallback to weighted shuffle if all have been listened to
                    selectWeightedRandom(playlist, currentSongId)
                }
            }
            PlaybackMode.SMART_SHUFFLE -> {
                selectWeightedRandom(playlist, currentSongId)
            }
        }
    }

    /**
     * Weighted Random Selection:
     * Higher score = higher probability of selection.
     * Uses exponent 1.3 to create smooth natural affinity bias.
     */
    private fun selectWeightedRandom(playlist: List<Song>, currentSongId: Long?): Song {
        val candidates = playlist.filter { it.id != currentSongId }
        val pool = if (candidates.isNotEmpty()) candidates else playlist

        // Compute weights with power curve to amplify favorites without starving exploration
        val weights = pool.map { song ->
            val base = song.score.toDouble().coerceAtLeast(MIN_SCORE.toDouble())
            base.pow(1.3)
        }

        val totalWeight = weights.sum()
        if (totalWeight <= 0.0) return pool.random()

        val randomThreshold = Random.nextDouble(0.0, totalWeight)
        var cumulative = 0.0

        for (i in pool.indices) {
            cumulative += weights[i]
            if (cumulative >= randomThreshold) {
                return pool[i]
            }
        }

        return pool.last()
    }
}
