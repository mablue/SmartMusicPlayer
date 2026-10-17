package com.example.player

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.example.data.preferences.PlaybackPreferences

class BluetoothAudioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = PlaybackPreferences.getInstance(context)

        Log.d("BluetoothAudioReceiver", "Received audio broadcast action: $action")

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                // Bluetooth headset or car audio system connected!
                if (prefs.isBluetoothAutoPlayEnabled) {
                    Log.i("BluetoothAudioReceiver", "Bluetooth audio connected: triggering auto-play")
                    SmartAudioPlayer.instance?.let { player ->
                        if (!player.isPlaying.value) {
                            player.resume()
                        }
                    }
                }
            }

            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1 && prefs.isBluetoothAutoPlayEnabled) {
                    // Headset plugged in
                    SmartAudioPlayer.instance?.let { player ->
                        if (!player.isPlaying.value) {
                            player.resume()
                        }
                    }
                }
            }

            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                // Bluetooth disconnected or headphones unplugged: pause immediately to prevent loudspeaker blast
                Log.i("BluetoothAudioReceiver", "Audio becoming noisy: pausing playback")
                SmartAudioPlayer.instance?.pause()
            }
        }
    }
}
