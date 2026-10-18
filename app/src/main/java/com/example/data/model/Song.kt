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
    val genre: String = "Music",
    val folderName: String = "Music",
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 320,
    val mimeType: String = "audio/mpeg",
    val detectedLanguage: String = "Auto",
    val lyrics: String = "",
    val syncedLyrics: String = "",
    val resumePositionMs: Long = 0L
) {
    val hasResumePosition: Boolean
        get() = resumePositionMs > 5000L && (durationMs <= 0 || resumePositionMs < (durationMs * 0.95f).toLong())

    val formattedResumePosition: String
        get() {
            val totalSeconds = (resumePositionMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

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

    val formattedFileSize: String
        get() {
            if (fileSizeBytes <= 0L) return "3.5 MB"
            val mb = fileSizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1.0) "%.1f MB".format(mb) else "%d KB".format(fileSizeBytes / 1024)
        }

    val formattedBitrate: String
        get() = if (bitrateKbps > 0) "$bitrateKbps kbps" else "320 kbps HQ"

    val isPodcastOrAudiobook: Boolean
        get() = genre.equals("Podcast", ignoreCase = true) ||
                genre.equals("Audiobook", ignoreCase = true) ||
                genre.equals("کتاب صوتی", ignoreCase = true) ||
                genre.equals("پادکست", ignoreCase = true) ||
                folderName.contains("podcast", ignoreCase = true) ||
                folderName.contains("پادکست", ignoreCase = true) ||
                folderName.contains("audiobook", ignoreCase = true) ||
                folderName.contains("کتاب", ignoreCase = true) ||
                folderName.contains("castbox", ignoreCase = true) ||
                folderName.contains("shenoto", ignoreCase = true) ||
                durationMs > 12 * 60 * 1000L ||
                title.contains("podcast", ignoreCase = true) ||
                title.contains("پادکست", ignoreCase = true) ||
                title.contains("episode", ignoreCase = true) ||
                title.contains("قسمت", ignoreCase = true) ||
                title.contains("کتاب صوتی", ignoreCase = true)
}

enum class SongSortOrder(val labelFa: String, val labelEn: String) {
    DEFAULT("امتیاز هوشمند", "Smart Rank"),
    DURATION_DESC("طولانی‌ترین", "Duration (Long)"),
    DURATION_ASC("کوتاه‌ترین", "Duration (Short)"),
    SIZE_DESC("حجم فایل (بیشترین)", "Size (Largest)"),
    SIZE_ASC("حجم فایل (کمترین)", "Size (Smallest)"),
    TITLE_ASC("عنوان (الفبا)", "Title (A–Z)"),
    ARTIST_ASC("خواننده (الفبا)", "Artist (A–Z)");

    fun localized(isFa: Boolean = Locale.getDefault().language == "fa"): String =
        if (isFa) labelFa else labelEn
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
