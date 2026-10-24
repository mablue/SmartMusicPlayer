package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.example.util.FileLogger

/**
 * PlaybackService with MANUAL notification management.
 *
 * Media3's automatic notification pipeline (onUpdateNotification) does not fire
 * in our setup (no MediaController connected — we drive exoPlayer directly from
 * SmartAudioPlayer). Confirmed via verbose logs on Xiaomi 2201123G / Android 15:
 * onUpdateNotification is never invoked, so no notification is ever built.
 *
 * Solution: manage a MediaStyle notification ourselves, driven by a
 * Player.Listener on the session's player. The session token is set on the
 * MediaStyle so Android's Now Playing / lock screen integration still works.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "smart_music_playback"
        private const val NOTIF_ID = 1
    }

    private var mediaSession: MediaSession? = null
    private var standbyPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null

    override fun onCreate() {
        super.onCreate()
        FileLogger.info(TAG, "onCreate")
        createNotificationChannel()
        logNotificationPermission()

        val instance = SmartAudioPlayer.instance
        val player: Player = if (instance != null) {
            FileLogger.info(TAG, "Using SmartAudioPlayer.exoPlayer")
            instance.exoPlayer
        } else {
            FileLogger.warn(TAG, "SmartAudioPlayer.instance null — standby")
            ExoPlayer.Builder(this).build().also { standbyPlayer = it }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivity())
            .setCallback(MediaCallback())
            .build()

        registerPlayerListener(player)
        FileLogger.info(TAG, "MediaSession built + listener registered: playing=${player.isPlaying}, state=${player.playbackState}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FileLogger.info(TAG, "onStartCommand: playing=${mediaSession?.player?.isPlaying}, playWhenReady=${mediaSession?.player?.playWhenReady}, state=${mediaSession?.player?.playbackState}, items=${mediaSession?.player?.mediaItemCount}")

        val instance = SmartAudioPlayer.instance
        if (instance != null && mediaSession?.player !== instance.exoPlayer) {
            FileLogger.warn(TAG, "Rebinding session to real ExoPlayer")
            mediaSession?.release()
            standbyPlayer?.release()
            standbyPlayer = null
            playerListener?.let { mediaSession?.player?.removeListener(it) }
            mediaSession = MediaSession.Builder(this, instance.exoPlayer)
                .setSessionActivity(buildSessionActivity())
                .setCallback(MediaCallback())
                .build()
            registerPlayerListener(instance.exoPlayer)
            FileLogger.info(TAG, "Session rebound + listener re-registered")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Explicitly override to PREVENT Media3 from ever building a notification.
     * We manage everything ourselves — see class doc.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        FileLogger.info(TAG, "onUpdateNotification IGNORED (using manual notification)")
    }

    private fun registerPlayerListener(player: Player) {
        playerListener?.let { runCatching { player.removeListener(it) } }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                FileLogger.info(TAG, "onIsPlayingChanged: $isPlaying")
                if (isPlaying) startForegroundNotification() else updateNotification()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                FileLogger.info(TAG, "onPlaybackStateChanged: $playbackState")
                when (playbackState) {
                    Player.STATE_READY -> updateNotification()
                    Player.STATE_IDLE -> stopNotification()
                }
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                FileLogger.info(TAG, "onMediaMetadataChanged: ${mediaMetadata.title}")
                updateNotification()
            }
        }
        player.addListener(listener)
        playerListener = listener
    }

    private fun startForegroundNotification() {
        val notif = buildNotification() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, notif)
            }
            FileLogger.info(TAG, "startForeground OK")
        } catch (e: Exception) {
            FileLogger.error(TAG, "startForeground FAILED", e)
        }
    }

    private fun updateNotification() {
        val p = mediaSession?.player ?: return
        if (p.playbackState == Player.STATE_IDLE) return
        if (p.isPlaying) {
            startForegroundNotification()
            return
        }
        val notif = buildNotification() ?: return
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, notif)
            FileLogger.info(TAG, "updateNotification OK (paused)")
        } catch (e: Exception) {
            FileLogger.error(TAG, "updateNotification FAILED", e)
        }
    }

    private fun stopNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            FileLogger.info(TAG, "stopNotification")
        } catch (e: Exception) {
            FileLogger.error(TAG, "stopNotification FAILED", e)
        }
    }

    private fun buildNotification(): Notification? {
        val session = mediaSession ?: return null
        val p = session.player
        val song = SmartAudioPlayer.instance?.currentSong?.value
        val title = p.mediaMetadata.title?.toString() ?: song?.title ?: "SmartMusic"
        val artist = p.mediaMetadata.artist?.toString() ?: song?.artist ?: ""

        val isPlaying = p.isPlaying
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val playPauseAction = NotificationCompat.Action(
            playPauseIcon, playPauseTitle, mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 1)
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Next", mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_NEXT, 2)
        )
        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Previous", mediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, 3)
        )

        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(buildSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setStyle(
                    MediaStyleNotificationHelper.MediaStyle(session)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .build()
        } catch (e: Exception) {
            FileLogger.error(TAG, "buildNotification FAILED", e)
            null
        }
    }

    private fun mediaButtonPendingIntent(keyCode: Int, reqCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setPackage(packageName)
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        return PendingIntent.getBroadcast(
            this, reqCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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
        playerListener?.let { runCatching { mediaSession?.player?.removeListener(it) } }
        playerListener = null
        mediaSession?.let { session ->
            val shared = session.player === SmartAudioPlayer.instance?.exoPlayer
            session.release()
            if (!shared) runCatching { session.player.release() }
        }
        mediaSession = null
        standbyPlayer = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
            FileLogger.info(TAG, "NotificationChannel created")
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
