package com.example.data.lyrics

import com.example.data.model.Song
import com.example.util.FileLogger

/**
 * Generates a chain of cleaner + narrower search queries for LRCLIB.
 *
 * The library dump (855 tracks) showed titles like:
 *   "Dele Sheyda ~ Musics-Fa.Com"
 *   "Qocalmazd谋m [ CafehMusic.ir ]"
 *   "Lanie%20Gardner%20-%20Buzzkill%20(Lyric%20Video)"
 *   "Habib - Khaabe Sorkhe Booseha"  (with artist=<unknown>)
 *
 * Instead of one shot, we try up to 8 progressively simpler strategies.
 * First success wins and is logged so we can prune later.
 */
object LyricsQueryBuilder {

    data class Query(val artist: String, val title: String, val stage: String)

    private const val TAG = "LyricsQueryBuilder"

    fun shouldSkip(song: Song): Boolean {
        if (song.genre.equals("Podcast", ignoreCase = true) ||
            song.genre.equals("Audiobook", ignoreCase = true)) return true
        if (song.durationMs > 20 * 60 * 1000L) return true
        return false
    }

    fun generateStrategies(artist: String, title: String): List<Query> {
        val artistWasChannel = artist.trim().startsWith("@") ||
                artist.contains("t.me/", ignoreCase = true) ||
                artist.contains("Telegram", ignoreCase = true) ||
                artist.contains("کانال", ignoreCase = true)

        val cleanedArtist = clean(artist)
        val cleanedTitle = clean(title)

        if (cleanedTitle.length < 2) {
            FileLogger.info(TAG, "Title too short after clean: '$title' → '$cleanedTitle'")
            return emptyList()
        }

        val artistIsMeaningful = !artistWasChannel &&
                cleanedArtist.length >= 2 &&
                !cleanedArtist.equals("<unknown>", ignoreCase = true) &&
                !cleanedArtist.equals("unknown", ignoreCase = true) &&
                !cleanedArtist.equals("unknown artist", ignoreCase = true) &&
                !cleanedArtist.equals("audiolab", ignoreCase = true) &&
                !cleanedArtist.equals("mablue", ignoreCase = true)

        val strategies = mutableListOf<Query>()

        if (artistIsMeaningful) {
            strategies.add(Query(cleanedArtist, cleanedTitle, "S1"))
        }
        strategies.add(Query("", cleanedTitle, "S2"))

        val split = splitArtistTitle(title)
        if (split != null) {
            strategies.add(Query(split.first, split.second, "S3"))
        }

        val noBrackets = stripBrackets(cleanedTitle)
        if (noBrackets.length >= 2 && noBrackets != cleanedTitle) {
            strategies.add(Query(if (artistIsMeaningful) cleanedArtist else "", noBrackets, "S4"))
        }

        val words = cleanedTitle.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size >= 5) {
            val first5 = words.take(5).joinToString(" ")
            if (first5 != cleanedTitle) {
                strategies.add(Query(if (artistIsMeaningful) cleanedArtist else "", first5, "S5"))
            }
        }

        val result = strategies
            .filter { it.title.length >= 2 }
            .distinctBy { "${it.artist.lowercase()}|${it.title.lowercase()}" }

        FileLogger.info(TAG, "Strategies for '$artist' - '$title': ${result.map { it.stage }}")
        return result
    }

    fun clean(s: String): String {
        var r = s.trim()
        if (r.isEmpty()) return r

        if (r.contains("%")) {
            r = try { java.net.URLDecoder.decode(r, "UTF-8") } catch (e: Exception) { r }
        }

        r = r.replace("Ä±", "ı").replace("Ä°", "İ")
            .replace("Ã¼", "ü").replace("Ã§", "ç")
            .replace("ÅŸ", "ş").replace("ÄŸ", "ğ")
            .replace("Ã¶", "ö").replace("Ã—", "×")

        r = r.replace(Regex("""@[A-Za-z_]\w{2,}"""), " ")
            .replace(Regex("""[Tt]\.me/\S+"""), " ")
            .replace(Regex("""Telegram[:@]?\s*\S+""", RegexOption.IGNORE_CASE), " ")

        r = r.replace(
            Regex("""\S*\.(com|ir|net|me|in|org|io|co|tv|info|site|online|xyz|top)\b\S*""",
                RegexOption.IGNORE_CASE), " ")

        r = r.replace(Regex("""\[(Official|Lyric|M/V|MV|Audio|Video|Tiktok|New|HD|4K).*?\]""",
                RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\((Official|Lyric|M/V|MV|Audio|Video|Music Video|Remaster|Remastered).*?\)""",
                RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""_?\s*OFFICIAL\s+(TRACK|VIDEO|AUDIO)""", RegexOption.IGNORE_CASE), " ")

        r = r.replace(Regex("""^\d{1,3}[-_.\s]+"""), "")
        r = r.replace(Regex("""_?\d{6,}$"""), "")

        r = r.replace(Regex("""\s*[-–]\s*[Aa][Ii]\s*$"""), "")
            .replace(Regex("""\(AI\s+Tribute\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[Aa][Ii]\s+[Cc]over\s*"""), " ")

        r = r.replace(Regex("""\s*[-–]\s*\d{4}\s+[Rr]emaster(ed)?"""), "")

        r = r.replace(Regex("""کانال\s+\S+"""), " ")
            .replace(Regex("""سایت\s+\S+"""), " ")

        r = r.replace(Regex("""\s+"""), " ").trim()
        r = r.trim(',', '.', '-', '~', '[', ']', '{', '}', '(', ')', '«', '»', '"', '\'', '_', ' ')

        return r
    }

    private fun splitArtistTitle(title: String): Pair<String, String>? {
        val separators = listOf(" - ", " – ", " — ", " _ ")
        for (sep in separators) {
            val idx = title.indexOf(sep)
            if (idx > 0 && idx < title.length - sep.length) {
                val left = title.substring(0, idx).trim()
                val right = title.substring(idx + sep.length).trim()
                if (left.length >= 2 && right.length >= 2) {
                    return Pair(clean(left), clean(right))
                }
            }
        }
        return null
    }

    private fun stripBrackets(s: String): String {
        return s.replace(Regex("""\[[^\]]*\]|\{[^}]*\}|\([^)]*\)"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
