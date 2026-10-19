package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.model.Song
import java.io.File

object SongShareHelper {

    fun shareSong(context: Context, song: Song) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                val shareText = "🎵 ${song.title} — ${song.artist}\n" +
                        "💿 Album: ${song.album}\n" +
                        "🏷️ Genre: ${song.genre} | ⏱️ Duration: ${song.formattedDuration}\n" +
                        "⭐ Smart Rating: ${song.score.toInt()} pts (${song.rankTier.localizedLabel()})\n\n" +
                        "Shared via SmartMusic Player"

                putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
                putExtra(Intent.EXTRA_TEXT, shareText)

                var hasAudioStream = false
                if (song.uri.isNotEmpty()) {
                    val parsedUri = Uri.parse(song.uri)
                    if (parsedUri.scheme == "content") {
                        putExtra(Intent.EXTRA_STREAM, parsedUri)
                        type = "audio/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        hasAudioStream = true
                    } else {
                        val file = File(if (parsedUri.scheme == "file") parsedUri.path ?: song.uri else song.uri)
                        if (file.exists()) {
                            val fileUri = Uri.fromFile(file)
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            type = "audio/*"
                            hasAudioStream = true
                        }
                    }
                }

                if (!hasAudioStream) {
                    type = "text/plain"
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share \"${song.title}\"")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("SongShareHelper", "Error sharing song", e)
            Toast.makeText(context, "Could not share track", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMultipleSongs(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        try {
            val audioUris = arrayListOf<Uri>()
            val textBuilder = StringBuilder("Shared tracks via SmartMusic:\n")
            songs.forEachIndexed { i, s ->
                textBuilder.append("${i + 1}. ${s.title} — ${s.artist} (${s.formattedDuration})\n")
                if (s.uri.isNotEmpty()) {
                    val u = Uri.parse(s.uri)
                    if (u.scheme == "content") {
                        audioUris.add(u)
                    } else {
                        val f = File(if (u.scheme == "file") u.path ?: s.uri else s.uri)
                        if (f.exists()) audioUris.add(Uri.fromFile(f))
                    }
                }
            }

            val shareIntent = if (audioUris.isNotEmpty()) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "audio/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, audioUris)
                    putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                }
            }

            val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری ${songs.size} آهنگ")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("SongShareHelper", "Error sharing multiple songs", e)
            Toast.makeText(context, "خطا در اشتراک‌گذاری فایل‌ها", Toast.LENGTH_SHORT).show()
        }
    }
}
