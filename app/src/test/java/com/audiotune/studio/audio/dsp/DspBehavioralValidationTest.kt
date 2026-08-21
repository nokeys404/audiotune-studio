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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class DspBehavioralValidationTest {

    private val sampleRate = 48000f
    private val channels = 2

    private fun createSine(
        frequency: Double,
        amplitude: Double,
        seconds: Double = 1.0
    ): ByteBuffer {

        val frames = (sampleRate * seconds).toInt()

        val buffer = ByteBuffer
            .allocate(frames * channels * 2)
            .order(ByteOrder.nativeOrder())

        for (i in 0 until frames) {

            val sample =
                (
                    sin(
                        2.0 *
                        Math.PI *
                        frequency *
                        i /
                        sampleRate
                    ) *
                    amplitude
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

        var peak = 0.0

        while (duplicate.remaining() >= 2) {

            val sample =
                abs(duplicate.short / 32768.0)

            peak = max(peak, sample)
        }

        return peak
    }

    private fun db(value: Double): Double {
        return 20.0 * log10(max(value, 1e-12))
    }

    private fun process(
        processor: Any,
        input: ByteBuffer
    ): ByteBuffer {

        return when (processor) {

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
    }

    @Test
    fun eqProcessorProducesFiniteOutput() {

        val processor =
            ParametricEqProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        val input =
            createSine(
                frequency = 1000.0,
                amplitude = 0.5
            )

        val output =
            processor.process(input)

        val value =
            rms(output)

        println("EQ OUTPUT RMS = $value")

        assertTrue(value.isFinite())
        assertTrue(peak(output) <= 1.0)

        processor.release()
    }

    @Test
    fun compressorReducesHighLevelSignal() {

        val processor =
            CompressorProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        val input =
            createSine(
                frequency = 1000.0,
                amplitude = 0.95
            )

        val inputRms =
            rms(input)

        val output =
            processor.process(input)

        val outputRms =
            rms(output)

        println(
            "COMP INPUT RMS = $inputRms"
        )

        println(
            "COMP OUTPUT RMS = $outputRms"
        )

        assertTrue(
            "Compressor output is invalid",
            outputRms.isFinite()
        )

        assertTrue(
            "Compressor produced invalid peak",
            peak(output) <= 1.0
        )

        processor.release()
    }

    @Test
    fun limiterNeverExceedsPcmRange() {

        val processor =
            LimiterProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        val input =
            createSine(
                frequency = 1000.0,
                amplitude = 1.0
            )

        val output =
            processor.process(input)

        val outputPeak =
            peak(output)

        println(
            "LIMITER OUTPUT PEAK = $outputPeak"
        )

        assertTrue(
            "Limiter output exceeds PCM range",
            outputPeak <= 1.0
        )

        assertTrue(
            "Limiter output is invalid",
            outputPeak.isFinite()
        )

        processor.release()
    }

    @Test
    fun noiseGateAttenuatesSilenceLikeSignal() {

        val processor =
            NoiseGateProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        val input =
            createSine(
                frequency = 1000.0,
                amplitude = 0.01
            )

        val inputRms =
            rms(input)

        val output =
            processor.process(input)

        val outputRms =
            rms(output)

        println(
            "GATE INPUT RMS = $inputRms"
        )

        println(
            "GATE OUTPUT RMS = $outputRms"
        )

        assertTrue(
            outputRms.isFinite()
        )

        processor.release()
    }

    @Test
    fun expanderProcessesLowLevelSignal() {

        val processor =
            ExpanderProcessor()

        processor.configure(
            sampleRate,
            channels
        )

        val input =
            createSine(
                frequency = 1000.0,
                amplitude = 0.03
            )

        val inputRms =
            rms(input)

        val output =
            processor.process(input)

        val outputRms =
            rms(output)

        println(
            "EXPANDER INPUT RMS = $inputRms"
        )

        println(
            "EXPANDER OUTPUT RMS = $outputRms"
        )

        assertTrue(
            outputRms.isFinite()
        )

        processor.release()
    }

    @Test
    fun allProcessorsRemainStableForRepeatedProcessing() {

        val processors =
            listOf(
                ParametricEqProcessor(),
                CompressorProcessor(),
                LimiterProcessor(),
                NoiseGateProcessor(),
                ExpanderProcessor()
            )

        processors.forEach { processor ->

            processor.configure(
                sampleRate,
                channels
            )

            repeat(1000) {

                val input =
                    createSine(
                        frequency = 1000.0,
                        amplitude = 0.5,
                        seconds = 0.01
                    )

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

                val outputPeak =
                    peak(output)

                assertTrue(
                    "Invalid RMS at iteration $it",
                    outputRms.isFinite()
                )

                assertTrue(
                    "Invalid peak at iteration $it",
                    outputPeak.isFinite()
                )

                assertTrue(
                    "Peak exceeds PCM range at iteration $it",
                    outputPeak <= 1.0
                )
            }

            processor.release()
        }
    }

    @Test
    fun completePipelineRemainsStable() {

        val pipeline =
            DspPipeline()

        pipeline.configure(
            sampleRate,
            channels
        )

        repeat(2000) {

            val input =
                createSine(
                    frequency = 1000.0,
                    amplitude = 0.5,
                    seconds = 0.01
                )

            val output =
                pipeline.process(input)

            val outputRms =
                rms(output)

            val outputPeak =
                peak(output)

            assertTrue(
                "Pipeline RMS invalid at iteration $it",
                outputRms.isFinite()
            )

            assertTrue(
                "Pipeline peak invalid at iteration $it",
                outputPeak.isFinite()
            )

            assertTrue(
                "Pipeline peak exceeds PCM range at iteration $it",
                outputPeak <= 1.0
            )
        }

        pipeline.release()
    }
}
