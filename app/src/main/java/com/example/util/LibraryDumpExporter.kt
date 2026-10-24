package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports the current song library as a JSON dump for analysis.
 *
 * Output path: context.getExternalFilesDir(null)/library_dump/YYYY-MM-DD_HH-mm-ss.json
 * Shareable via FileProvider.
 *
 * Does NOT include full lyrics text (too large) — only booleans indicating
 * whether synced/plain/translated lyrics are present per song.
 */
object LibraryDumpExporter {

    private const val TAG = "LibraryDumpExporter"

    data class Result(val file: File, val songCount: Int)

    fun export(context: Context, songs: List<Song>): Result? {
        return try {
            val dir = File(context.getExternalFilesDir(null), "library_dump")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val file = File(dir, "library_$timestamp.json")

            val root = JSONObject()
            root.put("exported_at", Date().toString())
            root.put("song_count", songs.size)

            val arr = JSONArray()
            songs.forEach { song ->
                val obj = JSONObject()
                obj.put("id", song.id)
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("album", song.album)
                obj.put("folder", song.folderName)
                obj.put("genre", song.genre)
                obj.put("language", song.detectedLanguage)
                obj.put("durationMs", song.durationMs)
                obj.put("score", song.score.toDouble())
                obj.put("playCount", song.playCount)
                obj.put("skipCount", song.skipCount)
                obj.put("isUnplayed", song.isUnplayed)
                obj.put("hasSyncedLyrics", song.syncedLyrics.isNotBlank())
                obj.put("hasPlainLyrics", song.lyrics.isNotBlank())
                obj.put("hasTranslatedLyrics", song.translatedLyrics.isNotBlank())
                obj.put("uriScheme",
                    if (song.uri.isNotEmpty()) {
                        runCatching { android.net.Uri.parse(song.uri).scheme }.getOrNull() ?: ""
                    } else ""
                )
                arr.put(obj)
            }
            root.put("songs", arr)

            file.writeText(root.toString(2))
            FileLogger.info(TAG, "Exported ${songs.size} songs to ${file.absolutePath}")
            Result(file, songs.size)
        } catch (e: Exception) {
            FileLogger.error(TAG, "Export failed", e)
            null
        }
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SmartMusic Library Dump")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
