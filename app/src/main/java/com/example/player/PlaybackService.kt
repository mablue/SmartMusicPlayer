package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.util.FileLogger

class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "smart_music_playback"
    }

    private var mediaSession: MediaSession? = null
    private var standbyPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        FileLogger.info(TAG, "onCreate")

        createNotificationChannelIfNeeded()
        logNotificationPermission()

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .build()
            .apply { setSmallIcon(android.R.drawable.ic_media_play) }
        setMediaNotificationProvider(provider)
        FileLogger.info(TAG, "Notification provider configured: icon=ic_media_play, channel=$CHANNEL_ID")

        val instance = SmartAudioPlayer.instance
        val player: Player = if (instance != null) {
            FileLogger.info(TAG, "Using SmartAudioPlayer.exoPlayer")
            instance.exoPlayer
        } else {
            FileLogger.warn(TAG, "SmartAudioPlayer.instance null — standby player")
            ExoPlayer.Builder(this).build().also { standbyPlayer = it }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivity())
            .setCallback(MediaCallback())
            .build()
        FileLogger.info(TAG, "MediaSession built: playing=${player.isPlaying}, state=${player.playbackState}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.info(TAG, "onStartCommand: action=${intent?.action}, playing=${mediaSession?.player?.isPlaying}, playWhenReady=${mediaSession?.player?.playWhenReady}, state=${mediaSession?.player?.playbackState}, items=${mediaSession?.player?.mediaItemCount}")

        val instance = SmartAudioPlayer.instance
        if (instance != null && mediaSession?.player !== instance.exoPlayer) {
            FileLogger.warn(TAG, "Rebinding session to real ExoPlayer")
            mediaSession?.release()
            standbyPlayer?.release()
            standbyPlayer = null
            mediaSession = MediaSession.Builder(this, instance.exoPlayer)
                .setSessionActivity(buildSessionActivity())
                .setCallback(MediaCallback())
                .build()
            FileLogger.info(TAG, "Session rebound")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Called by Media3 when the session decides the notification should be shown/updated.
     * Logging here tells us definitively whether Media3's notification pipeline runs.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val p = session.player
        FileLogger.info(
            TAG,
            "onUpdateNotification: startInForeground=$startInForegroundRequired, " +
            "playWhenReady=${p.playWhenReady}, state=${p.playbackState}, " +
            "items=${p.mediaItemCount}, isPlaying=${p.isPlaying}"
        )
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
            FileLogger.info(TAG, "onUpdateNotification: super() OK")
        } catch (e: Exception) {
            FileLogger.error(TAG, "onUpdateNotification: super() FAILED", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        FileLogger.info(TAG, "onGetSession: pkg=${controllerInfo.packageName}")
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        val shouldStop = p == null || !p.playWhenReady || p.mediaItemCount == 0
        FileLogger.info(TAG, "onTaskRemoved: shouldStop=$shouldStop")
        if (shouldStop) stopSelf()
    }

    override fun onDestroy() {
        FileLogger.info(TAG, "onDestroy")
        mediaSession?.let { session ->
            val shared = session.player === SmartAudioPlayer.instance?.exoPlayer
            session.release()
            if (!shared) runCatching { session.player.release() }
        }
        mediaSession = null
        standbyPlayer = null
        super.onDestroy()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
            FileLogger.info(TAG, "NotificationChannel created: $CHANNEL_ID (importance=LOW)")
        } else {
            FileLogger.info(TAG, "NotificationChannel exists: importance=${existing.importance}")
        }
    }

    private fun logNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            FileLogger.info(TAG, "POST_NOTIFICATIONS granted=$granted")
        }
    }

    private fun buildSessionActivity(): PendingIntent {
        val intent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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
            FileLogger.info(TAG, "mediaButton key=${key.keyCode}")

            val instance = SmartAudioPlayer.instance
            if (instance == null) {
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
