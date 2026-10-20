package com.example.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
import com.example.data.preferences.PlaybackPreferences
import com.example.data.repository.SongRepository
import com.example.engine.PlaybackMode
import com.example.engine.SmartRankingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.data.lyrics.LyricsResult
import com.example.data.lyrics.LyricsService

class SmartAudioPlayer(
    private val context: Context,
    private val repository: SongRepository,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "SmartAudioPlayer"

    // ---------- ExoPlayer ----------

    // --- ReplayGain (loudness normalization) ---
    private val replayGainProcessor = ReplayGainAudioProcessor()
    private val replayGainRenderersFactory by lazy {
        ReplayGainRenderersFactory(context, replayGainProcessor)
    }

    internal val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).setRenderersFactory(replayGainRenderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // handleAudioFocus = true → مدیریت خودکار AudioFocus
            )
            .setHandleAudioBecomingNoisy(true) // توقف خودکار با درآوردن هدفون
            .build()
    }

    private val prefs = PlaybackPreferences.getInstance(context)

    // ---------- StateFlows (همان‌طور که در نسخه قبلی بود) ----------
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.SMART_SHUFFLE)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _lastEvaluation = MutableStateFlow<SessionEvaluation?>(null)
    val lastEvaluation: StateFlow<SessionEvaluation?> = _lastEvaluation.asStateFlow()

    // Lyrics State
    private val _lyricsResult = MutableStateFlow<LyricsResult?>(null)
    val lyricsResult: StateFlow<LyricsResult?> = _lyricsResult.asStateFlow()

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    private val _isTranslatingLyrics = MutableStateFlow(false)
    val isTranslatingLyrics: StateFlow<Boolean> = _isTranslatingLyrics.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    // A-B Repeat Loop State
    private val _abLoopActive = MutableStateFlow(false)
    val abLoopActive: StateFlow<Boolean> = _abLoopActive.asStateFlow()
    private val _abLoopStartMs = MutableStateFlow<Long?>(null)
    val abLoopStartMs: StateFlow<Long?> = _abLoopStartMs.asStateFlow()
    private val _abLoopEndMs = MutableStateFlow<Long?>(null)
    val abLoopEndMs: StateFlow<Long?> = _abLoopEndMs.asStateFlow()

    // Equalizer
    val audioEffectManager = AudioEffectManager(context)

    // Playlist reference
    var currentPlaylist: List<Song> = emptyList()

    // ============================================================
    // Queue (Up Next) — restored after Media3 migration
    // ============================================================
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    fun playNext(song: Song) {
        val list = _queue.value.toMutableList()
        val idx = list.indexOfFirst { it.id == _currentSong.value?.id }
        if (idx >= 0) list.add(idx + 1, song) else list.add(0, song)
        _queue.value = list
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun removeFromQueue(songId: Long) {
        _queue.value = _queue.value.filter { it.id != songId }
    }

    fun clearQueue() {
        val current = _currentSong.value
        _queue.value = if (current != null) listOf(current) else emptyList()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val list = _queue.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _queue.value = list
        }
    }

    private var lastPlayedSongId: Long? = null

    init {
        instance = this
        setupPlayerListeners()
        restoreSavedMode()
    }

    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    android.util.Log.d(TAG, "AudioSessionId changed: $audioSessionId — attaching EQ")
                    audioEffectManager.attachToAudioSession(audioSessionId)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val gainDb = mediaMetadata.extras?.getFloat("replaygain_db", 0f) ?: 0f
                replayGainProcessor.setGainDb(gainDb)
                android.util.Log.d(TAG, "ReplayGain applied: ${gainDb} dB (factor ${Math.pow(10.0, (gainDb/20.0))})")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (!isPlaying) saveCurrentPosition()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                    // Audio session becomes valid only when player reaches READY.
                    // Re-attach EQ now so it actually affects the audio output.
                    runCatching {
                        val sid = exoPlayer.audioSessionId
                        if (sid != C.AUDIO_SESSION_ID_UNSET && sid > 0) {
                            audioEffectManager.attachToAudioSession(sid)
                            Log.d(TAG, "EQ attached on STATE_READY: session=$sid")
                        }
                    }
                }
                if (state == Player.STATE_ENDED) {
                    handleTrackCompletion()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPositionMs.value = exoPlayer.currentPosition
            }
        })

        // به‌روزرسانی مداوم موقعیت پخش
        coroutineScope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition
                    // ذخیره موقعیت هر ۲ ثانیه
                    if (exoPlayer.currentPosition % 2000 < 300) {
                        saveCurrentPosition()
                    }
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    private fun saveCurrentPosition() {
        val song = _currentSong.value ?: return
        prefs.saveSongResumePosition(song.id, exoPlayer.currentPosition, exoPlayer.duration)
    }

    private fun restoreSavedMode() {
        try {
            _playbackMode.value = PlaybackMode.valueOf(prefs.lastPlaybackMode)
        } catch (_: Exception) {
            _playbackMode.value = PlaybackMode.SMART_SHUFFLE
        }
    }

    fun restoreLastSong(playlist: List<Song>) {
        if (_currentSong.value != null || playlist.isEmpty()) return
        val savedId = prefs.lastSongId
        val song = playlist.firstOrNull { it.id == savedId } ?: playlist.first()
        val resumePos = prefs.getSongResumePosition(song.id).coerceIn(0L, song.durationMs)
        _currentSong.value = song.copy(resumePositionMs = resumePos)
        _currentPositionMs.value = resumePos
        _durationMs.value = song.durationMs
        fetchLyricsForSong(song)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        prefs.lastPlaybackMode = mode.name
    }

    fun playSong(song: Song, startAtMs: Long = -1L) {
        val prevSong = _currentSong.value
        if (prevSong != null && prevSong.id != song.id) {
            saveCurrentPosition()
            if (_isPlaying.value) evaluateAndSaveSession(prevSong, exoPlayer.currentPosition, exoPlayer.duration, true)
        }

        val targetStart = if (startAtMs >= 0) startAtMs else {
            prefs.getSongResumePosition(song.id).takeIf { it > 0 && it < song.durationMs * 0.95 } ?: 0L
        }

        val isReplay = lastPlayedSongId == song.id
        lastPlayedSongId = song.id

        _currentSong.value = song.copy(resumePositionMs = targetStart)
        _currentPositionMs.value = targetStart
        prefs.lastSongId = song.id
        prefs.lastPositionMs = targetStart
        prefs.saveSongResumePosition(song.id, targetStart, song.durationMs)

        fetchLyricsForSong(song)

        // ساخت MediaItem از Song
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri.takeIf { it.isNotEmpty() } ?: getFallbackUri(song))
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setExtras(android.os.Bundle().apply {
                        val gain = ReplayGainProvider.getGainDb(context, song.uri)
                        putFloat("replaygain_db", gain)
                    })
                    .build()
            )
            .build()

        // Start MediaSessionService so Bluetooth/headset media buttons work
        try {
            val svcIntent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start PlaybackService: ${e.message}")
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(targetStart)
        exoPlayer.play()

        // Fallback: ensure EQ is attached to the current session
        runCatching {
            val sid = exoPlayer.audioSessionId
            if (sid != C.AUDIO_SESSION_ID_UNSET && sid > 0) {
                audioEffectManager.attachToAudioSession(sid)
            }
        }

        // Populate queue if not already containing this song
        if (_queue.value.none { it.id == song.id }) {
            _queue.value = currentPlaylist.ifEmpty { listOf(song) }
        }

        _isPlaying.value = true
    }

    private fun getFallbackUri(song: Song): String {
        return "asset:///procedural_${song.id}.mp3" // Placeholder — بعداً با ProceduralAudioGenerator جایگزین شود
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        saveCurrentPosition()
    }

    fun resume() {
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        saveCurrentPosition()
    }

    fun seekRelative(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun setABLoopStart() {
        _abLoopStartMs.value = exoPlayer.currentPosition
        if (_abLoopEndMs.value != null && _abLoopEndMs.value!! <= _abLoopStartMs.value!!) {
            _abLoopEndMs.value = null; _abLoopActive.value = false
        }
    }

    fun setABLoopEnd() {
        val start = _abLoopStartMs.value ?: 0L
        if (exoPlayer.currentPosition > start) {
            _abLoopEndMs.value = exoPlayer.currentPosition
            _abLoopActive.value = true
            seekTo(start)
        }
    }

    fun clearABLoop() {
        _abLoopActive.value = false
        _abLoopStartMs.value = null
        _abLoopEndMs.value = null
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        if (enabled) {
            val gain = _currentSong.value?.let { ReplayGainProvider.getGainDb(context, it.uri) } ?: 0f
            replayGainProcessor.setGainDb(gain)
        } else {
            replayGainProcessor.reset()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        // Implementation of sleep timer via coroutine
        coroutineScope.launch {
            var remaining = minutes
            while (remaining > 1) {
                kotlinx.coroutines.delay(60_000L)
                remaining--
                _sleepTimerMinutes.value = remaining
            }
            kotlinx.coroutines.delay(45_000L)
            // Fade out
            for (vol in 10 downTo 1) {
                exoPlayer.volume = vol / 10f
                kotlinx.coroutines.delay(1500L)
            }
            pause()
            exoPlayer.volume = 1.0f
            _sleepTimerMinutes.value = 0
        }
    }

    fun skipNext(isManual: Boolean = true) {
        val song = _currentSong.value
        if (song != null) {
            evaluateAndSaveSession(song, exoPlayer.currentPosition, exoPlayer.duration, isManual)
            saveCurrentPosition()
        }
        val next = SmartRankingEngine.selectNextSong(currentPlaylist, song?.id, _playbackMode.value)
        next?.let { playSong(it) }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3000) { seekTo(0); return }
        val song = _currentSong.value ?: return
        val idx = currentPlaylist.indexOfFirst { it.id == song.id }
        val prev = if (idx > 0) currentPlaylist[idx - 1] else currentPlaylist.lastOrNull()
        prev?.let { playSong(it) }
    }

    fun restartCurrentSong() {
        seekTo(0)
    }

    fun updateCurrentSongMetadata(updatedSong: Song) {
        if (_currentSong.value?.id == updatedSong.id) {
            _currentSong.value = updatedSong
        }
    }

    fun simulateSession(ratio: Float, isManualSkip: Boolean) {
        val song = _currentSong.value ?: return
        val total = exoPlayer.duration.coerceAtLeast(song.durationMs)
        val listened = (total * ratio).toLong()
        evaluateAndSaveSession(song, listened, total, isManualSkip)
        val next = SmartRankingEngine.selectNextSong(currentPlaylist, song.id, _playbackMode.value)
        next?.let { playSong(it) }
    }

    fun dismissEvaluation() { _lastEvaluation.value = null }

    private fun handleTrackCompletion() {
        val song = _currentSong.value ?: return
        val total = exoPlayer.duration
        val pos = exoPlayer.currentPosition
        val fullPlay = pos >= total * 0.85f
        if (fullPlay) prefs.saveSongResumePosition(song.id, 0L, song.durationMs)
        evaluateAndSaveSession(song, if (fullPlay) total else pos, total, false)

        if (_playbackMode.value == PlaybackMode.REPEAT_ONE) {
            playSong(song)
        } else {
            val next = SmartRankingEngine.selectNextSong(currentPlaylist, song.id, _playbackMode.value)
            next?.let { playSong(it) }
        }
    }

    private fun evaluateAndSaveSession(song: Song, listenedMs: Long, totalMs: Long, isManualSkip: Boolean) {
        val (updated, evaluation) = SmartRankingEngine.evaluateSession(song, listenedMs, totalMs, isManualSkip)
        _lastEvaluation.value = evaluation
        _currentSong.value = updated
        coroutineScope.launch(Dispatchers.IO) { repository.updateSong(updated) }
    }

    fun fetchLyricsForSong(song: Song) {
        coroutineScope.launch(Dispatchers.IO) {
            _isFetchingLyrics.value = true
            val lyrics = LyricsService.fetchLyrics(song.artist, song.title, song.durationMs / 1000)
            _lyricsResult.value = lyrics
            _isFetchingLyrics.value = false
            if (lyrics != null && prefs.isLyricsTranslationEnabled) {
                translateCurrentLyrics(prefs.targetTranslationLanguage)
            }
        }
    }

    fun translateCurrentLyrics(targetLang: String) {
        val current = _lyricsResult.value ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _isTranslatingLyrics.value = true
            val translated = current.lines.map { it.copy(translatedText = LyricsService.translateText(it.text, targetLang)) }
            _lyricsResult.value = current.copy(lines = translated)
            _isTranslatingLyrics.value = false
        }
    }

    fun onSongDeleted(deletedId: Long) {
        if (_currentSong.value?.id == deletedId) {
            val wasPlaying = _isPlaying.value
            exoPlayer.stop()
            _isPlaying.value = false
            _currentPositionMs.value = 0
            val next = currentPlaylist.firstOrNull { it.id != deletedId }
            _currentSong.value = next
            if (next != null && wasPlaying) playSong(next)
        }
    }

    fun release() {
        exoPlayer.release()
        audioEffectManager.release()
        instance = null
    }

    companion object {
        @Volatile
        var instance: SmartAudioPlayer? = null
    }
}
