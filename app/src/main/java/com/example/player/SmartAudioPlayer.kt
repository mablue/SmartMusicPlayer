package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.SessionEvaluation
import com.example.data.model.Song
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
    private val handler = Handler(Looper.getMainLooper())

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

    // Playlist reference for next song selection
    var currentPlaylist: List<Song> = emptyList()

    private var lastPlayedSongId: Long? = null

    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.toLong()
                    _durationMs.value = player.duration.coerceAtLeast(0).toLong()
                }
            }
            handler.postDelayed(this, 300)
        }
    }

    init {
        handler.post(progressUpdater)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
    }

    fun playSong(song: Song) {
        // Finalize previous song session if it was playing and changing tracks
        val prevSong = _currentSong.value
        if (prevSong != null && prevSong.id != song.id && _isPlaying.value) {
            evaluateAndSaveSession(prevSong, _currentPositionMs.value, _durationMs.value, isManualSkip = true)
        }

        releasePlayer()

        val isConsecutiveReplay = (lastPlayedSongId == song.id)
        lastPlayedSongId = song.id
        _currentSong.value = song
        _currentPositionMs.value = 0L

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Try local file or URI
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
                        Log.w(TAG, "Could not set provided URI, generating fallback WAV", e)
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
                start()

                setOnCompletionListener {
                    handleTrackCompletion(song, isConsecutiveReplay)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
            }

            mediaPlayer = player
            _isPlaying.value = true
            _durationMs.value = player.duration.toLong().coerceAtLeast(song.durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play song: ${song.title}", e)
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
            }
        } ?: run {
            _currentSong.value?.let { playSong(it) }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
    }

    /**
     * Skip to next song. Evaluates skip behavior (e.g. quick skip = negative score).
     */
    fun skipNext(isManual: Boolean = true) {
        val song = _currentSong.value
        val listenedMs = _currentPositionMs.value
        val totalMs = _durationMs.value

        if (song != null) {
            evaluateAndSaveSession(song, listenedMs, totalMs, isManualSkip = isManual)
        }

        val nextSong = SmartRankingEngine.selectNextSong(
            playlist = currentPlaylist,
            currentSongId = song?.id,
            mode = _playbackMode.value
        )

        if (nextSong != null) {
            playSong(nextSong)
        }
    }

    /**
     * Previous track or restart current.
     */
    fun skipPrevious() {
        if (_currentPositionMs.value > 4000L) {
            seekTo(0)
            return
        }
        val song = _currentSong.value
        val currentIndex = currentPlaylist.indexOfFirst { it.id == song?.id }
        if (currentIndex > 0) {
            playSong(currentPlaylist[currentIndex - 1])
        } else if (currentPlaylist.isNotEmpty()) {
            playSong(currentPlaylist.last())
        }
    }

    /**
     * Simulates a listen session for quick algorithm verification without waiting.
     */
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

        // Select and play next song
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

        // Track completed 100%: Big positive reward!
        evaluateAndSaveSession(
            song = song,
            listenedMs = total,
            totalMs = total,
            isManualSkip = false,
            isConsecutiveReplay = isConsecutiveReplay
        )

        if (_playbackMode.value == PlaybackMode.REPEAT_ONE) {
            playSong(song)
        } else {
            val nextSong = SmartRankingEngine.selectNextSong(
                playlist = currentPlaylist,
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
            releasePlayer()
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            val remaining = currentPlaylist.filter { it.id != deletedSongId }
            _currentSong.value = remaining.firstOrNull()
        }
    }

    fun release() {
        handler.removeCallbacks(progressUpdater)
        releasePlayer()
    }
}
