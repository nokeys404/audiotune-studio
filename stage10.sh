#!/bin/bash
set -e

echo "=================================================="
echo " STAGE 10 - FINAL AUDIO ENGINE VALIDATION"
echo "=================================================="

echo ""
echo "===== 1. CHECK AUDIOENGINE INSTANTIATION ====="
grep -Rni --exclude-dir=build "AudioEngine()" app/src/main/java || true

echo ""
echo "===== 2. CHECK MEDIA3 ADAPTER ====="
cat app/src/main/java/com/audiotune/studio/audio/engine/Media3AudioProcessorAdapter.kt

echo ""
echo "===== 3. CHECK AUDIO ENGINE ====="
cat app/src/main/java/com/audiotune/studio/audio/engine/AudioEngine.kt

echo ""
echo "===== 4. CHECK DSP PIPELINE ====="
cat app/src/main/java/com/audiotune/studio/audio/dsp/DspPipeline.kt

echo ""
echo "===== 5. CHECK PLAYBACK SERVICE ====="
cat app/src/main/java/com/audiotune/studio/playback/PlaybackService.kt

echo ""
echo "===== 6. CHECK AUDIO ENGINE RELEASE ====="
grep -Rni --exclude-dir=build "audioEngine.release" app/src/main/java || true

echo ""
echo "===== 7. CHECK DSP PROCESSORS ====="

for f in \
app/src/main/java/com/audiotune/studio/audio/dsp/eq/ParametricEqProcessor.kt \
app/src/main/java/com/audiotune/studio/audio/dsp/dynamics/CompressorProcessor.kt \
app/src/main/java/com/audiotune/studio/audio/dsp/dynamics/LimiterProcessor.kt \
app/src/main/java/com/audiotune/studio/audio/dsp/dynamics/NoiseGateProcessor.kt \
app/src/main/java/com/audiotune/studio/audio/dsp/dynamics/ExpanderProcessor.kt
do
    echo ""
    echo "----- $f -----"
    grep -n -A 35 "override fun process" "$f" || true
done

echo ""
echo "===== 8. CHECK DSP PROCESSING ORDER ====="
grep -n -A 10 "EXPECTED_ORDER" \
app/src/main/java/com/audiotune/studio/audio/dsp/DspPipeline.kt || true

echo ""
echo "===== 9. CHECK ALL AUDIO ENGINE REFERENCES ====="
grep -Rni --exclude-dir=build "audioEngine" app/src/main/java || true

echo ""
echo "===== 10. CHECK MEDIA3 ADAPTER REFERENCES ====="
grep -Rni --exclude-dir=build "Media3AudioProcessorAdapter" app/src/main/java || true

echo ""
echo "===== 11. CHECK DSP PIPELINE REFERENCES ====="
grep -Rni --exclude-dir=build "dspPipeline" app/src/main/java || true

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
echo " BUILD COMPLETED"
echo "=================================================="

echo ""
echo "===== 12. FIND DEBUG APK ====="

find . \
    -type f \
    -path "*/build/outputs/apk/*.apk" \
    -print

echo ""
echo "===== 13. FINAL AUDIO ENGINE CHECK ====="

echo ""
echo "--- AudioEngine instances ---"
grep -Rni --exclude-dir=build "AudioEngine()" app/src/main/java || true

echo ""
echo "--- AudioEngine release calls ---"
grep -Rni --exclude-dir=build "audioEngine.release" app/src/main/java || true

echo ""
echo "--- Media3 adapter references ---"
grep -Rni --exclude-dir=build "Media3AudioProcessorAdapter" app/src/main/java || true

echo ""
echo "--- DSP pipeline references ---"
grep -Rni --exclude-dir=build "dspPipeline" app/src/main/java || true

echo ""
echo "=================================================="
echo " STAGE 10 COMPLETE"
echo "=================================================="
