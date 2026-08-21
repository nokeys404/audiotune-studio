#!/bin/bash
set -e

echo "=================================================="
echo " STAGE 11 - RUNTIME DSP AUDIO VALIDATION"
echo "=================================================="

SRC="app/src/main/java/com/audiotune/studio"

echo ""
echo "===== 1. VERIFY DSP PROCESSOR IMPLEMENTATIONS ====="

PROCESSORS=(
    "$SRC/audio/dsp/eq/ParametricEqProcessor.kt"
    "$SRC/audio/dsp/dynamics/CompressorProcessor.kt"
    "$SRC/audio/dsp/dynamics/LimiterProcessor.kt"
    "$SRC/audio/dsp/dynamics/NoiseGateProcessor.kt"
    "$SRC/audio/dsp/dynamics/ExpanderProcessor.kt"
)

for FILE in "${PROCESSORS[@]}"; do
    echo ""
    echo "----- $FILE -----"

    if [ ! -f "$FILE" ]; then
        echo "ERROR: Missing processor: $FILE"
        exit 1
    fi

    grep -n "class " "$FILE" || true
    grep -n "override fun configure" "$FILE" || true
    grep -n "override fun process" "$FILE" || true
    grep -n "override fun flush" "$FILE" || true
done

echo ""
echo "===== 2. VERIFY DSP PROCESSING ORDER ====="

python3 - <<'PY'
from pathlib import Path

path = Path(
    "app/src/main/java/com/audiotune/studio/audio/dsp/DspPipeline.kt"
)

text = path.read_text()

expected = [
    "noise_gate",
    "expander",
    "parametric_eq",
    "compressor",
    "limiter",
]

missing = [x for x in expected if f'"{x}"' not in text]

if missing:
    print("ERROR: Missing DSP processors:", missing)
    raise SystemExit(1)

positions = [text.index(f'"{x}"') for x in expected]

if positions != sorted(positions):
    print("ERROR: DSP processing order is incorrect.")
    raise SystemExit(1)

print("DSP order verified:")
print(" -> ".join(expected))
PY

echo ""
echo "===== 3. VERIFY AUDIO ENGINE PIPELINE ====="

grep -n -A 35 "class AudioEngine" \
    "$SRC/audio/engine/AudioEngine.kt"

echo ""
echo "===== 4. VERIFY MEDIA3 AUDIO PROCESSOR ====="

grep -n -A 90 "class Media3AudioProcessorAdapter" \
    "$SRC/audio/engine/Media3AudioProcessorAdapter.kt"

echo ""
echo "===== 5. VERIFY SHARED AUDIO ENGINE ====="

COUNT=$(grep -Rni --exclude-dir=build \
    "AudioEngine()" \
    "$SRC" | wc -l)

echo "AudioEngine() occurrences: $COUNT"

if [ "$COUNT" -ne 1 ]; then
    echo "ERROR: Expected exactly one AudioEngine instance."
    exit 1
fi

echo "OK: Single AudioEngine instance."

echo ""
echo "===== 6. VERIFY NO SHARED ENGINE RELEASE ====="

if grep -Rni --exclude-dir=build \
    "audioEngine.release" \
    "$SRC"; then

    echo "ERROR: Shared AudioEngine release call detected."
    exit 1
else
    echo "OK: No shared AudioEngine release call."
fi

echo ""
echo "===== 7. VERIFY DSP CONTROLLERS ====="

for controller in \
    EqController \
    CompressorController \
    LimiterController \
    NoiseGateController \
    ExpanderController
do
    echo ""
    echo "--- $controller ---"

    grep -Rni --exclude-dir=build \
        "class $controller" \
        "$SRC" || true
done

echo ""
echo "===== 8. VERIFY EQUALIZER VIEWMODEL CONNECTION ====="

grep -n -A 20 -B 5 \
    "AppContainer.audioEngine" \
    "$SRC/presentation/equalizer/EqualizerViewModel.kt" || true

echo ""
echo "===== 9. CHECK PCM FORMAT SUPPORT ====="

grep -Rni --exclude-dir=build \
    "ENCODING_PCM_16BIT" \
    "$SRC/audio"

echo ""
echo "===== 10. CHECK SAMPLE RATE / CHANNEL CONFIGURATION ====="

grep -Rni --exclude-dir=build \
    "sampleRate" \
    "$SRC/audio/dsp" \
    "$SRC/audio/engine" | head -100

echo ""
echo "===== 11. CHECK FLOAT PROCESSING ====="

for FILE in "${PROCESSORS[@]}"; do
    echo ""
    echo "--- $FILE ---"

    grep -n \
        "FloatArray\|processFloat\|32768f\|putShort\|getShort" \
        "$FILE" || true
done

echo ""
echo "===== 12. CHECK BYPASS BEHAVIOR ====="

for FILE in "${PROCESSORS[@]}"; do
    echo ""
    echo "--- $FILE ---"

    grep -n \
        "if (!isEnabled) return inputBuffer" \
        "$FILE" || true
done

echo ""
echo "===== 13. CHECK MEDIA3 PLAYBACK INTEGRATION ====="

grep -n -A 25 -B 10 \
    "setAudioProcessors" \
    "$SRC/playback/PlaybackService.kt"

echo ""
echo "===== 14. CHECK PLAYBACK SERVICE AUDIO ATTRIBUTES ====="

grep -n -A 15 \
    "AudioAttributes.Builder" \
    "$SRC/playback/PlaybackService.kt"

echo ""
echo "===== 15. CHECK DSP STATE FLOW ====="

grep -n -A 15 \
    "PipelineState" \
    "$SRC/audio/dsp/DspPipeline.kt" || true

echo ""
echo "===== 16. SEARCH FOR TODO / FIXME IN AUDIO ENGINE ====="

grep -Rni --exclude-dir=build \
    "TODO\|FIXME\|NOT IMPLEMENTED\|IMPLEMENT ME" \
    "$SRC/audio" || true

echo ""
echo "===== 17. RUN UNIT TESTS ====="

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew test
else
    gradle test
fi

echo ""
echo "===== 18. RUN CLEAN DEBUG BUILD ====="

if [ -f "./gradlew" ]; then
    ./gradlew clean assembleDebug
else
    gradle clean assembleDebug
fi

echo ""
echo "===== 19. FIND APK ====="

find . \
    -type f \
    -path "*/build/outputs/apk/*.apk" \
    -print

echo ""
echo "=================================================="
echo " STAGE 11 STATIC DSP VALIDATION COMPLETE"
echo "=================================================="

echo ""
echo "IMPORTANT:"
echo "This stage validates DSP wiring, processing order,"
echo "PCM processing, controllers, Media3 integration,"
echo "unit tests, and APK compilation."
echo ""
echo "It does NOT claim that audio quality has been"
echo "verified until the APK is tested with real audio."
echo ""

echo "=================================================="
echo " NEXT: INSTALL APK AND PERFORM REAL AUDIO TEST"
echo "=================================================="

