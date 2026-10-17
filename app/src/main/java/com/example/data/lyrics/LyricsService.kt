package com.example.data.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translatedText: String? = null
)

data class LyricsResult(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val source: String,
    val plainText: String = ""
)

object LyricsService {

    private const val TAG = "LyricsService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val LRC_PATTERN = Pattern.compile("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    /**
     * Fetches real synced/plain lyrics from open public lyrics registry (LRCLIB).
     * No proprietary keys required.
     */
    suspend fun fetchLyrics(artist: String, title: String, durationSec: Long = 0L): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = cleanSearchQuery(title)
            val cleanArtist = cleanSearchQuery(artist)

            // Step 1: Direct exact match
            val directUrl = StringBuilder("https://lrclib.net/api/get?")
                .append("artist_name=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
                .append("&track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
            if (durationSec > 0) {
                directUrl.append("&duration=").append(durationSec)
            }

            val directRequest = Request.Builder()
                .url(directUrl.toString())
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(directRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val json = JSONObject(body)
                        val syncedLyrics = json.optString("syncedLyrics")
                        val plainLyrics = json.optString("plainLyrics")

                        if (syncedLyrics.isNotEmpty()) {
                            val parsed = parseLrc(syncedLyrics)
                            if (parsed.isNotEmpty()) {
                                return@withContext LyricsResult(
                                    lines = parsed,
                                    isSynced = true,
                                    source = "LRCLIB Synced",
                                    plainText = plainLyrics.ifEmpty { syncedLyrics }
                                )
                            }
                        }

                        if (plainLyrics.isNotEmpty()) {
                            val lines = plainLyrics.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                                LyricLine(timestampMs = idx * 4000L, text = line.trim())
                            }
                            return@withContext LyricsResult(
                                lines = lines,
                                isSynced = false,
                                source = "LRCLIB Plain",
                                plainText = plainLyrics
                            )
                        }
                    }
                }
            }

            // Step 2: Fallback query search
            val searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() > 0) {
                            val first = jsonArray.getJSONObject(0)
                            val syncedLyrics = first.optString("syncedLyrics")
                            val plainLyrics = first.optString("plainLyrics")

                            if (syncedLyrics.isNotEmpty()) {
                                val parsed = parseLrc(syncedLyrics)
                                if (parsed.isNotEmpty()) {
                                    return@withContext LyricsResult(
                                        lines = parsed,
                                        isSynced = true,
                                        source = "LRCLIB Synced",
                                        plainText = plainLyrics.ifEmpty { syncedLyrics }
                                    )
                                }
                            }

                            if (plainLyrics.isNotEmpty()) {
                                val lines = plainLyrics.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                                    LyricLine(timestampMs = idx * 4000L, text = line.trim())
                                }
                                return@withContext LyricsResult(
                                    lines = lines,
                                    isSynced = false,
                                    source = "LRCLIB Plain",
                                    plainText = plainLyrics
                                )
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "Lyrics fetch error for $artist - $title: ${e.message}")
        }
        null
    }

    /**
     * Translates lyrics text using public translation endpoint.
     * Supports translating into Persian ('fa') or English ('en').
     */
    suspend fun translateText(text: String, targetLang: String = "fa"): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        try {
            val encoded = URLEncoder.encode(text.take(2000), "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val jsonArray = JSONArray(body)
                    val sentences = jsonArray.getJSONArray(0)
                    val result = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val part = sentences.getJSONArray(i).getString(0)
                        result.append(part)
                    }
                    return@withContext result.toString()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Translation error: ${e.message}")
        }
        null
    }

    /**
     * Parses standard LRC format strings with [mm:ss.xx] timestamp headers.
     */
    fun parseLrc(lrcText: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val lines = lrcText.lines()

        for (line in lines) {
            val matcher = LRC_PATTERN.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msPart = matcher.group(3) ?: "0"
                val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                val content = matcher.group(4)?.trim() ?: ""

                if (content.isNotEmpty()) {
                    result.add(LyricLine(timestampMs = totalMs, text = content))
                }
            }
        }

        return result.sortedBy { it.timestampMs }
    }

    private fun cleanSearchQuery(text: String): String {
        return text
            .replace(Regex("""\((feat|ft|remix|official|audio|video|lyrics).*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[.*?\]"""), "")
            .replace(".mp3", "", ignoreCase = true)
            .replace(".wav", "", ignoreCase = true)
            .replace(".flac", "", ignoreCase = true)
            .replace(".m4a", "", ignoreCase = true)
            .trim()
    }
}
