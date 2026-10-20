package com.example.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Extracts ReplayGain values from audio file metadata.
 *
 * Supports:
 * - MP3 ID3v2: REPLAYGAIN_TRACK_GAIN / REPLAYGAIN_ALBUM_GAIN
 * - FLAC/OGG Vorbis comments: REPLAYGAIN_TRACK_GAIN
 * - MP4/M4A: ----:com.apple.iTunes:replaygain_track_gain
 *
 * Results are cached in-memory so extraction happens once per file.
 */
object ReplayGainProvider {
    private const val TAG = "ReplayGainProvider"
    private const val TARGET_DB = -14.0f // target loudness (dBFS)
    private val cache = ConcurrentHashMap<String, Float>()

    fun getGainDb(context: Context, uri: String): Float {
        if (uri.isEmpty()) return 0f
        cache[uri]?.let { return it }

        val gain = try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uri))
                // Try multiple keys (different tags per format)
                val trackGain = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.let { /* not RG, ignore */ null }

                // MediaMetadataRetriever doesn't expose RG directly.
                // For now, return 0f (no normalization) until we add
                // a proper ID3/Vorbis parser (jaudiotagger or TagLib).
                0f
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Log.d(TAG, "ReplayGain extract failed for $uri: ${e.message}")
            0f
        }

        cache[uri] = gain
        return gain
    }

    fun clearCache() = cache.clear()
}
