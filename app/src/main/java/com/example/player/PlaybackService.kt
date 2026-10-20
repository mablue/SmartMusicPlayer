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
 * MediaSessionService — lets Media3 handle Bluetooth/headset media buttons
 * automatically. The custom Callback handles Next/Prev/PlayPause explicitly
 * so single-tap on headsets is reliable across Android 12–15.
 *
 * Reference: developer.android.com/media/legacy/media-buttons
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        FileLogger.info("PlaybackService", "onCreate")

        // Media3 notification provider (channel + icon)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("smart_music_playback")
                .build()
        )

        val instance = SmartAudioPlayer.instance
        val player: Player = if (instance != null) {
            instance.exoPlayer
        } else {
            ExoPlayer.Builder(this).build()
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaCallback())
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.info("PlaybackService", "onStartCommand action=${intent?.action}")

        // Rebind session if SmartAudioPlayer initialized after service
        val instance = SmartAudioPlayer.instance
        if (instance != null && mediaSession?.player !== instance.exoPlayer) {
            FileLogger.info("PlaybackService", "Rebinding session to real ExoPlayer")
            mediaSession?.release()
            mediaSession = MediaSession.Builder(this, instance.exoPlayer)
                .setCallback(MediaCallback())
                .build()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /**
     * Explicit media-button handling.
     *
     * Media3 default behavior is usually fine, but on Android 12–15 some
     * Bluetooth stacks send KeyEvents that Media3 does NOT auto-handle
     * (e.g. some headsets emit KEYCODE_HEADSETHOOK for play/pause, and
     * double-tap of PLAY_PAUSE is not always routed to onSkipToNext).
     */
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

            val player = session.player
            return when (key.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                    player.seekToNextMediaItem()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                    player.seekToPreviousMediaItem()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (player.isPlaying) player.pause() else player.play()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    player.play()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    player.pause()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    player.pause()
                    true
                }
                else -> super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        override fun onPlay() {
            mediaSession?.player?.play()
        }

        override fun onPause() {
            mediaSession?.player?.pause()
        }

        override fun onSkipToNext() {
            mediaSession?.player?.seekToNextMediaItem()
        }

        override fun onSkipToPrevious() {
            mediaSession?.player?.seekToPreviousMediaItem()
        }
    }
}
