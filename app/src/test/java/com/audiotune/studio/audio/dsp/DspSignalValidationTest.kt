package com.audiotune.studio.audio.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin

class DspSignalValidationTest {

    private val sampleRateFloat = 48000f
    private val sampleRate = 48000
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
                (sin(2.0 * Math.PI * frequency * i / sampleRate) * amplitude)
                    .coerceIn(-1.0, 1.0)

            val pcm = (sample * 32767.0).toInt().toShort()

            repeat(channels) {
                buffer.putShort(pcm)
            }
        }

        buffer.flip()
        return buffer
    }

    private fun rms(buffer: ByteBuffer): Double {

        val duplicate = buffer.duplicate()
            .order(ByteOrder.nativeOrder())

        var sum = 0.0
        var count = 0

        while (duplicate.remaining() >= 2) {
            val sample = duplicate.short / 32768.0
            sum += sample * sample
            count++
        }

        return if (count == 0) {
            0.0
        } else {
            kotlin.math.sqrt(sum / count)
        }
    }

    private fun peak(buffer: ByteBuffer): Double {

        val duplicate = buffer.duplicate()
            .order(ByteOrder.nativeOrder())

        var peak = 0.0

        while (duplicate.remaining() >= 2) {
            val sample = abs(duplicate.short / 32768.0)
            if (sample > peak) {
                peak = sample
            }
        }

        return peak
    }

    @Test
    fun dspPipelineCanProcessAudio() {

        val pipeline = DspPipeline()

        pipeline.configure(
            sampleRateFloat,
            channels
        )

        val input = createSine(
            frequency = 1000.0,
            amplitude = 0.5
        )

        val inputRms = rms(input)

        val output = pipeline.process(input)

        val outputRms = rms(output)

        println("INPUT RMS  = $inputRms")
        println("OUTPUT RMS = $outputRms")

        assertTrue(
            "DSP pipeline produced invalid output",
            outputRms.isFinite()
        )

        pipeline.release()
    }

    @Test
    fun pipelineDoesNotProduceInvalidSamples() {

        val pipeline = DspPipeline()

        pipeline.configure(
            sampleRateFloat,
            channels
        )

        val input = createSine(
            frequency = 1000.0,
            amplitude = 0.8
        )

        val output = pipeline.process(input)

        val outputPeak = peak(output)

        println("OUTPUT PEAK = $outputPeak")

        assertTrue(
            "Output peak exceeds PCM range",
            outputPeak <= 1.0
        )

        pipeline.release()
    }

    @Test
    fun repeatedProcessingDoesNotBreakPipeline() {

        val pipeline = DspPipeline()

        pipeline.configure(
            sampleRateFloat,
            channels
        )

        repeat(100) {

            val input = createSine(
                frequency = 1000.0,
                amplitude = 0.5,
                seconds = 0.02
            )

            val output = pipeline.process(input)

            val rmsValue = rms(output)

            assertTrue(
                "Invalid RMS at iteration $it",
                rmsValue.isFinite()
            )
        }

        pipeline.release()
    }

    @Test
    fun silenceDoesNotProduceNaN() {

        val pipeline = DspPipeline()

        pipeline.configure(
            sampleRateFloat,
            channels
        )

        val buffer = ByteBuffer
            .allocate(sampleRate * channels * 2 / 10)
            .order(ByteOrder.nativeOrder())

        repeat(
            sampleRate * channels / 10
        ) {
            buffer.putShort(0)
        }

        buffer.flip()

        val output = pipeline.process(buffer)

        val outputRms = rms(output)

        println("SILENCE OUTPUT RMS = $outputRms")

        assertTrue(
            "Silence produced invalid output",
            outputRms.isFinite()
        )

        pipeline.release()
    }
}
