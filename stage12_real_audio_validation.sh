#!/bin/bash
set -e

echo "=================================================="
echo " STAGE 12 - REAL AUDIO RUNTIME VALIDATION"
echo "=================================================="

SRC="app/src/main/java/com/audiotune/studio"

echo ""
echo "===== 1. VERIFY ALL DSP CONTROLLERS ====="

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
echo "===== 2. VERIFY CONTROLLER -> PROCESSOR CONNECTION ====="

grep -Rni --exclude-dir=build \
    "setProcessorEnabled\|isEnabled\|thresholdDb\|gainDb\|ratio\|attackMs\|releaseMs" \
    "$SRC/audio" || true

echo ""
echo "===== 3. VERIFY EQ PARAMETER FLOW ====="

grep -Rni --exclude-dir=build \
    "eqController\|ParametricEqProcessor\|gainDb\|frequency\|qFactor" \
    "$SRC" || true

echo ""
echo "===== 4. VERIFY COMPRESSOR PARAMETER FLOW ====="

grep -Rni --exclude-dir=build \
    "compressorController\|CompressorProcessor\|thresholdDb\|ratio\|attackMs\|releaseMs" \
    "$SRC" || true

echo ""
echo "===== 5. VERIFY LIMITER PARAMETER FLOW ====="

grep -Rni --exclude-dir=build \
    "limiterController\|LimiterProcessor\|thresholdDb\|ceiling\|releaseMs" \
    "$SRC" || true

echo ""
echo "===== 6. VERIFY NOISE GATE PARAMETER FLOW ====="

grep -Rni --exclude-dir=build \
    "noiseGateController\|NoiseGateProcessor\|thresholdDb\|holdMs\|rangeDb" \
    "$SRC" || true

echo ""
echo "===== 7. VERIFY EXPANDER PARAMETER FLOW ====="

grep -Rni --exclude-dir=build \
    "expanderController\|ExpanderProcessor\|thresholdDb\|ratio\|rangeDb" \
    "$SRC" || true

echo ""
echo "===== 8. VERIFY DSP PIPELINE IS PROCESSED ON EVERY AUDIO BUFFER ====="

grep -Rni --exclude-dir=build \
    "processAudio(buffer)\|dspPipeline.process\|processor.process" \
    "$SRC/audio" "$SRC/playback" || true

echo ""
echo "===== 9. VERIFY MEDIA3 QUEUE INPUT ====="

grep -n -A 30 \
    "override fun queueInput" \
    "$SRC/audio/engine/Media3AudioProcessorAdapter.kt"

echo ""
echo "===== 10. VERIFY PCM FORMAT ====="

grep -Rni --exclude-dir=build \
    "ENCODING_PCM_16BIT\|ByteOrder.nativeOrder\|getShort\|putShort" \
    "$SRC/audio" || true

echo ""
echo "===== 11. VERIFY BUFFER POSITION HANDLING ====="

grep -Rni --exclude-dir=build \
    "position()\|remaining()\|flip()\|clear()" \
    "$SRC/audio/engine" "$SRC/audio/dsp" || true

echo ""
echo "===== 12. CHECK FOR PROCESSING ALLOCATION ====="

echo "Searching DSP realtime path for allocation-heavy operations..."

grep -Rni --exclude-dir=build \
    "ByteBuffer.allocate\|ByteBuffer.allocateDirect\|FloatArray(" \
    "$SRC/audio/dsp" "$SRC/audio/engine" || true

echo ""
echo "NOTE:"
echo "Allocations inside configure/reset are acceptable."
echo "Allocations inside process() may cause realtime audio glitches."

echo ""
echo "===== 13. VERIFY PROCESSOR BYPASS ====="

for FILE in \
    "$SRC/audio/dsp/eq/ParametricEqProcessor.kt" \
    "$SRC/audio/dsp/dynamics/CompressorProcessor.kt" \
    "$SRC/audio/dsp/dynamics/LimiterProcessor.kt" \
    "$SRC/audio/dsp/dynamics/NoiseGateProcessor.kt" \
    "$SRC/audio/dsp/dynamics/ExpanderProcessor.kt"
do
    echo ""
    echo "--- $FILE ---"
    grep -n \
        "if (!isEnabled) return inputBuffer" \
        "$FILE" || true
done

echo ""
echo "===== 14. VERIFY PLAYBACK SERVICE ====="

cat "$SRC/playback/PlaybackService.kt"

echo ""
echo "===== 15. VERIFY AUDIO ENGINE LIFECYCLE ====="

grep -Rni --exclude-dir=build \
    "AudioEngine()\|audioEngine.flush\|audioEngine.release" \
    "$SRC" || true

echo ""
echo "===== 16. VERIFY TRACK CHANGE SAFETY ====="

grep -Rni --exclude-dir=build \
    "setMediaItem\|setMediaItems\|clearMediaItems\|prepare\|stop\|release" \
    "$SRC/playback" || true

echo ""
echo "===== 17. RUN UNIT TESTS ====="

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew test
else
    gradle test
fi

echo ""
echo "===== 18. RUN CLEAN BUILD ====="

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
echo " STAGE 12 STATIC PRE-RUNTIME VALIDATION COMPLETE"
echo "=================================================="

echo ""
echo "IMPORTANT:"
echo ""
echo "Static validation cannot prove that the DSP"
echo "actually changes audible audio."
echo ""
echo "The APK MUST now be installed on a physical"
echo "Android device or emulator."
echo ""
echo "=================================================="
echo " REQUIRED REAL AUDIO TESTS"
echo "=================================================="

echo ""
echo "TEST 1 - CLEAN BYPASS"
echo "All DSP modules OFF."
echo "Play reference audio."
echo "Confirm audio is clean and unchanged."

echo ""
echo "TEST 2 - EQ"
echo "Enable EQ."
echo "Set one band to approximately +12 dB."
echo "Sweep frequency."
echo "Confirm audible tonal change."

echo ""
echo "TEST 3 - EQ CUT"
echo "Set the same band to approximately -12 dB."
echo "Confirm the same frequency is audibly reduced."

echo ""
echo "TEST 4 - COMPRESSOR"
echo "Use a high input level."
echo "Enable compressor."
echo "Use an aggressive ratio."
echo "Confirm dynamic range is reduced."

echo ""
echo "TEST 5 - LIMITER"
echo "Push the source level high."
echo "Enable limiter."
echo "Confirm output does not produce obvious digital clipping."

echo ""
echo "TEST 6 - NOISE GATE"
echo "Use low-level background noise."
echo "Enable gate."
echo "Confirm low-level signal is attenuated."

echo ""
echo "TEST 7 - EXPANDER"
echo "Enable expander."
echo "Confirm low-level signal is progressively reduced."

echo ""
echo "TEST 8 - BYPASS COMPARISON"
echo "Toggle each processor ON/OFF."
echo "Confirm audible difference occurs."

echo ""
echo "TEST 9 - TRACK CHANGE"
echo "Play track A."
echo "Change to track B."
echo "Confirm DSP remains active."

echo ""
echo "TEST 10 - PAUSE / RESUME"
echo "Pause playback."
echo "Resume playback."
echo "Confirm DSP continues processing."

echo ""
echo "TEST 11 - RAPID TRACK CHANGE"
echo "Change tracks repeatedly."
echo "Confirm no crash, silence, distortion, or DSP failure."

echo ""
echo "TEST 12 - UI PARAMETER TEST"
echo "Change DSP parameters from the UI."
echo "Confirm changes are immediately audible."

echo ""
echo "=================================================="
echo " STAGE 12 COMPLETE"
echo "=================================================="
