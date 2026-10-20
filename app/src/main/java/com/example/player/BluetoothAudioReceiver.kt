package com.example.player

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.preferences.PlaybackPreferences

/**
 * Receives Bluetooth ACL_CONNECTED events to auto-play when reconnecting
 * to a car or headphones. Also handles AUDIO_BECOMING_NOISY (as a backup
 * to ExoPlayer's built-in handleAudioBecomingNoisy).
 */
class BluetoothAudioReceiver : BroadcastReceiver() {
    private val TAG = "BluetoothAudioReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d(TAG, "Bluetooth connected — scheduling auto-play")
                // Delay to let audio routing settle
                Handler(Looper.getMainLooper()).postDelayed({
                    val player = SmartAudioPlayer.instance
                    if (player == null) {
                        Log.d(TAG, "No player instance")
                        return@postDelayed
                    }
                    val prefs = PlaybackPreferences.getInstance(context)
                    if (!prefs.isBluetoothAutoPlayEnabled) {
                        Log.d(TAG, "Auto-play disabled in prefs")
                        return@postDelayed
                    }
                    val current = player.currentSong.value
                    if (current == null) {
                        Log.d(TAG, "No current song to resume")
                        return@postDelayed
                    }
                    if (!player.isPlaying.value) {
                        Log.d(TAG, "Resuming: ${current.title}")
                        player.resume()
                    }
                }, 1500L)
            }

            android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Log.d(TAG, "Audio becoming noisy — pausing")
                SmartAudioPlayer.instance?.pause()
            }

            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 0) {
                    Log.d(TAG, "Headset unplugged — pausing")
                    SmartAudioPlayer.instance?.pause()
                }
            }
        }
    }
}
