package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song

class MediaPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null

    private var lastPreviousClickTime = 0L
    private var lastNextClickTime = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        mediaSession = MediaSession(this, "SmartMusicMediaSession").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    } ?: return super.onMediaButtonEvent(mediaButtonIntent)

                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                if (keyEvent.repeatCount > 0) {
                                    // Long press holding: continuous rewind
                                    SmartAudioPlayer.instance?.seekRelative(-10000L)
                                } else {
                                    val now = SystemClock.uptimeMillis()
                                    if (now - lastPreviousClickTime < 500L) {
                                        // Double-tap in rapid succession -> Previous track!
                                        lastPreviousClickTime = 0L
                                        SmartAudioPlayer.instance?.skipPrevious()
                                    } else {
                                        // Single tap default -> rewind 15 seconds!
                                        lastPreviousClickTime = now
                                        SmartAudioPlayer.instance?.seekRelative(-15000L)
                                    }
                                }
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                if (keyEvent.repeatCount > 0) {
                                    // Long press holding: continuous fast forward
                                    SmartAudioPlayer.instance?.seekRelative(10000L)
                                } else {
                                    val now = SystemClock.uptimeMillis()
                                    if (now - lastNextClickTime < 500L) {
                                        // Double-tap in rapid succession -> Next track!
                                        lastNextClickTime = 0L
                                        SmartAudioPlayer.instance?.skipNext(isManual = true)
                                    } else {
                                        // Single tap default -> forward 15 seconds!
                                        lastNextClickTime = now
                                        SmartAudioPlayer.instance?.seekRelative(15000L)
                                    }
                                }
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                SmartAudioPlayer.instance?.seekRelative(-15000L)
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                SmartAudioPlayer.instance?.seekRelative(15000L)
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                SmartAudioPlayer.instance?.togglePlayPause()
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                SmartAudioPlayer.instance?.resume()
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                SmartAudioPlayer.instance?.pause()
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                SmartAudioPlayer.instance?.pause()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }

                override fun onPlay() {
                    SmartAudioPlayer.instance?.resume()
                }

                override fun onPause() {
                    SmartAudioPlayer.instance?.pause()
                }

                override fun onSkipToNext() {
                    SmartAudioPlayer.instance?.skipNext(isManual = true)
                }

                override fun onSkipToPrevious() {
                    SmartAudioPlayer.instance?.skipPrevious()
                }

                override fun onFastForward() {
                    SmartAudioPlayer.instance?.seekRelative(15000L)
                }

                override fun onRewind() {
                    SmartAudioPlayer.instance?.seekRelative(-15000L)
                }

                override fun onSeekTo(pos: Long) {
                    SmartAudioPlayer.instance?.seekTo(pos)
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY -> SmartAudioPlayer.instance?.resume()
            ACTION_PAUSE -> SmartAudioPlayer.instance?.pause()
            ACTION_PLAY_PAUSE -> SmartAudioPlayer.instance?.togglePlayPause()
            ACTION_NEXT -> SmartAudioPlayer.instance?.skipNext(isManual = true)
            ACTION_PREVIOUS -> SmartAudioPlayer.instance?.skipPrevious()
            ACTION_REWIND_15 -> SmartAudioPlayer.instance?.seekRelative(-15000L)
            ACTION_FORWARD_15 -> SmartAudioPlayer.instance?.seekRelative(15000L)
            ACTION_BOOST -> SmartAudioPlayer.instance?.simulateSession(ratio = 1.0f, isManualSkip = false)
            ACTION_STOP -> {
                SmartAudioPlayer.instance?.pause()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: ""
                val songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: ""
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val score = intent.getIntExtra(EXTRA_SCORE, 50)
                val tier = intent.getStringExtra(EXTRA_TIER) ?: ""

                if (songTitle.isNotEmpty()) {
                    val notification = buildNotification(songTitle, songArtist, isPlaying, score, tier)
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }

        return START_STICKY
    }

    private fun buildNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        score: Int,
        tier: String
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_FULL_PLAYER", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val prevPending = PendingIntent.getService(
            this, 1,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewind15Pending = PendingIntent.getService(
            this, 2,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_REWIND_15),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePending = PendingIntent.getService(
            this, 3,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forward15Pending = PendingIntent.getService(
            this, 4,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_FORWARD_15),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPending = PendingIntent.getService(
            this, 5,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this, 6,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) getString(R.string.action_pause) else getString(R.string.action_play)

        // Create default album art thumbnail
        val largeIcon: Bitmap? = try {
            BitmapFactory.decodeResource(resources, R.drawable.default_album_art_1792079403842)
        } catch (e: Exception) {
            null
        }

        val subtext = "$score pts • $tier"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_fg_1792079391319)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText("$artist • $subtext")
            .setSubText(subtext)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.action_previous), prevPending)
            .addAction(android.R.drawable.ic_media_rewind, getString(R.string.action_rewind_15), rewind15Pending)
            .addAction(playPauseIcon, playPauseText, playPausePending)
            .addAction(android.R.drawable.ic_media_ff, getString(R.string.action_forward_15), forward15Pending)
            .addAction(android.R.drawable.ic_media_next, getString(R.string.action_next), nextPending)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)
                    .setMediaSession(mediaSession?.sessionToken?.let {
                        android.support.v4.media.session.MediaSessionCompat.Token.fromToken(it)
                    })
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartMusic Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls media playback, track navigation, and adaptive ranking actions"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "smart_music_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.smartmusic.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.smartmusic.ACTION_PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.smartmusic.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.smartmusic.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.smartmusic.ACTION_PREVIOUS"
        const val ACTION_REWIND_15 = "com.example.smartmusic.ACTION_REWIND_15"
        const val ACTION_FORWARD_15 = "com.example.smartmusic.ACTION_FORWARD_15"
        const val ACTION_BOOST = "com.example.smartmusic.ACTION_BOOST"
        const val ACTION_STOP = "com.example.smartmusic.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.smartmusic.ACTION_UPDATE"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_TIER = "extra_tier"

        fun updateNotification(context: Context, song: Song?, isPlaying: Boolean) {
            if (song == null) return
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_SONG_TITLE, song.title)
                putExtra(EXTRA_SONG_ARTIST, song.artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_SCORE, song.score.toInt())
                putExtra(EXTRA_TIER, song.rankTier.localizedLabel())
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore if background start restricted
            }
        }

        fun stopNotification(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {}
        }
    }
}
