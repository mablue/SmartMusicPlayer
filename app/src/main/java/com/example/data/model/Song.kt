package com.example.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.R
import java.util.Locale

enum class RankTier(
    @StringRes val labelRes: Int,
    val labelEn: String,
    val labelFa: String,
    val colorHex: String
) {
    DIAMOND(R.string.tier_diamond, "Diamond Favorite", "الماس / محبوب‌ترین", "#38BDF8"),
    GOLD(R.string.tier_gold, "Gold Loved", "عالی و طلایی", "#F59E0B"),
    UNPLAYED(R.string.tier_unplayed, "Unplayed Discovery", "نشنیده / شانس اولیه", "#A78BFA"),
    SILVER(R.string.tier_silver, "Neutral", "معمولی / خنثی", "#9CA3AF"),
    DISLIKED(R.string.tier_disliked, "Frequently Skipped", "اسکیپ شده / کم‌علاقه", "#F43F5E");

    fun localizedLabel(isFa: Boolean = Locale.getDefault().language == "fa"): String =
        if (isFa) labelFa else labelEn
}

@Immutable
data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "Album",
    val durationMs: Long = 180000L,
    val uri: String = "",
    val isUnplayed: Boolean = true,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val score: Float = 50.0f,
    val lastListenRatio: Float = 0.0f,
    val lastPlayedTimestamp: Long = 0L,
    val colorHex: String = "#8B5CF6",
    val genre: String = "Music"
) {
    val rankTier: RankTier
        get() = when {
            isUnplayed -> RankTier.UNPLAYED
            score >= 80.0f -> RankTier.DIAMOND
            score >= 65.0f -> RankTier.GOLD
            score >= 45.0f -> RankTier.SILVER
            else -> RankTier.DISLIKED
        }

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

data class SessionEvaluation(
    val songId: Long,
    val songTitle: String,
    val oldScore: Float,
    val newScore: Float,
    val scoreDelta: Float,
    val reasonFa: String,
    val reasonEn: String,
    val isPositive: Boolean,
    val listenedRatio: Float,
    val wasUnplayed: Boolean
) {
    fun localizedReason(isFa: Boolean = Locale.getDefault().language == "fa"): String =
        if (isFa) reasonFa else reasonEn
}
