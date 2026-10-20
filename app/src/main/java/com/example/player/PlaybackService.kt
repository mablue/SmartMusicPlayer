package com.example.player

import android.content.Intent
import com.example.util.FileLogger
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSessionService

/**
 * MediaSessionService — lets Media3 handle Bluetooth/headset media buttons
 * automatically. No manual MediaButtonReceiver needed.
 *
 * Reference: developer.android.com/guide/topics/media/session/mediasessionservice
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Required for Media3 to build and show the media notification
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("smart_music_playback")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        )
        FileLogger.info("PlaybackService", "onCreate")
        val instance = SmartAudioPlayer.instance
        if (instance != null) {
            mediaSession = MediaSession.Builder(this, instance.exoPlayer).build()
            android.util.Log.d("PlaybackService", "Session created from SmartAudioPlayer instance")
        } else {
            // Service started before app — create standby player so Android's
            // startForegroundService contract is satisfied.
            val standby = ExoPlayer.Builder(this).build()
            mediaSession = MediaSession.Builder(this, standby).build()
            android.util.Log.d("PlaybackService", "Standby ExoPlayer created (SmartAudioPlayer not initialized yet)")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.info("PlaybackService", "onStartCommand action=${intent?.action}")
        // If SmartAudioPlayer initialized after the service, rebind the session
        // to the real ExoPlayer so Bluetooth commands affect the actual playback.
        val instance = SmartAudioPlayer.instance
        if (instance != null && mediaSession?.player !== instance.exoPlayer) {
            android.util.Log.d("PlaybackService", "Rebinding session to SmartAudioPlayer's ExoPlayer")
            mediaSession?.release()
            mediaSession = MediaSession.Builder(this, instance.exoPlayer).build()
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
}
