package com.example.player

import android.content.Intent
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.util.FileLogger

/**
 * MediaSessionService with explicit media-button handling.
 *
 * Bluetooth Next/Prev MUST route through SmartAudioPlayer.skipNext/
 * skipPrevious — calling player.seekToNextMediaItem() does nothing here
 * because playSong() sets a single media item, not a playlist.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var standbyPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        FileLogger.info("PlaybackService", "onCreate")

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId("smart_music_playback")
            .build()
            .apply { setSmallIcon(com.example.R.drawable.ic_launcher_foreground) }
        setMediaNotificationProvider(provider)

        val instance = SmartAudioPlayer.instance
        val player: Player = if (instance != null) {
            instance.exoPlayer
        } else {
            ExoPlayer.Builder(this).build().also { standbyPlayer = it }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaCallback())
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.info("PlaybackService", "onStartCommand")

        val instance = SmartAudioPlayer.instance
        if (instance != null && mediaSession?.player !== instance.exoPlayer) {
            FileLogger.info("PlaybackService", "Rebinding to real ExoPlayer, releasing standby")
            mediaSession?.release()
            standbyPlayer?.release()
            standbyPlayer = null
            mediaSession = MediaSession.Builder(this, instance.exoPlayer)
                .setCallback(MediaCallback())
                .build()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        FileLogger.info("PlaybackService", "onDestroy")
        mediaSession?.let { session ->
            // Only release if the player is NOT SmartAudioPlayer.exoPlayer
            val shared = session.player === SmartAudioPlayer.instance?.exoPlayer
            session.release()
            if (!shared) {
                runCatching { session.player.release() }
            }
        }
        mediaSession = null
        standbyPlayer = null
        super.onDestroy()
    }

    private inner class MediaCallback : MediaSession.Callback {

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                ?: return super.onMediaButtonEvent(session, controllerInfo, intent)
            if (key.action != KeyEvent.ACTION_DOWN) return true

            FileLogger.info("PlaybackService", "key=${key.keyCode} repeat=${key.repeatCount}")

            val instance = SmartAudioPlayer.instance
            if (instance == null) {
                // No SmartAudioPlayer — fall back to raw player
                return when (key.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        if (session.player.isPlaying) session.player.pause() else session.player.play()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> { session.player.play(); true }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> { session.player.pause(); true }
                    else -> super.onMediaButtonEvent(session, controllerInfo, intent)
                }
            }

            return when (key.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> { instance.skipNext(isManual = true); true }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> { instance.skipPrevious(); true }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> { instance.togglePlayPause(); true }
                KeyEvent.KEYCODE_MEDIA_PLAY -> { instance.resume(); true }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> { instance.pause(); true }
                KeyEvent.KEYCODE_MEDIA_STOP -> { instance.pause(); true }
                else -> super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

    }
}
