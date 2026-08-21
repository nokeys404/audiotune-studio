#!/bin/bash
set -e

echo "=================================================="
echo " STAGE 10 FINAL FIX - SHARED AUDIO ENGINE LIFECYCLE"
echo "=================================================="

ADAPTER="app/src/main/java/com/audiotune/studio/audio/engine/Media3AudioProcessorAdapter.kt"

echo ""
echo "===== 1. CHECK CURRENT RESET IMPLEMENTATION ====="
grep -n -A 15 "override fun reset" "$ADAPTER" || true

echo ""
echo "===== 2. REMOVE SHARED ENGINE RELEASE FROM ADAPTER ====="

python3 - <<'PY'
from pathlib import Path

path = Path("app/src/main/java/com/audiotune/studio/audio/engine/Media3AudioProcessorAdapter.kt")
text = path.read_text()

old = """        audioEngine.release()
"""

if old in text:
    text = text.replace(
        old,
        """        // AudioEngine is a shared singleton owned by AppContainer.
        // Do NOT release it from the Media3 processor adapter.
        // The application lifecycle owns the AudioEngine lifetime.
"""
    )

path.write_text(text)
print("Fixed: AudioEngine.release() removed from Media3AudioProcessorAdapter.reset().")
PY

echo ""
echo "===== 3. VERIFY NO ADAPTER RELEASE CALL REMAINS ====="

if grep -n "audioEngine.release" "$ADAPTER"; then
    echo "ERROR: audioEngine.release() still exists in adapter."
    exit 1
else
    echo "OK: Adapter no longer releases shared AudioEngine."
fi

echo ""
echo "===== 4. VERIFY AUDIO ENGINE IS CREATED ONCE ====="

grep -Rni --exclude-dir=build \
    "AudioEngine()" \
    app/src/main/java || true

echo ""
echo "===== 5. VERIFY MEDIA3 ADAPTER ====="

cat "$ADAPTER"

echo ""
echo "===== 6. VERIFY PLAYBACK SERVICE ====="

grep -n -A 8 -B 5 \
    "Media3AudioProcessorAdapter" \
    app/src/main/java/com/audiotune/studio/playback/PlaybackService.kt

echo ""
echo "===== 7. VERIFY DSP PROCESSING ORDER ====="

grep -n -A 10 \
    "EXPECTED_ORDER" \
    app/src/main/java/com/audiotune/studio/audio/dsp/DspPipeline.kt

echo ""
echo "===== 8. VERIFY DSP PIPELINE ====="

grep -Rni --exclude-dir=build \
    "dspPipeline" \
    app/src/main/java || true

echo ""
echo "=================================================="
echo " RUNNING CLEAN DEBUG BUILD"
echo "=================================================="

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew clean assembleDebug
else
    gradle clean assembleDebug
fi

echo ""
echo "=================================================="
echo " BUILD SUCCESSFUL"
echo "=================================================="

echo ""
echo "===== 9. APK ====="

find . \
    -type f \
    -path "*/build/outputs/apk/*.apk" \
    -print

echo ""
echo "===== 10. FINAL LIFECYCLE CHECK ====="

echo "--- Shared AudioEngine instances ---"
grep -Rni --exclude-dir=build \
    "AudioEngine()" \
    app/src/main/java || true

echo ""
echo "--- AudioEngine release calls ---"
grep -Rni --exclude-dir=build \
    "audioEngine.release" \
    app/src/main/java || true

echo ""
echo "--- Media3 adapter references ---"
grep -Rni --exclude-dir=build \
    "Media3AudioProcessorAdapter" \
    app/src/main/java || true

echo ""
echo "=================================================="
echo " STAGE 10 FINAL FIX COMPLETE"
echo "=================================================="

