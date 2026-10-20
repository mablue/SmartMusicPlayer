#!/data/data/com.termux/files/usr/bin/bash
set -uo pipefail

PROJ="$HOME/SmartMusicPlayer"
cd "$PROJ" || { echo "❌ Project not found at $PROJ"; exit 1; }

RG_AP="$PROJ/app/src/main/java/com/example/player/ReplayGainAudioProcessor.kt"
RG_RF="$PROJ/app/src/main/java/com/example/player/ReplayGainRenderersFactory.kt"
NPS="$PROJ/app/src/main/java/com/example/ui/components/NowPlayingSection.kt"
HS="$PROJ/app/src/main/java/com/example/ui/screens/HomeScreen.kt"

echo "=============================================="
echo " Fix 1/4 — ReplayGainAudioProcessor.reset()"
echo "=============================================="
python3 - "$RG_AP" <<'PYEOF'
import sys
path = sys.argv[1]
c = open(path).read()

if 'override fun reset()' in c:
    print("  ✓ Already override")
else:
    old = '    fun reset() {\n        gainFactor = 1.0f\n    }'
    new = '    override fun reset() {\n        super.reset()\n        gainFactor = 1.0f\n    }'
    if old in c:
        c = c.replace(old, new)
        open(path, 'w').write(c)
        print("  ✓ Added override + super.reset()")
    else:
        print("  ⚠️  Pattern not found")
PYEOF

echo ""
echo "=============================================="
echo " Fix 2/4 — ReplayGainRenderersFactory array cast"
echo "=============================================="
python3 - "$RG_RF" <<'PYEOF'
import sys
path = sys.argv[1]
c = open(path).read()

# Add AudioProcessor import if missing
if 'import androidx.media3.common.audio.AudioProcessor' not in c:
    lines = c.split('\n')
    last_import = max(i for i, l in enumerate(lines) if l.startswith('import '))
    lines.insert(last_import + 1, 'import androidx.media3.common.audio.AudioProcessor')
    c = '\n'.join(lines)
    print("  ✓ Added AudioProcessor import")

# DefaultAudioProcessorChain takes vararg, not array
if 'arrayOf(replayGainProcessor)' in c:
    c = c.replace(
        'arrayOf(replayGainProcessor)',
        'replayGainProcessor'
    )
    open(path, 'w').write(c)
    print("  ✓ Unpacked vararg: arrayOf(x) → x")
elif 'DefaultAudioProcessorChain(replayGainProcessor' in c:
    print("  ✓ Already unpacked")
else:
    print("  ⚠️  Pattern not found — check manually")
PYEOF

echo ""
echo "=============================================="
echo " Fix 3/4 — NowPlayingSection: revert LazyRow → Row"
echo "=============================================="
python3 - "$NPS" <<'PYEOF'
import sys, re
path = sys.argv[1]
c = open(path).read()

if 'LazyRow' not in c:
    print("  ✓ No LazyRow — already reverted")
    sys.exit(0)

# Replace LazyRow with Row + horizontalScroll modifier
c = c.replace(
    'androidx.compose.foundation.lazy.LazyRow(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.spacedBy(6.dp),\n            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {',
    'Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .horizontalScroll(rememberScrollState()),\n            horizontalArrangement = Arrangement.spacedBy(6.dp),\n            verticalAlignment = Alignment.CenterVertically\n        ) {'
)

# Fallback if exact pattern didn't match
if 'LazyRow' in c:
    c = c.replace('androidx.compose.foundation.lazy.LazyRow(', 'Row(')
    c = c.replace('LazyRow(', 'Row(')
    c = re.sub(r'contentPadding = androidx\.compose\.foundation\.layout\.PaddingValues\(horizontal = 4\.dp\),\n', '', c)
    if '.horizontalScroll(rememberScrollState())' not in c:
        c = c.replace(
            'Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.spacedBy(6.dp)',
            'Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .horizontalScroll(rememberScrollState()),\n            horizontalArrangement = Arrangement.spacedBy(6.dp)'
        )
    print("  ✓ Generic LazyRow → Row replacement done")
else:
    print("  ✓ Replaced with Row + horizontalScroll")

# Add imports
lines = c.split('\n')
last_import = max(i for i, l in enumerate(lines) if l.startswith('import '))
for imp in [
    'import androidx.compose.foundation.horizontalScroll',
    'import androidx.compose.foundation.rememberScrollState',
]:
    if imp not in c:
        lines.insert(last_import + 1, imp)
        last_import += 1
        print(f"  ✓ Added {imp}")

c = '\n'.join(lines)
open(path, 'w').write(c)
PYEOF

echo ""
echo "=============================================="
echo " Fix 4/4 — HomeScreen: remove orphan )"
echo "=============================================="
python3 - "$HS" <<'PYEOF'
import sys
path = sys.argv[1]
c = open(path).read()
lines = c.split('\n')

# Find "text = \"SmartMusic\""
sm_line = -1
for i, line in enumerate(lines):
    if 'text = "SmartMusic"' in line:
        sm_line = i
        break

if sm_line < 0:
    print("  ⚠️  SmartMusic Text not found")
    # Fallback: check line 358
    if len(lines) > 357 and lines[357].strip() == ')':
        del lines[357]
        open(path, 'w').write('\n'.join(lines))
        print("  ✓ Removed orphan ) at line 358 (fallback)")
    sys.exit(0)

# Find Text( opener (going backward)
text_start = -1
for i in range(sm_line, max(-1, sm_line - 20), -1):
    if 'Text(' in lines[i]:
        text_start = i
        break

if text_start < 0:
    print("  ⚠️  Text( opener not found")
    sys.exit(0)

# Find matching ) by paren balance
depth = 0
text_end = -1
for i in range(text_start, min(len(lines), text_start + 30)):
    depth += lines[i].count('(') - lines[i].count(')')
    if depth == 0 and i > text_start:
        text_end = i
        break

if text_end < 0:
    print("  ⚠️  Text end not found")
    sys.exit(0)

# Find next non-empty line after Text block
next_line = text_end + 1
while next_line < len(lines) and lines[next_line].strip() == '':
    next_line += 1

if next_line < len(lines) and lines[next_line].strip() == ')':
    del lines[next_line]
    open(path, 'w').write('\n'.join(lines))
    print(f"  ✓ Removed orphan ) at line {next_line + 1}")
else:
    print(f"  ℹ️  Next line after Text: '{lines[next_line].strip() if next_line < len(lines) else 'EOF'}'")
PYEOF

echo ""
echo "=============================================="
echo " Verification"
echo "=============================================="
echo "  reset override:        $(grep -c 'override fun reset()' "$RG_AP" 2>/dev/null || echo 0)"
echo "  array unpacked:        $(grep -c 'DefaultAudioProcessorChain(replayGainProcessor' "$RG_RF" 2>/dev/null || echo 0)"
echo "  LazyRow in NPS:        $(grep -c 'LazyRow' "$NPS" 2>/dev/null || echo 0)"
echo "  horizontalScroll:      $(grep -c 'horizontalScroll' "$NPS" 2>/dev/null || echo 0)"
echo "  SmartMusic Text:       $(grep -c 'text = \"SmartMusic\"' "$HS" 2>/dev/null || echo 0)"

echo ""
echo "=============================================="
echo " Now compile:"
echo "=============================================="
echo ""
echo "  ./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | tee compile.log"
echo ""
echo "  if success:"
echo "  ./gradlew assembleDebug --no-daemon 2>&1 | tee build.log"
echo ""
