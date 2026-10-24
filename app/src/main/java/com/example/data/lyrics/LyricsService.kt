package com.example.data.lyrics

import android.util.Log
import com.example.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
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
     * Multi-strategy lyrics fetch. Tries up to ~8 progressively-simpler
     * queries against LRCLIB before falling back to lyrics.ovh and Lyrica.
     * First successful provider returns immediately.
     */
    suspend fun fetchLyrics(
        artist: String,
        title: String,
        durationSec: Long = 0L
    ): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val strategies = LyricsQueryBuilder.generateStrategies(artist, title)
            if (strategies.isEmpty()) {
                FileLogger.info(TAG, "No strategies for '$artist' - '$title' (probably cleaned to empty)")
                return@withContext null
            }

            FileLogger.info(TAG, "fetchLyrics: $artist - $title → ${strategies.size} strategies")

            for ((idx, q) in strategies.withIndex()) {
                if (!isActive) {
                    FileLogger.info(TAG, "Cancelled at idx=$idx")
                    return@withContext null
                }

                if (q.artist.isNotBlank()) {
                    val fromGet = tryLrclibGet(q.artist, q.title, durationSec)
                    if (fromGet != null) {
                        FileLogger.info(TAG, "WON ${q.stage}(get) idx=$idx: '${q.artist}' - '${q.title}'")
                        return@withContext fromGet
                    }
                }

                val fromSearch = tryLrclibSearch(q.artist, q.title)
                if (fromSearch != null) {
                    FileLogger.info(TAG, "WON ${q.stage}(search) idx=$idx: '${q.artist}' - '${q.title}'")
                    return@withContext fromSearch
                }

                FileLogger.info(TAG, "✗ ${q.stage} idx=$idx: '${q.artist}' - '${q.title}'")
            }

            val s1 = strategies.firstOrNull()

            if (s1 != null && s1.artist.isNotBlank()) {
                val ovh = tryLyricsOvh(s1.artist, s1.title, durationSec)
                if (ovh != null) {
                    FileLogger.info(TAG, "WON ovh: '${s1.artist}' - '${s1.title}'")
                    return@withContext ovh
                }
            }

            if (s1 != null) {
                val lyrica = fetchLyricsFromLyrica(s1.artist, s1.title)
                if (lyrica != null) {
                    FileLogger.info(TAG, "WON lyrica: '${s1.artist}' - '${s1.title}'")
                    return@withContext lyrica
                }
            }

            FileLogger.info(TAG, "ALL FAILED: '$artist' - '$title'")
            null
        } catch (e: Exception) {
            FileLogger.error(TAG, "fetchLyrics error: ${e.message}", e)
            null
        }
    }

    private suspend fun tryLrclibGet(
        artist: String,
        title: String,
        durationSec: Long
    ): LyricsResult? {
        return try {
            val url = StringBuilder("https://lrclib.net/api/get?")
                .append("artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
                .append("&track_name=").append(URLEncoder.encode(title, "UTF-8"))
            if (durationSec > 0) url.append("&duration=").append(durationSec)

            val req = Request.Builder()
                .url(url.toString())
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                if (body.isBlank()) return null
                val json = JSONObject(body)
                parseLrclibResponse(json)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryLrclibSearch(artist: String, title: String): LyricsResult? {
        return try {
            val q = if (artist.isNotBlank()) "$artist $title" else title
            val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(q, "UTF-8")
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                if (body.isBlank()) return null
                val arr = JSONArray(body)
                if (arr.length() == 0) return null
                parseLrclibResponse(arr.getJSONObject(0))
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrclibResponse(json: JSONObject): LyricsResult? {
        val synced = json.optString("syncedLyrics")
        val plain = json.optString("plainLyrics")

        if (synced.isNotEmpty()) {
            val parsed = parseLrc(synced)
            if (parsed.isNotEmpty()) {
                return LyricsResult(
                    lines = parsed,
                    isSynced = true,
                    source = "LRCLIB Synced",
                    plainText = plain.ifEmpty { synced }
                )
            }
        }
        if (plain.isNotEmpty()) {
            val lines = plain.lines().filter { it.isNotBlank() }.mapIndexed { i, l ->
                LyricLine(timestampMs = i * 4000L, text = l.trim())
            }
            return LyricsResult(
                lines = lines,
                isSynced = false,
                source = "LRCLIB Plain",
                plainText = plain
            )
        }
        return null
    }

    private suspend fun tryLyricsOvh(
        artist: String,
        title: String,
        durationSec: Long
    ): LyricsResult? {
        return try {
            val url = "https://api.lyrics.ovh/v1/" +
                    URLEncoder.encode(artist, "UTF-8") + "/" +
                    URLEncoder.encode(title, "UTF-8")
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val lyricsStr = json.optString("lyrics")
                if (lyricsStr.isNotBlank()) {
                    return fitPlainLyricsToSongDuration(
                        lyricsStr, durationSec * 1000L, "Lyrics.ovh"
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchLyricsFromLyrica(artist: String, title: String): LyricsResult? {
        return try {
            val url = "https://wilooper-lyrica.hf.space/lyrics/?artist=" +
                    URLEncoder.encode(artist, "UTF-8") +
                    "&song=" + URLEncoder.encode(title, "UTF-8") +
                    "&timestamps=true"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SmartMusic-Android/2.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                if (body.isBlank()) return null

                val json = JSONObject(body)
                if (json.optString("status") != "success") return null

                val data = json.optJSONObject("data") ?: return null
                val lyricsText = data.optString("lyrics", "")
                if (lyricsText.isBlank()) return null
                val sourceName = data.optString("source", "unknown")

                if (Regex("""\[\d{2}:\d{2}\.\d{2,3}\]""").containsMatchIn(lyricsText)) {
                    val parsed = parseLrc(lyricsText)
                    if (parsed.isNotEmpty()) {
                        return LyricsResult(
                            lines = parsed,
                            isSynced = true,
                            source = "Lyrica ($sourceName)",
                            plainText = lyricsText
                        )
                    }
                }

                val lines = lyricsText.lines()
                    .filter { it.isNotBlank() }
                    .mapIndexed { i, l -> LyricLine(timestampMs = i * 4000L, text = l.trim()) }

                LyricsResult(
                    lines = lines,
                    isSynced = false,
                    source = "Lyrica ($sourceName)",
                    plainText = lyricsText
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fitPlainLyricsToSongDuration(
        plainText: String,
        totalDurationMs: Long,
        sourceName: String
    ): LyricsResult {
        val validLines = plainText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("Paroles de") && !it.startsWith("Lyrics by") }

        if (validLines.isEmpty()) {
            return LyricsResult(emptyList(), false, sourceName, plainText)
        }

        val trackDuration = if (totalDurationMs > 30_000L) totalDurationMs else (validLines.size * 4500L)
        val introPauseMs = (trackDuration * 0.08f).toLong().coerceIn(6000L, 16000L)
        val outroPauseMs = (trackDuration * 0.05f).toLong().coerceIn(4000L, 12000L)
        val effectiveMs = (trackDuration - introPauseMs - outroPauseMs).coerceAtLeast(10_000L)

        val weights = validLines.map { line -> line.length.coerceIn(15, 80).toFloat() }
        val totalWeight = weights.sum().coerceAtLeast(1.0f)

        var acc = introPauseMs
        val fitted = mutableListOf<LyricLine>()
        for (i in validLines.indices) {
            val lineDur = ((weights[i] / totalWeight) * effectiveMs).toLong().coerceAtLeast(2500L)
            fitted.add(LyricLine(timestampMs = acc, text = validLines[i]))
            acc += lineDur
        }

        return LyricsResult(
            lines = fitted,
            isSynced = true,
            source = "$sourceName (Auto-Fitted)",
            plainText = plainText
        )
    }

    suspend fun translateText(text: String, targetLang: String = "fa"): String? =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext null
            try {
                val hasPersian = Regex("[\u0600-\u06FF]").containsMatchIn(text)
                val effectiveTarget = if (hasPersian && targetLang == "fa") "en" else targetLang

                val encoded = URLEncoder.encode(text.take(2000), "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$effectiveTarget&dt=t&q=$encoded"

                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@withContext null
                        val arr = JSONArray(body)
                        val sentences = arr.getJSONArray(0)
                        val sb = StringBuilder()
                        for (i in 0 until sentences.length()) {
                            sb.append(sentences.getJSONArray(i).getString(0))
                        }
                        val translated = sb.toString().trim()
                        return@withContext if (translated.equals(text.trim(), ignoreCase = true)) null else translated
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Translation error: ${e.message}")
            }
            null
        }

    fun parseLrc(lrcText: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        for (line in lrcText.lines()) {
            val m = LRC_PATTERN.matcher(line.trim())
            if (m.matches()) {
                val min = m.group(1)?.toLongOrNull() ?: 0L
                val sec = m.group(2)?.toLongOrNull() ?: 0L
                val msPart = m.group(3) ?: "0"
                val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                val content = m.group(4)?.trim() ?: ""
                if (content.isNotEmpty()) {
                    result.add(LyricLine(timestampMs = totalMs, text = content))
                }
            }
        }
        return result.sortedBy { it.timestampMs }
    }
}
