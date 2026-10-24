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
import com.example.util.FileLogger
import com.example.data.preferences.PlaybackPreferences
import com.example.data.repository.SongRepository
import com.example.engine.PlaybackMode
import com.example.engine.SmartRankingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.data.lyrics.LyricLine
import com.example.data.lyrics.LyricsResult
import com.example.data.lyrics.LyricsService

class SmartAudioPlayer(
    private val context: Context,
    private val repository: SongRepository,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "SmartAudioPlayer"
    private var lyricsJob: Job? = null
    private var translateJob: Job? = null
    @Volatile private var lyricsGeneration: Long = 0L
    @Volatile private var translateGeneration: Long = 0L

    // ---------- ExoPlayer ----------

    // --- ReplayGain (loudness normalization) ---
    // ReplayGain disabled for performance

    internal val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
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

    private var attachJob: Job? = null
    private fun scheduleAudioEffectAttach() {
        attachJob?.cancel()
        attachJob = coroutineScope.launch {
            delay(1200L)
            if (!exoPlayer.isPlaying) return@launch
            runCatching {
                val sid = exoPlayer.audioSessionId
                if (sid != C.AUDIO_SESSION_ID_UNSET && sid > 0) {
                    audioEffectManager.attachToAudioSession(sid)
                    Log.d(TAG, "Effects attached after STATE_READY+1.2s: session=$sid")
                }
            }
        }
    }

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
        // Start MediaSessionService immediately so Bluetooth buttons are routed to us
        startMediaSessionService()
    }

    private fun startMediaSessionService() {
        try {
            val svcIntent = Intent(context, PlaybackService::class.java)
            // SAFETY: use startService, NOT startForegroundService.
            // startForegroundService requires startForeground() within 5-30s.
            // Media3 will call startForeground itself when playback becomes
            // active — but if that fails, Android kills the app. Using
            // startService avoids the timeout entirely. Playback continues,
            // we just lose foreground priority (acceptable trade-off).
            context.startService(svcIntent)
            FileLogger.info(TAG, "PlaybackService.startService OK")
        } catch (e: Exception) {
            FileLogger.warn(TAG, "startService failed: ${e.message}")
            Log.w(TAG, "startService failed: ${e.message}")
        }
    }

    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                FileLogger.info(TAG, "onAudioSessionIdChanged id=$audioSessionId")
                // DO NOT attach here — mutating pipeline during session creation
                // mutes the FIRST play on many devices. Deferred below.
            }



            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (!isPlaying) saveCurrentPosition()
            }

            override fun onPlaybackStateChanged(state: Int) {
                FileLogger.info(TAG, "onPlaybackStateChanged state=$state")
                if (state == Player.STATE_READY) {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                    scheduleAudioEffectAttach()
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

        // CRITICAL FIX: load the media item into ExoPlayer WITHOUT auto-play.
        // Previously, the first user togglePlayPause() called exoPlayer.play()
        // on an EMPTY player (mediaItemCount == 0). That flipped _isPlaying=true
        // but produced no audio and no position advance — user had to skip to
        // another track to "wake up" the player. Now the media item is ready
        // in paused state, so the very first play() actually starts audio.
        val playableUri = song.uri.ifEmpty {
            runCatching { getFallbackUri(song) }.getOrDefault("")
        }
        if (playableUri.isNotEmpty()) {
            runCatching {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(playableUri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .build()
                    )
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(resumePos)
                prefs.lastSongId = song.id
                prefs.lastPositionMs = resumePos
            }
        }

        fetchLyricsForSong(song)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        prefs.lastPlaybackMode = mode.name
    }

    fun playSong(song: Song, startAtMs: Long = -1L) {
        FileLogger.info(TAG, "playSong('${song.title}')")

        // Resolve URI before mutating state
        val playableUri = when {
            song.uri.isNotEmpty() -> song.uri
            else -> getFallbackUri(song).takeIf { it.isNotEmpty() } ?: run {
                FileLogger.warn(TAG, "No playable URI for '${song.title}' — aborting")
                return
            }
        }

        val prevSong = _currentSong.value
        if (prevSong != null && prevSong.id != song.id) {
            saveCurrentPosition()
            if (_isPlaying.value) evaluateAndSaveSession(prevSong, exoPlayer.currentPosition, exoPlayer.duration, true)
        }

        val targetStart = if (startAtMs >= 0) startAtMs else {
            prefs.getSongResumePosition(song.id).takeIf { it > 0 && it < song.durationMs * 0.95 } ?: 0L
        }

        lastPlayedSongId = song.id
        _currentSong.value = song.copy(resumePositionMs = targetStart)
        _currentPositionMs.value = targetStart
        prefs.lastSongId = song.id
        prefs.lastPositionMs = targetStart
        prefs.saveSongResumePosition(song.id, targetStart, song.durationMs)

        fetchLyricsForSong(song)

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(playableUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(targetStart)
        exoPlayer.play()

        try {
            context.startService(Intent(context, PlaybackService::class.java))
            FileLogger.info(TAG, "playSong: startService OK (Media3 will promote to foreground)")
        } catch (e: Exception) {
            FileLogger.error(TAG, "playSong: startService FAILED", e)
        }

        // NOTE: effects attach ONLY from scheduleAudioEffectAttach() (STATE_READY
        // + 1.2s), never directly here. Calling attachToAudioSession during
        // pipeline init was muting the first play AND preventing the deferred
        // attach from ever firing (early-return on same session id).

        _isPlaying.value = true
    }

    private fun getFallbackUri(song: Song): String {
        return ProceduralAudioGenerator.getOrCreateSongAudioFile(
            context,
            song.id,
            durationSeconds = (song.durationMs / 1000).toInt().coerceAtLeast(30)
        )
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

        // Check user queue first
        val q = _queue.value
        val currentIdx = q.indexOfFirst { it.id == song?.id }
        if (currentIdx >= 0 && currentIdx + 1 < q.size) {
            val next = q[currentIdx + 1]
            FileLogger.info(TAG, "skipNext -> from queue: ${next.title}")
            playSong(next)
            return
        }

        // Fallback to playlist ranking
        val next = SmartRankingEngine.selectNextSong(currentPlaylist, song?.id, _playbackMode.value)
        if (next != null) {
            FileLogger.info(TAG, "skipNext -> from playlist: ${next.title}")
            playSong(next)
        }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3000) { seekTo(0); return }
        val song = _currentSong.value ?: return

        val q = _queue.value
        val qIdx = q.indexOfFirst { it.id == song.id }
        if (qIdx > 0) {
            playSong(q[qIdx - 1])
            return
        }

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
        val rawDuration = exoPlayer.duration
        val total = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else song.durationMs.coerceAtLeast(1000L)
        val pos = exoPlayer.currentPosition
        val fullPlay = pos >= total * 0.85f
        if (fullPlay) prefs.saveSongResumePosition(song.id, 0L, song.durationMs)
        evaluateAndSaveSession(song, if (fullPlay) total else pos, total, false)

        if (_playbackMode.value == PlaybackMode.REPEAT_ONE) {
            playSong(song)
            return
        }

        // Advance without double-rating
        val q = _queue.value
        val qIdx = q.indexOfFirst { it.id == song.id }
        if (qIdx >= 0 && qIdx + 1 < q.size) {
            playSong(q[qIdx + 1])
            return
        }

        val next = SmartRankingEngine.selectNextSong(currentPlaylist, song.id, _playbackMode.value)
        next?.let { playSong(it) }
    }

    private fun evaluateAndSaveSession(song: Song, listenedMs: Long, totalMs: Long, isManualSkip: Boolean) {
        val (updated, evaluation) = SmartRankingEngine.evaluateSession(song, listenedMs, totalMs, isManualSkip)
        _lastEvaluation.value = evaluation
        _currentSong.value = updated
        coroutineScope.launch(Dispatchers.IO) { repository.updateSong(updated) }
    }

    fun fetchLyricsForSong(song: Song, force: Boolean = false) {
        val gen = ++lyricsGeneration
        lyricsJob?.cancel()
        lyricsJob = coroutineScope.launch(Dispatchers.IO) {
            if (!force && !prefs.isLyricsAutoSearchEnabled) {
                val cached = repository.getCachedLyrics(song.id)
                if (cached == null) {
                    Log.d(TAG, "Auto-search disabled, no cache for '${song.title}' — skipping")
                    return@launch
                }
            }

            _isFetchingLyrics.value = true
            try {
                val cached = repository.getCachedLyrics(song.id)
                if (cached != null) {
                    val (plain, synced, translated) = cached
                    val baseLines: List<LyricLine> = if (synced.isNotBlank()) {
                        LyricsService.parseLrc(synced)
                    } else {
                        plain.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                            LyricLine(timestampMs = idx * 4000L, text = line.trim())
                        }
                    }

                    // Trust translations only in new format AND only if at least
                    // one line has non-null translation. All-null means the last
                    // attempt failed (e.g. offline) — retry.
                    val transValid: List<String?>? =
                        if (translated.isNotBlank() && translated.contains("\u001F")) {
                            val list = translated.split("\u001F").map {
                                if (it == "\u0000") null else it
                            }
                            if (list.all { it == null }) null else list
                        } else null

                    if (baseLines.isNotEmpty()) {
                        val finalLines = if (transValid != null) {
                            baseLines.mapIndexed { idx, l ->
                                l.copy(translatedText = transValid.getOrNull(idx))
                            }
                        } else baseLines

                        if (gen == lyricsGeneration) {
                            _lyricsResult.value = LyricsResult(
                                lines = finalLines,
                                isSynced = synced.isNotBlank(),
                                source = "Cache",
                                plainText = plain
                            )
                        }
                        Log.d(TAG, "Cache hit for '${song.title}' (trans=${transValid != null})")

                        if (transValid == null && prefs.isLyricsTranslationEnabled) {
                            translateCurrentLyrics(prefs.targetTranslationLanguage)
                        }
                        return@launch
                    }
                }

                val lyrics = LyricsService.fetchLyrics(
                    artist = song.artist,
                    title = song.title,
                    durationSec = song.durationMs / 1000
                )
                if (!isActive || gen != lyricsGeneration) return@launch
                _lyricsResult.value = lyrics

                if (lyrics != null) {
                    val plainText = lyrics.lines.joinToString("\n") { it.text }
                    val syncedText = if (lyrics.isSynced) {
                        lyrics.lines.joinToString("\n") { line ->
                            val ms = line.timestampMs
                            val min = ms / 60000
                            val sec = (ms % 60000) / 1000
                            val centis = (ms % 1000) / 10
                            "[%02d:%02d.%02d] %s".format(min, sec, centis, line.text)
                        }
                    } else ""
                    repository.cacheLyrics(song.id, plainText, syncedText, "")
                    Log.d(TAG, "Cached lyrics for '${song.title}' (synced=${lyrics.isSynced})")
                }

                if (lyrics != null && prefs.isLyricsTranslationEnabled) {
                    translateCurrentLyrics(prefs.targetTranslationLanguage)
                }
            } finally {
                if (gen == lyricsGeneration) {
                    _isFetchingLyrics.value = false
                }
            }
        }
    }

    fun translateCurrentLyrics(targetLang: String) {
        val current = _lyricsResult.value ?: return
        val songId = _currentSong.value?.id ?: return
        val gen = ++translateGeneration
        translateJob?.cancel()
        translateJob = coroutineScope.launch(Dispatchers.IO) {
            _isTranslatingLyrics.value = true
            try {
                val result = ArrayList<LyricLine>(current.lines.size)
                for (chunk in current.lines.chunked(5)) {
                    if (!isActive || gen != translateGeneration) return@launch
                    val batch = chunk.map { line ->
                        async(Dispatchers.IO) {
                            val t = LyricsService.translateText(line.text, targetLang)
                            line.copy(translatedText = t)
                        }
                    }.awaitAll()
                    result.addAll(batch)
                }
                if (gen != translateGeneration) return@launch
                _lyricsResult.value = current.copy(lines = result)
                val plain = current.lines.joinToString("\n") { it.text }
                val synced = if (current.isSynced) current.plainText else ""
                val transBlock = result.joinToString("\u001F") {
                    it.translatedText ?: "\u0000"
                }
                repository.cacheLyrics(songId, plain, synced, transBlock)
                Log.d(TAG, "Cached translation for song $songId (${result.size} lines)")
            } finally {
                if (gen == translateGeneration) {
                    _isTranslatingLyrics.value = false
                }
            }
        }
    }

    fun ensureQueueInitialized(playlist: List<Song>) {
        if (_queue.value.isEmpty() && playlist.isNotEmpty()) {
            _queue.value = playlist
            FileLogger.info(TAG, "Queue initialized with ${playlist.size} songs")
        }
    }

    fun setQueue(songs: List<Song>) {
        _queue.value = songs
        FileLogger.info(TAG, "Queue replaced with ${songs.size} songs")
    }

    fun onSongDeleted(deletedId: Long) {
        val wasCurrent = _currentSong.value?.id == deletedId
        val originalIdx = currentPlaylist.indexOfFirst { it.id == deletedId }
        FileLogger.info(TAG, "onSongDeleted id=$deletedId wasCurrent=$wasCurrent idx=$originalIdx")
        _queue.value = _queue.value.filter { it.id != deletedId }
        currentPlaylist = currentPlaylist.filter { it.id != deletedId }

        if (wasCurrent) {
            val next = when {
                originalIdx in currentPlaylist.indices -> currentPlaylist[originalIdx]
                currentPlaylist.isNotEmpty() -> currentPlaylist.first()
                else -> null
            }
            FileLogger.info(TAG, "onSongDeleted next=${next?.title}")
            exoPlayer.stop()
            _isPlaying.value = false
            _currentPositionMs.value = 0
            _currentSong.value = next
            if (next != null) playSong(next)
        }
    }

    fun release() {
        lyricsJob?.cancel()
        translateJob?.cancel()
        exoPlayer.release()
        audioEffectManager.release()
        _queue.value = emptyList()
        currentPlaylist = emptyList()
        _currentSong.value = null
        _lyricsResult.value = null
        _isPlaying.value = false
        _isFetchingLyrics.value = false
        _isTranslatingLyrics.value = false
        instance = null
    }

    companion object {
        @Volatile
        var instance: SmartAudioPlayer? = null
    }
}
