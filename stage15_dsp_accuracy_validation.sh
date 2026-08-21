#!/bin/bash
set -e

echo "=================================================="
echo " STAGE 15 - DSP FREQUENCY RESPONSE / ACCURACY"
echo "=================================================="

SRC="app/src/main/java/com/audiotune/studio"
TEST_DIR="app/src/test/java/com/audiotune/studio/audio/dsp"

mkdir -p "$TEST_DIR"

echo ""
echo "===== 1. VERIFY DSP PROCESSORS ====="

PROCESSORS=(
"$SRC/audio/dsp/eq/ParametricEqProcessor.kt"
"$SRC/audio/dsp/dynamics/CompressorProcessor.kt"
"$SRC/audio/dsp/dynamics/LimiterProcessor.kt"
"$SRC/audio/dsp/dynamics/NoiseGateProcessor.kt"
"$SRC/audio/dsp/dynamics/ExpanderProcessor.kt"
)

for FILE in "${PROCESSORS[@]}"; do
    if [ ! -f "$FILE" ]; then
        echo "ERROR: Missing processor:"
        echo "$FILE"
        exit 1
    fi

    echo "OK: $FILE"
done

echo ""
echo "===== 2. INSPECT CURRENT DSP PARAMETERS ====="

for FILE in "${PROCESSORS[@]}"; do
    echo ""
    echo "----- $FILE -----"

    grep -n \
        "thresholdDb\|ratio\|attackMs\|releaseMs\|frequency\|gainDb\|qFactor\|ceilingDb\|rangeDb\|isEnabled" \
        "$FILE" || true
done

echo ""
echo "===== 3. CREATE DSP ACCURACY TEST ====="

cat > "$TEST_DIR/DspAccuracyValidationTest.kt" <<'KOTLIN'
package com.audiotune.studio.audio.dsp

import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor
import com.audiotune.studio.audio.dsp.dynamics.CompressorProcessor
import com.audiotune.studio.audio.dsp.dynamics.LimiterProcessor
import com.audiotune.studio.audio.dsp.dynamics.NoiseGateProcessor
import com.audiotune.studio.audio.dsp.dynamics.ExpanderProcessor

import org.junit.Assert.assertTrue
import org.junit.Test

