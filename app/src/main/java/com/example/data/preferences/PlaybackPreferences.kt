package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PlaybackPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastSongId: Long
        get() = prefs.getLong(KEY_LAST_SONG_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_SONG_ID, value).apply()

    var lastPositionMs: Long
        get() = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_POSITION_MS, value).apply()

    var lastPlaybackMode: String
        get() = prefs.getString(KEY_PLAYBACK_MODE, "SMART_SHUFFLE") ?: "SMART_SHUFFLE"
        set(value) = prefs.edit().putString(KEY_PLAYBACK_MODE, value).apply()

    var isBluetoothAutoPlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_BT_AUTOPLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_BT_AUTOPLAY, value).apply()

    var isLyricsTranslationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LYRICS_TRANSLATION, true)
        set(value) = prefs.edit().putBoolean(KEY_LYRICS_TRANSLATION, value).apply()

    var targetTranslationLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, if (java.util.Locale.getDefault().language == "fa") "fa" else "en") ?: "fa"
        set(value) = prefs.edit().putString(KEY_TARGET_LANG, value).apply()

    var isCarModeAutoLaunchEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAR_MODE_AUTOLAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_CAR_MODE_AUTOLAUNCH, value).apply()

    fun getSongResumePosition(songId: Long): Long {
        if (songId <= 0) return 0L
        return prefs.getLong(KEY_PREFIX_RESUME_POS + songId, 0L)
    }

    /**
     * Saves the playback position for a specific song or podcast track.
     * If the user listened to the end (e.g. >95% or within last 4s), position resets to 0.
     */
    fun saveSongResumePosition(songId: Long, positionMs: Long, durationMs: Long) {
        if (songId <= 0) return
        val isNearEnd = (durationMs > 0 && positionMs >= durationMs * 0.95f) ||
                (durationMs > 4000L && (durationMs - positionMs) < 4000L)
        val posToSave = if (isNearEnd) 0L else positionMs.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_PREFIX_RESUME_POS + songId, posToSave).apply()
    }

    fun clearSongResumePosition(songId: Long) {
        if (songId <= 0) return
        prefs.edit().remove(KEY_PREFIX_RESUME_POS + songId).apply()
    }

    companion object {
        private const val PREFS_NAME = "smart_music_playback_prefs"
        private const val KEY_LAST_SONG_ID = "key_last_song_id"
        private const val KEY_LAST_POSITION_MS = "key_last_position_ms"
        private const val KEY_PLAYBACK_MODE = "key_playback_mode"
        private const val KEY_BT_AUTOPLAY = "key_bt_autoplay"
        private const val KEY_LYRICS_TRANSLATION = "key_lyrics_translation"
        private const val KEY_TARGET_LANG = "key_target_lang"
        private const val KEY_CAR_MODE_AUTOLAUNCH = "key_car_mode_autolaunch"
        private const val KEY_PREFIX_RESUME_POS = "resume_pos_"

        @Volatile
        private var instance: PlaybackPreferences? = null

        fun getInstance(context: Context): PlaybackPreferences {
            return instance ?: synchronized(this) {
                instance ?: PlaybackPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
