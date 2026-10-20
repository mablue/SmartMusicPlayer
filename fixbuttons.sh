#!/data/data/com.termux/files/usr/bin/bash
set -e

PROJ="$HOME/SmartMusicPlayer"
cd "$PROJ"

SERVICE_FILE="app/src/main/java/com/example/player/PlaybackService.kt"
MANIFEST="app/src/main/AndroidManifest.xml"

echo "==> Creating PlaybackService.kt (MediaSessionService)..."
cat > "$SERVICE_FILE" << 'KOT'
package com.example.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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

        // Use the same ExoPlayer instance that SmartAudioPlayer exposes (if any),
        // otherwise create a new one.
        val player: Player = SmartAudioPlayer.instance?.exoPlayer
            ?: ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player).build()
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
KOT

echo "  ✓ PlaybackService.kt created"

echo "==> Updating AndroidManifest.xml..."
python3 - "$MANIFEST" << 'PY'
import sys, re
path = sys.argv[1]
c = open(path).read()

# Remove any old MediaPlaybackService / MediaButtonReceiver / BluetoothAudioReceiver entries
c = re.sub(r'<service\s+android:name="\.player\.MediaPlaybackService".*?</service>', '', c, flags=re.DOTALL)
c = re.sub(r'<receiver\s+android:name="androidx\.media\.session\.MediaButtonReceiver".*?</receiver>', '', c, flags=re.DOTALL)
c = re.sub(r'<receiver\s+android:name="\.player\.BluetoothAudioReceiver".*?</receiver>', '', c, flags=re.DOTALL)

# Add PlaybackService with MediaSessionService intent-filter
service_block = '''
        <service
            android:name=".player.PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
                <action android:name="android.media.browse.MediaBrowserService" />
            </intent-filter>
        </service>
'''
if '.player.PlaybackService' not in c:
    c = c.replace('    </application>', service_block + '    </application>')

# Ensure permissions
for perm in ['FOREGROUND_SERVICE', 'FOREGROUND_SERVICE_MEDIA_PLAYBACK', 'BLUETOOTH_CONNECT']:
    tag = f'<uses-permission android:name="android.permission.{perm}" />'
    if perm not in c:
        c = c.replace('<uses-permission android:name="android.permission.INTERNET" />',
                      '<uses-permission android:name="android.permission.INTERNET" />\n    ' + tag)

open(path, 'w').write(c)
print("  ✓ Manifest updated: PlaybackService added, old receivers removed")
PY

echo ""
echo "==> Adding MusicServiceConnection helper (optional, for future UI binding)..."
# (skip — not required for Bluetooth buttons to work)

echo ""
echo "============================================================"
echo " ✅ Bluetooth button fix applied."
echo " Rebuild:  ./gradlew assembleDebug --no-daemon"
echo "============================================================"
