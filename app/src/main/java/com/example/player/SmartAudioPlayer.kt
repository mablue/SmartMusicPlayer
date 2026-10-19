package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.lyrics.LyricLine
import com.example.data.lyrics.LyricsResult
import com.example.data.lyrics.LyricsService
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
import java.io.File

class SmartAudioPlayer(
    private val context: Context,
    private val repository: SongRepository,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "SmartAudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var pausedByFocusLoss = false
    private var duckedByFocusLoss = false
    private val desiredVolume = 0.6f
    private val focusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                if (_isPlaying.value) { pausedByFocusLoss = true; pause() }
                abandonAudioFocus()
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) { pausedByFocusLoss = true; pause() }
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_isPlaying.value) {
                    duckedByFocusLoss = true
                    try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (_: Exception) { }
                }
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                if (duckedByFocusLoss) {
                    duckedByFocusLoss = false
                    try { mediaPlayer?.setVolume(desiredVolume, desiredVolume) } catch (_: Exception) { }
                }
                if (pausedByFocusLoss) { pausedByFocusLoss = false; resume() }
            }
        }
    }
    private fun requestAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusChangeListener,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN)
        }
        return r == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = PlaybackPreferences.getInstance(context)

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

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    var stopAtEndOfTrack: Boolean = false

    // Equalizer & Audio Effects
    val audioEffectManager = AudioEffectManager(context)

    // Playback Queue State (Up Next)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // A-B Repeat Loop State
    private val _abLoopActive = MutableStateFlow(false)
    val abLoopActive: StateFlow<Boolean> = _abLoopActive.asStateFlow()

    private val _abLoopStartMs = MutableStateFlow<Long?>(null)
    val abLoopStartMs: StateFlow<Long?> = _abLoopStartMs.asStateFlow()

    private val _abLoopEndMs = MutableStateFlow<Long?>(null)
    val abLoopEndMs: StateFlow<Long?> = _abLoopEndMs.asStateFlow()

    // Playlist reference for next song selection
    var currentPlaylist: List<Song> = emptyList()

    private var lastPlayedSongId: Long? = null
    @Volatile
    private var isSeeking = false

    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && !isSeeking) {
                    val pos = player.currentPosition.toLong()
                    val dur = player.duration.coerceAtLeast(0).toLong()
                    if (dur > 0) {
                        _durationMs.value = dur
                    }

                    // Guard against MediaPlayer temporarily reporting 0 while seeking to saved resume position
                    if (pos > 0 || _currentPositionMs.value < 1000L) {
                        _currentPositionMs.value = pos
                    }

                    // Check A-B Repeat Loop
                    if (_abLoopActive.value) {
                        val loopEnd = _abLoopEndMs.value
                        val loopStart = _abLoopStartMs.value ?: 0L
                        if (loopEnd != null && pos >= loopEnd) {
                            player.seekTo(loopStart.toInt())
                            _currentPositionMs.value = loopStart
                        }
                    }

                    // Periodic save of playback position (every 2 seconds)
                    if (pos > 0 && pos % 2000L < 300L) {
                        prefs.lastPositionMs = pos
                        _currentSong.value?.let { s ->
                            prefs.saveSongResumePosition(s.id, pos, dur)
                        }
                    }
                }
            }
            handler.postDelayed(this, 250)
        }
    }

    init {
        instance = this
        handler.post(progressUpdater)
        restoreSavedMode()
    }

    private fun restoreSavedMode() {
        try {
            val savedMode = PlaybackMode.valueOf(prefs.lastPlaybackMode)
            _playbackMode.value = savedMode
        } catch (e: Exception) {
            _playbackMode.value = PlaybackMode.SMART_SHUFFLE
        }
    }

    fun restoreLastSong(playlist: List<Song>) {
        if (_currentSong.value != null || playlist.isEmpty()) return
        val savedId = prefs.lastSongId
        val savedSong = if (savedId > 0) playlist.firstOrNull { it.id == savedId } else null
        val songToPrepare = savedSong ?: playlist.firstOrNull()

        if (songToPrepare != null) {
            val resumePos = prefs.getSongResumePosition(songToPrepare.id).let {
                if (it > 0L) it else prefs.lastPositionMs
            }.coerceIn(0L, songToPrepare.durationMs)

            _currentSong.value = songToPrepare.copy(resumePositionMs = resumePos)
            _currentPositionMs.value = resumePos
            _durationMs.value = songToPrepare.durationMs
            fetchLyricsForSong(songToPrepare)
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        prefs.lastPlaybackMode = mode.name
    }

    /**
     * Plays a song, automatically resuming from its saved position if startAtMs is negative (-1L).
     * If the user previously completed or skipped near the end, saved position will be 0 (starts from beginning).
     */
    fun playSong(song: Song, startAtMs: Long = -1L) {
        requestAudioFocus()
        val prevSong = _currentSong.value
        if (prevSong != null && prevSong.id != song.id) {
            prefs.saveSongResumePosition(prevSong.id, _currentPositionMs.value, _durationMs.value)
            if (_isPlaying.value) {
                evaluateAndSaveSession(prevSong, _currentPositionMs.value, _durationMs.value, isManualSkip = true)
            }
        }

        releasePlayer()

        val targetStartMs = if (startAtMs >= 0L) {
            startAtMs
        } else {
            val savedPos = prefs.getSongResumePosition(song.id)
            if (savedPos > 0L && (song.durationMs <= 0 || savedPos < (song.durationMs * 0.95f).toLong())) {
                savedPos
            } else {
                0L
            }
        }

        val isConsecutiveReplay = (lastPlayedSongId == song.id)
        lastPlayedSongId = song.id
        _currentSong.value = song.copy(resumePositionMs = targetStartMs)
        _currentPositionMs.value = targetStartMs

        // Persist active song ID and resume position
        prefs.lastSongId = song.id
        prefs.lastPositionMs = targetStartMs
        prefs.saveSongResumePosition(song.id, targetStartMs, song.durationMs)

        // Trigger automatic lyrics fetch
        fetchLyricsForSong(song)

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                var uriSet = false
                if (song.uri.isNotEmpty()) {
                    try {
                        val file = File(song.uri)
                        if (file.exists()) {
                            setDataSource(song.uri)
                            uriSet = true
                        } else {
                            setDataSource(context, Uri.parse(song.uri))
                            uriSet = true
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set provided URI, using procedural fallback", e)
                    }
                }

                if (!uriSet) {
                    val fallbackPath = ProceduralAudioGenerator.getOrCreateSongAudioFile(
                        context,
                        song.id,
                        durationSeconds = (song.durationMs / 1000).toInt().coerceAtLeast(30)
                    )
                    setDataSource(fallbackPath)
                }

                prepare()
                val realDur = duration.toLong()
                val safeTarget = if (realDur > 0) {
                    targetStartMs.coerceIn(0L, (realDur - 800L).coerceAtLeast(0L))
                } else {
                    targetStartMs
                }

                if (safeTarget > 0) {
                    isSeeking = true
                    seekTo(safeTarget.toInt())
                    setOnSeekCompleteListener {
                        isSeeking = false
                    }
                }
                start()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(_playbackSpeed.value)
                    } catch (e: Exception) { Log.w(TAG, "setSpeed failed: ${e.message}") }
                }

                setOnCompletionListener {
                    handleTrackCompletion(song, isConsecutiveReplay)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    updateNotification()
                    true
                }
            }

            mediaPlayer = player
            try {
                audioEffectManager.attachToAudioSession(player.audioSessionId)
            } catch (e: Exception) {
                Log.d(TAG, "Audio effect attach: ${e.message}")
            }
            if (!_queue.value.any { it.id == song.id }) {
                _queue.value = currentPlaylist.ifEmpty { listOf(song) }
            }
            _isPlaying.value = true
            _durationMs.value = player.duration.toLong().coerceAtLeast(song.durationMs)
            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play song: ${song.title}", e)
            _isPlaying.value = false
            updateNotification()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                prefs.lastPositionMs = _currentPositionMs.value
                _currentSong.value?.let { song ->
                    prefs.saveSongResumePosition(song.id, _currentPositionMs.value, _durationMs.value)
                }
                updateNotification()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                updateNotification()
            }
        } ?: run {
            _currentSong.value?.let { playSong(it, _currentPositionMs.value) }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        isSeeking = true
        mediaPlayer?.setOnSeekCompleteListener {
            isSeeking = false
        }
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
        prefs.lastPositionMs = positionMs
        _currentSong.value?.let { song ->
            prefs.saveSongResumePosition(song.id, positionMs, _durationMs.value)
        }
    }

    fun seekRelative(deltaMs: Long) {
        val maxDuration = _durationMs.value.coerceAtLeast(1000L)
        val target = (_currentPositionMs.value + deltaMs).coerceIn(0L, maxDuration)
        seekTo(target)
    }

    fun setABLoopStart() {
        _abLoopStartMs.value = _currentPositionMs.value
        if (_abLoopEndMs.value != null && _abLoopEndMs.value!! <= _currentPositionMs.value) {
            _abLoopEndMs.value = null
            _abLoopActive.value = false
        }
    }

    fun setABLoopEnd() {
        val start = _abLoopStartMs.value ?: 0L
        if (_currentPositionMs.value > start) {
            _abLoopEndMs.value = _currentPositionMs.value
            _abLoopActive.value = true
            seekTo(start)
        }
    }

    fun clearABLoop() {
        _abLoopActive.value = false
        _abLoopStartMs.value = null
        _abLoopEndMs.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set playback speed", e)
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        stopAtEndOfTrack = (minutes == -1)
        if (minutes <= 0) return

        sleepTimerJob = coroutineScope.launch {
            var remaining = minutes
            while (remaining > 1) {
                kotlinx.coroutines.delay(60_000L)
                remaining--
                _sleepTimerMinutes.value = remaining
            }
            // In final minute: 45s normal, then 15s gentle volume fade-out
            kotlinx.coroutines.delay(45_000L)
            for (vol in 10 downTo 1) {
                try {
                    mediaPlayer?.setVolume(vol / 10f, vol / 10f)
                } catch (e: Exception) { Log.w(TAG, "fade volume step failed: ${e.message}") }
                kotlinx.coroutines.delay(1500L)
            }
            pause()
            try {
                mediaPlayer?.setVolume(desiredVolume, desiredVolume)
            } catch (e: Exception) { Log.w(TAG, "restore volume failed: ${e.message}") }
            _sleepTimerMinutes.value = 0
        }
    }

    fun playNext(song: Song) {
        val currentList = _queue.value.toMutableList()
        val currentIdx = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        if (currentIdx != -1) {
            currentList.add(currentIdx + 1, song)
        } else {
            currentList.add(0, song)
        }
        _queue.value = currentList
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun removeFromQueue(songId: Long) {
        _queue.value = _queue.value.filter { it.id != songId }
    }

    fun clearQueue() {
        val curr = _currentSong.value
        _queue.value = if (curr != null) listOf(curr) else emptyList()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _queue.value = list
        }
    }

    fun skipNext(isManual: Boolean = true) {
        val song = _currentSong.value
        val listenedMs = _currentPositionMs.value
        val totalMs = _durationMs.value

        if (song != null) {
            evaluateAndSaveSession(song, listenedMs, totalMs, isManualSkip = isManual)
            prefs.saveSongResumePosition(song.id, listenedMs, totalMs)
        }

        // Check if queue has upcoming tracks
        val q = _queue.value
        val currentIdx = q.indexOfFirst { it.id == song?.id }
        val nextInQueue = if (currentIdx != -1 && currentIdx + 1 < q.size) {
            q[currentIdx + 1]
        } else null

        val nextSong = nextInQueue ?: SmartRankingEngine.selectNextSong(
            playlist = if (q.isNotEmpty()) q else currentPlaylist,
            currentSongId = song?.id,
            mode = _playbackMode.value
        )

        if (nextSong != null) {
            playSong(nextSong)
        }
    }

    fun skipPrevious() {
        val song = _currentSong.value
        val pos = _currentPositionMs.value

        if (pos > 3000L) {
            seekTo(0)
            return
        }

        if (song != null) {
            prefs.saveSongResumePosition(song.id, pos, _durationMs.value)
        }

        val q = _queue.value
        val currentIdx = q.indexOfFirst { it.id == song?.id }
        val prevInQueue = if (currentIdx > 0) q[currentIdx - 1] else null

        val prevSong = prevInQueue ?: if (currentPlaylist.isNotEmpty()) {
            val listIdx = currentPlaylist.indexOfFirst { it.id == song?.id }
            if (listIdx > 0) currentPlaylist[listIdx - 1] else currentPlaylist.last()
        } else null

        if (prevSong != null) {
            playSong(prevSong)
        }
    }

    fun restartCurrentSong() {
        val song = _currentSong.value ?: return
        prefs.saveSongResumePosition(song.id, 0L, _durationMs.value)
        seekTo(0)
    }

    fun updateCurrentSongMetadata(updatedSong: Song) {
        if (_currentSong.value?.id == updatedSong.id) {
            _currentSong.value = updatedSong
            updateNotification()
        }
    }

    fun simulateSession(ratio: Float, isManualSkip: Boolean) {
        val song = _currentSong.value ?: return
        val total = _durationMs.value.coerceAtLeast(song.durationMs)
        val simulatedListenedMs = (total * ratio).toLong()

        evaluateAndSaveSession(
            song = song,
            listenedMs = simulatedListenedMs,
            totalMs = total,
            isManualSkip = isManualSkip
        )

        val nextSong = SmartRankingEngine.selectNextSong(
            playlist = currentPlaylist,
            currentSongId = song.id,
            mode = _playbackMode.value
        )
        nextSong?.let { playSong(it) }
    }

    fun dismissEvaluation() {
        _lastEvaluation.value = null
    }

    private fun handleTrackCompletion(song: Song, isConsecutiveReplay: Boolean) {
        _isPlaying.value = false
        val total = _durationMs.value.coerceAtLeast(song.durationMs)
        val pos = _currentPositionMs.value

        // Only clear resume position if user truly listened to the full track (>85% or within last 4s)
        val isRealFullPlay = (total > 0 && pos >= total * 0.85f) || (total > 4000L && (total - pos) <= 4000L)
        if (isRealFullPlay) {
            prefs.saveSongResumePosition(song.id, 0L, song.durationMs)
        }

        evaluateAndSaveSession(
            song = song,
            listenedMs = if (isRealFullPlay) total else pos,
            totalMs = total,
            isManualSkip = false,
            isConsecutiveReplay = isConsecutiveReplay
        )

        if (stopAtEndOfTrack) {
            stopAtEndOfTrack = false
            _sleepTimerMinutes.value = 0
            pause()
            return
        }

        if (_playbackMode.value == PlaybackMode.REPEAT_ONE) {
            playSong(song)
        } else {
            val q = _queue.value
            val currentIdx = q.indexOfFirst { it.id == song.id }
            val nextInQueue = if (currentIdx != -1 && currentIdx + 1 < q.size) {
                q[currentIdx + 1]
            } else null

            val nextSong = nextInQueue ?: SmartRankingEngine.selectNextSong(
                playlist = if (q.isNotEmpty()) q else currentPlaylist,
                currentSongId = song.id,
                mode = _playbackMode.value
            )
            nextSong?.let { playSong(it) }
        }
    }

    private fun evaluateAndSaveSession(
        song: Song,
        listenedMs: Long,
        totalMs: Long,
        isManualSkip: Boolean,
        isConsecutiveReplay: Boolean = false
    ) {
        val (updatedSong, evaluation) = SmartRankingEngine.evaluateSession(
            song = song,
            listenedMs = listenedMs,
            totalMs = totalMs,
            isManualSkip = isManualSkip,
            isConsecutiveReplay = isConsecutiveReplay
        )

        _lastEvaluation.value = evaluation
        _currentSong.value = updatedSong

        coroutineScope.launch(Dispatchers.IO) {
            repository.updateSong(updatedSong)
        }
        updateNotification()
    }

    fun fetchLyricsForSong(song: Song) {
        coroutineScope.launch(Dispatchers.IO) {
            _isFetchingLyrics.value = true
            val lyrics = LyricsService.fetchLyrics(
                artist = song.artist,
                title = song.title,
                durationSec = song.durationMs / 1000
            )
            _lyricsResult.value = lyrics
            _isFetchingLyrics.value = false

            // Auto-translate if enabled in preferences
            if (lyrics != null && prefs.isLyricsTranslationEnabled) {
                translateCurrentLyrics(prefs.targetTranslationLanguage)
            }
        }
    }

    fun translateCurrentLyrics(targetLang: String) {
        val current = _lyricsResult.value ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _isTranslatingLyrics.value = true
            val translatedLines = current.lines.map { line ->
                val translated = LyricsService.translateText(line.text, targetLang)
                line.copy(translatedText = translated)
            }
            _lyricsResult.value = current.copy(lines = translatedLines)
            _isTranslatingLyrics.value = false
        }
    }

    private fun updateNotification() {
        val song = _currentSong.value
        MediaPlaybackService.updateNotification(context, song, _isPlaying.value)
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        }
        mediaPlayer = null
    }

    fun onSongDeleted(deletedSongId: Long) {
        if (_currentSong.value?.id == deletedSongId) {
            val wasPlaying = _isPlaying.value
            val playlist = currentPlaylist
            val currentIndex = playlist.indexOfFirst { it.id == deletedSongId }
            releasePlayer()
            _isPlaying.value = false
            _currentPositionMs.value = 0L

            val nextSong = if (currentIndex >= 0 && currentIndex < playlist.size - 1) {
                playlist[currentIndex + 1]
            } else {
                val remaining = playlist.filter { it.id != deletedSongId }
                remaining.firstOrNull()
            }

            _currentSong.value = nextSong
            if (nextSong != null) {
                prefs.lastSongId = nextSong.id
                prefs.lastPositionMs = 0L
                if (wasPlaying) {
                    playSong(nextSong)
                }
            } else {
                MediaPlaybackService.stopNotification(context)
            }
        }
    }

    fun release() {
        abandonAudioFocus()
        handler.removeCallbacks(progressUpdater)
        releasePlayer()
        MediaPlaybackService.stopNotification(context)
        instance = null
    }

    companion object {
        @Volatile
        var instance: SmartAudioPlayer? = null
    }
}
