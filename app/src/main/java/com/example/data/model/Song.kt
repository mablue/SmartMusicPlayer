package com.example.data.model

enum class RankTier(val labelFa: String, val labelEn: String, val colorHex: String) {
    DIAMOND("الماس / محبوب‌ترین", "Diamond Favorite", "#38BDF8"),
    GOLD("عالی و طلایی", "Gold Loved", "#F59E0B"),
    UNPLAYED("نشنیده / شانس اولیه", "Unplayed Discovery", "#A78BFA"),
    SILVER("معمولی / خنثی", "Neutral", "#9CA3AF"),
    DISLIKED("اسکیپ شده / کم‌علاقه", "Frequently Skipped", "#F43F5E")
}

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "آلبوم هوشمند",
    val durationMs: Long = 180000L,
    val uri: String = "",
    val isUnplayed: Boolean = true,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val score: Float = 50.0f,
    val lastListenRatio: Float = 0.0f,
    val lastPlayedTimestamp: Long = 0L,
    val colorHex: String = "#8B5CF6",
    val genre: String = "الکترونیک"
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
)
