#!/data/data/com.termux/files/usr/bin/bash
set -e

PROJ="$HOME/SmartMusicPlayer"
cd "$PROJ"

SAP="app/src/main/java/com/example/player/SmartAudioPlayer.kt"

echo "==> Patching SmartAudioPlayer.kt for dynamic EQ attach..."
python3 - "$SAP" << 'PY'
import sys, re
path = sys.argv[1]
c = open(path).read()

# 1) Remove the eager attach in init block
c = re.sub(
    r'\s*// اتصال اکولایزر به ExoPlayer\s*\n\s*exoPlayer\.audioSessionId\?\.let \{ audioEffectManager\.attachToAudioSession\(it\) \}',
    '',
    c
)
c = re.sub(
    r'\s*exoPlayer\.audioSessionId\?\.let \{ audioEffectManager\.attachToAudioSession\(it\) \}',
    '',
    c
)

# 2) Add onAudioSessionIdChanged to the existing Player.Listener
#    Find the listener block and append the callback
anchor = "        exoPlayer.addListener(object : Player.Listener {"
if anchor in c and "onAudioSessionIdChanged" not in c:
    inject = '''
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    android.util.Log.d(TAG, "AudioSessionId changed: $audioSessionId — attaching EQ")
                    audioEffectManager.attachToAudioSession(audioSessionId)
                }
            }
'''
    # Insert right after "exoPlayer.addListener(object : Player.Listener {"
    idx = c.find(anchor) + len(anchor)
    c = c[:idx] + inject + c[idx:]
    print("  ✓ onAudioSessionIdChanged callback added")

# 3) Ensure C import exists (for C.AUDIO_SESSION_ID_UNSET)
if 'import androidx.media3.common.C' not in c:
    lines = c.split('\n')
    last_import = max(i for i, l in enumerate(lines) if l.startswith('import '))
    lines.insert(last_import + 1, 'import androidx.media3.common.C')
    c = '\n'.join(lines)
    print("  ✓ Added import androidx.media3.common.C")

open(path, 'w').write(c)
print("  ✓ SmartAudioPlayer.kt patched")
PY

echo ""
echo "==> Adding fallback: re-attach when playback starts (belt-and-suspenders)..."
python3 - "$SAP" << 'PY'
import sys
path = sys.argv[1]
c = open(path).read()

if 'audioEffectManager.attachToAudioSession(exoPlayer.audioSessionId)' not in c:
    # Add a re-attach check inside playSong, right after exoPlayer.play()
    old = "        exoPlayer.play()"
    new = """        exoPlayer.play()

        // Fallback: ensure EQ is attached to the current session
        runCatching {
            val sid = exoPlayer.audioSessionId
            if (sid != C.AUDIO_SESSION_ID_UNSET && sid > 0) {
                audioEffectManager.attachToAudioSession(sid)
            }
        }"""
    if old in c:
        c = c.replace(old, new, 1)
        open(path, 'w').write(c)
        print("  ✓ Fallback attach in playSong added")
    else:
        print("  ⚠️  playSong anchor not found — check manually")
else:
    print("  ✓ Fallback already present")
PY

echo ""
echo "============================================================"
echo " ✅ Equalizer fix applied."
echo " Rebuild:  ./gradlew assembleDebug --no-daemon"
echo "============================================================"