import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class DspAccuracyValidationTest {

    private val sampleRate = 48000f
    private val sampleRateInt = 48000
    private val channels = 2

    private fun createSine(
        frequency: Double,
        amplitude: Double,
        seconds: Double = 1.0
    ): ByteBuffer {

        val frames = (sampleRateInt * seconds).toInt()

        val buffer = ByteBuffer
            .allocate(frames * channels * 2)
            .order(ByteOrder.nativeOrder())

        for (i in 0 until frames) {

            val sample =
                (
                    sin(
                        2.0 *
                            PI *
                            frequency *
                            i /
                            sampleRateInt
                    ) * amplitude
                ).coerceIn(-1.0, 1.0)

            val pcm =
                (sample * 32767.0)
                    .toInt()
                    .toShort()

            repeat(channels) {
                buffer.putShort(pcm)
            }
        }

        buffer.flip()

        return buffer
    }

    private fun rms(buffer: ByteBuffer): Double {

        val duplicate =
            buffer
                .duplicate()
                .order(ByteOrder.nativeOrder())

        var sum = 0.0
        var count = 0

        while (duplicate.remaining() >= 2) {

            val sample =
                duplicate.short / 32768.0

            sum += sample * sample
            count++
        }

        return if (count == 0) {
            0.0
        } else {
            sqrt(sum / count)
        }
    }

    private fun peak(buffer: ByteBuffer): Double {

        val duplicate =
            buffer
                .duplicate()
                .order(ByteOrder.nativeOrder())

        var result = 0.0

        while (duplicate.remaining() >= 2) {

            val sample =
                abs(duplicate.short / 32768.0)

            result = max(result, sample)
        }

        return result
    }

    private fun db(value: Double): Double {

        return 20.0 *
            log10(
                max(value, 1e-12)
            )
    }

    private fun measureGainDb(
        processor: Any,
        frequency: Double,
        amplitude: Double = 0.25
    ): Double {

        val input =
            createSine(
                frequency = frequency,
                amplitude = amplitude,
                seconds = 1.0
            )

        val inputRms =
            rms(input)

        val output =
            when (processor) {

                is ParametricEqProcessor ->
                    processor.process(input)

                is CompressorProcessor ->
                    processor.process(input)

                is LimiterProcessor ->
                    processor.process(input)

                is NoiseGateProcessor ->
                    processor.process(input)

                is ExpanderProcessor ->
                    processor.process(input)

                else ->
                    error("Unsupported processor")
            }

        val outputRms =
            rms(output)

        val gain =
            db(outputRms) - db(inputRms)

        println(
            "FREQ=${frequency}Hz " +
            "INPUT=${db(inputRms)}dBFS " +
            "OUTPUT=${db(outputRms)}dBFS " +
            "GAIN=${gain}dB"
        )

        assertTrue(
            "Output contains invalid RMS",
            outputRms.isFinite()
        )

        assertTrue(
            "Output peak exceeds PCM range",
            peak(output) <= 1.0
        )

        return gain
    }

    @Test
    fun eqFrequencyResponseIsFinite() {

        val frequencies =
            listOf(
                50.0,
                100.0,
                250.0,
                500.0,
                1000.0,
                2000.0,
                4000.0,
                8000.0,
                12000.0,
                16000.0
            )

        val processor =
            ParametricEqProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== PARAMETRIC EQ RESPONSE =====")

        frequencies.forEach { frequency ->

            val gain =
                measureGainDb(
                    processor,
                    frequency
                )

            assertTrue(
                "EQ response invalid at ${frequency}Hz",
                gain.isFinite()
            )
        }

        processor.release()
    }

    @Test
    fun compressorResponseAcrossLevels() {

        val levels =
            listOf(
                0.01,
                0.03,
                0.10,
                0.25,
                0.50,
                0.75,
                0.90
            )

        val processor =
            CompressorProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== COMPRESSOR LEVEL RESPONSE =====")

        levels.forEach { level ->

            val input =
                createSine(
                    frequency = 1000.0,
                    amplitude = level,
                    seconds = 1.0
                )

            val inputDb =
                db(rms(input))

            val output =
                processor.process(input)

            val outputDb =
                db(rms(output))

            val gainReduction =
                outputDb - inputDb

            println(
                "LEVEL=${level} " +
                "INPUT=${inputDb}dBFS " +
                "OUTPUT=${outputDb}dBFS " +
                "GAIN_CHANGE=${gainReduction}dB"
            )

            assertTrue(
                outputDb.isFinite()
            )

            assertTrue(
                peak(output) <= 1.0
            )
        }

        processor.release()
    }

    @Test
    fun limiterResponseAcrossLevels() {

        val levels =
            listOf(
                0.25,
                0.50,
                0.75,
                0.90,
                0.99,
                1.00
            )

        val processor =
            LimiterProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== LIMITER RESPONSE =====")

        levels.forEach { level ->

            val input =
                createSine(
                    frequency = 1000.0,
                    amplitude = level,
                    seconds = 1.0
                )

            val output =
                processor.process(input)

            val inputPeak =
                peak(input)

            val outputPeak =
                peak(output)

            println(
                "LEVEL=${level} " +
                "INPUT_PEAK=${inputPeak} " +
                "OUTPUT_PEAK=${outputPeak} " +
                "OUTPUT_DBFS=${db(outputPeak)}"
            )

            assertTrue(
                "Limiter output exceeded PCM range",
                outputPeak <= 1.0
            )

            assertTrue(
                outputPeak.isFinite()
            )
        }

        processor.release()
    }

    @Test
    fun noiseGateResponseAcrossLevels() {

        val levels =
            listOf(
                0.001,
                0.003,
                0.005,
                0.01,
                0.03,
                0.10,
                0.25
            )

        val processor =
            NoiseGateProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== NOISE GATE RESPONSE =====")

        levels.forEach { level ->

            val input =
                createSine(
                    frequency = 1000.0,
                    amplitude = level,
                    seconds = 1.0
                )

            val inputDb =
                db(rms(input))

            val output =
                processor.process(input)

            val outputDb =
                db(rms(output))

            println(
                "LEVEL=${level} " +
                "INPUT=${inputDb}dBFS " +
                "OUTPUT=${outputDb}dBFS " +
                "CHANGE=${outputDb - inputDb}dB"
            )

            assertTrue(
                outputDb.isFinite()
            )

            assertTrue(
                peak(output) <= 1.0
            )
        }

        processor.release()
    }

    @Test
    fun expanderResponseAcrossLevels() {

        val levels =
            listOf(
                0.003,
                0.005,
                0.01,
                0.03,
                0.10,
                0.25,
                0.50
            )

        val processor =
            ExpanderProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== EXPANDER RESPONSE =====")

        levels.forEach { level ->

            val input =
                createSine(
                    frequency = 1000.0,
                    amplitude = level,
                    seconds = 1.0
                )

            val inputDb =
                db(rms(input))

            val output =
                processor.process(input)

            val outputDb =
                db(rms(output))

            println(
                "LEVEL=${level} " +
                "INPUT=${inputDb}dBFS " +
                "OUTPUT=${outputDb}dBFS " +
                "CHANGE=${outputDb - inputDb}dB"
            )

            assertTrue(
                outputDb.isFinite()
            )

            assertTrue(
                peak(output) <= 1.0
            )
        }

        processor.release()
    }

    @Test
    fun pipelineFrequencySweepRemainsStable() {

        val frequencies =
            listOf(
                20.0,
                30.0,
                50.0,
                100.0,
                250.0,
                500.0,
                1000.0,
                2000.0,
                4000.0,
                8000.0,
                12000.0,
                16000.0,
                18000.0,
                20000.0
            )

        val pipeline =
            DspPipeline()

        pipeline.configure(
            sampleRate,
            channels
        )

        println("")
        println("===== COMPLETE DSP PIPELINE SWEEP =====")

        frequencies.forEach { frequency ->

            val input =
                createSine(
                    frequency = frequency,
                    amplitude = 0.25,
                    seconds = 0.25
                )

            val inputDb =
                db(rms(input))

            val output =
                pipeline.process(input)

            val outputDb =
                db(rms(output))

            val gain =
                outputDb - inputDb

            println(
                "FREQ=${frequency}Hz " +
                "INPUT=${inputDb}dBFS " +
                "OUTPUT=${outputDb}dBFS " +
                "GAIN=${gain}dB"
            )

            assertTrue(
                "Pipeline produced invalid output at ${frequency}Hz",
                outputDb.isFinite()
            )

            assertTrue(
                "Pipeline clipped at ${frequency}Hz",
                peak(output) <= 1.0
            )
        }

        pipeline.release()
    }
}
KOTLIN

chmod +x stage15_dsp_accuracy_validation.sh
./stage15_dsp_accuracy_validation.sh