package com.audiotune.studio.audio.dsp

import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.dsp.eq.EqFilterType
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
import kotlin.math.pow

class DspReferenceAccuracyTest {

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
                (sin(2.0 * PI * frequency * i / sampleRateInt) * amplitude)
                    .coerceIn(-1.0, 1.0)
            val pcm = (sample * 32767.0).toInt().toShort()
            repeat(channels) {
                buffer.putShort(pcm)
            }
        }
        buffer.flip()
        return buffer
    }
    
    private fun createTransientBuffer(seconds: Double, peakLevel: Double): ByteBuffer {
        val frames = (sampleRateInt * seconds).toInt()
        val buffer = ByteBuffer
            .allocate(frames * channels * 2)
            .order(ByteOrder.nativeOrder())
        
        for (i in 0 until frames) {
            var sample = 0.0
            if (i == frames / 2) {
                sample = peakLevel
            }
            val pcm = (sample * 32767.0).toInt().toShort()
            repeat(channels) {
                buffer.putShort(pcm)
            }
        }
        buffer.flip()
        return buffer
    }

    private fun rms(buffer: ByteBuffer, skipFrames: Int = 0): Double {
        val duplicate = buffer.duplicate().order(ByteOrder.nativeOrder())
        // Skip some frames to allow envelope to settle
        duplicate.position(skipFrames * channels * 2)
        var sum = 0.0
        var count = 0
        while (duplicate.remaining() >= 2) {
            val sample = duplicate.short / 32768.0
            sum += sample * sample
            count++
        }
        return if (count == 0) 0.0 else sqrt(sum / count)
    }

    private fun peak(buffer: ByteBuffer): Double {
        val duplicate = buffer.duplicate().order(ByteOrder.nativeOrder())
        var result = 0.0
        while (duplicate.remaining() >= 2) {
            val sample = abs(duplicate.short / 32768.0)
            result = max(result, sample)
        }
        return result
    }

    private fun db(value: Double): Double {
        return 20.0 * log10(max(value, 1e-12))
    }

    @Test
    fun eqReferenceTest_Unity() {
        val processor = ParametricEqProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        // Default is 0 gain
        val freqs = listOf(100.0, 1000.0, 8000.0)
        for (f in freqs) {
            val input = createSine(f, 0.25)
            val inRms = db(rms(input))
            val outRms = db(rms(processor.process(input)))
            val gain = outRms - inRms
            println("EQ UNITY $f Hz: INPUT=$inRms OUTPUT=$outRms GAIN=$gain EXPECTED=0.0")
            assertTrue("Unity EQ failed at $f", abs(gain) < 0.5)
        }
        processor.release()
    }
    
    @Test
    fun eqReferenceTest_Boost() {
        val processor = ParametricEqProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.updateBand(5, EqBand(frequencyHz = 1000f, gainDb = 6f, q = 1f, type = EqFilterType.PEAKING))
        
        val input = createSine(1000.0, 0.25)
        val inRms = db(rms(input))
        val outRms = db(rms(processor.process(input)))
        val gain = outRms - inRms
        println("EQ BOOST 1000 Hz: INPUT=$inRms OUTPUT=$outRms GAIN=$gain EXPECTED=6.0")
        assertTrue("Boost EQ failed", abs(gain - 6.0) < 0.5)
        processor.release()
    }

    @Test
    fun eqReferenceTest_Cut() {
        val processor = ParametricEqProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.updateBand(5, EqBand(frequencyHz = 1000f, gainDb = -6f, q = 1f, type = EqFilterType.PEAKING))
        
        val input = createSine(1000.0, 0.25)
        val inRms = db(rms(input))
        val outRms = db(rms(processor.process(input)))
        val gain = outRms - inRms
        println("EQ CUT 1000 Hz: INPUT=$inRms OUTPUT=$outRms GAIN=$gain EXPECTED=-6.0")
        assertTrue("Cut EQ failed", abs(gain - -6.0) < 0.5)
        processor.release()
    }
    
    @Test
    fun eqReferenceTest_Selectivity() {
        val processor = ParametricEqProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.updateBand(5, EqBand(frequencyHz = 1000f, gainDb = 6f, q = 1f, type = EqFilterType.PEAKING))
        
        val freqs = listOf(100.0, 500.0, 1000.0, 2000.0, 8000.0)
        val gains = freqs.map { f ->
            val input = createSine(f, 0.25)
            val inRms = db(rms(input))
            val outRms = db(rms(processor.process(input)))
            outRms - inRms
        }
        
        val maxGain = gains.maxOrNull() ?: 0.0
        val gainAt1k = gains[2]
        
        println("EQ SELECTIVITY GAINS: $gains")
        assertTrue("1000 Hz should have max gain", abs(maxGain - gainAt1k) < 0.1)
        assertTrue("Selectivity failed, 100 Hz has too much gain", gains[0] < gainAt1k - 4.0)
        processor.release()
    }

    @Test
    fun compressorReferenceTest_GainReduction() {
        val processor = CompressorProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.thresholdDb = -12f
        processor.ratio = 4f
        processor.makeupGainDb = 0f
        processor.attackMs = 0.1f 
        processor.releaseMs = 100f
        
        val testLevels = listOf(-24.0, -18.0, -12.0, -6.0, 0.0)
        for (levelDb in testLevels) {
            processor.flush() // Reset envelope
            val amplitude = 10.0.pow(levelDb / 20.0)
            val input = createSine(1000.0, amplitude, 1.0)
            
            // Skip first 0.5s to allow compressor to settle
            val skipFrames = (sampleRateInt * 0.5).toInt()
            
            val inRms = db(rms(input, skipFrames))
            val out = processor.process(input)
            val outRms = db(rms(out, skipFrames))
            
            val expectedOut = if (levelDb > -12.0) {
                inRms - ((levelDb - -12.0) * (1.0 - 1.0 / 4.0))
            } else {
                inRms
            }
            val error = abs(outRms - expectedOut)
            
            println("COMPRESSOR LEVEL=$levelDb INPUT=$inRms EXPECTED=$expectedOut OUTPUT=$outRms ERROR=$error")
            assertTrue("Compressor failed at level $levelDb with error $error", error < 1.0)
        }
        processor.release()
    }

    @Test
    fun limiterReferenceTest_Transient() {
        val processor = LimiterProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.ceilingDb = -1.0f
        val ceilingLinear = 10.0.pow(-1.0 / 20.0)
        
        // Test 1: Below ceiling
        processor.flush()
        val belowInput = createTransientBuffer(0.5, ceilingLinear * 0.5)
        val belowPeak = peak(belowInput)
        val belowOut = processor.process(belowInput)
        val belowOutPeak = peak(belowOut)
        println("LIMITER BELOW: IN_PEAK=$belowPeak OUT_PEAK=$belowOutPeak CEILING=$ceilingLinear ERROR=${abs(belowOutPeak - belowPeak)}")
        assertTrue("Limiter attenuated below ceiling", abs(belowOutPeak - belowPeak) < 0.05)
        
        // Test 2: Above ceiling
        processor.flush()
        val aboveInputValid = createTransientBuffer(0.5, 1.0)
        val abovePeak = peak(aboveInputValid)
        val aboveOut = processor.process(aboveInputValid)
        val aboveOutPeak = peak(aboveOut)
        println("LIMITER ABOVE: IN_PEAK=$abovePeak OUT_PEAK=$aboveOutPeak CEILING=$ceilingLinear ERROR=${abs(aboveOutPeak - ceilingLinear)}")
        assertTrue("Limiter failed to cap above ceiling", aboveOutPeak <= ceilingLinear + 0.05)
        
        processor.release()
    }
    
    @Test
    fun noiseGateReferenceTest() {
        val processor = NoiseGateProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.thresholdDb = -40f
        processor.rangeDb = -80f
        processor.attackMs = 0.1f
        processor.releaseMs = 100f
        processor.holdMs = 0f
        
        val levels = listOf(-50.0, -42.0, -38.0, -20.0)
        for (levelDb in levels) {
            processor.flush()
            val amplitude = 10.0.pow(levelDb / 20.0)
            val input = createSine(1000.0, amplitude, 1.0)
            val skipFrames = (sampleRateInt * 0.5).toInt()
            
            val inRms = db(rms(input, skipFrames))
            val out = processor.process(input)
            val outRms = db(rms(out, skipFrames))
            val gainChange = outRms - inRms
            
            println("GATE LEVEL=$levelDb INPUT=$inRms OUTPUT=$outRms GAIN_CHANGE=$gainChange THRESHOLD=-40 RANGE=-80")
            
            if (levelDb < -40.0) {
                // Below threshold
                assertTrue("Gate should attenuate below threshold", gainChange < -20.0)
            } else {
                // Above threshold
                assertTrue("Gate should pass signal above threshold", abs(gainChange) < 1.0)
            }
        }
        processor.release()
    }

    @Test
    fun expanderReferenceTest() {
        val processor = ExpanderProcessor()
        processor.configure(sampleRate, channels)
        processor.isEnabled = true
        processor.thresholdDb = -40f
        processor.ratio = 2f
        processor.attackMs = 0.1f
        processor.releaseMs = 100f
        
        val levels = listOf(-60.0, -50.0, -42.0, -38.0, -20.0)
        for (levelDb in levels) {
            processor.flush()
            val amplitude = 10.0.pow(levelDb / 20.0)
            val input = createSine(1000.0, amplitude, 1.0)
            val skipFrames = (sampleRateInt * 0.5).toInt()
            
            val inRms = db(rms(input, skipFrames))
            val out = processor.process(input)
            val outRms = db(rms(out, skipFrames))
            val gainChange = outRms - inRms
            
            println("EXPANDER LEVEL=$levelDb INPUT=$inRms OUTPUT=$outRms GAIN_CHANGE=$gainChange")
            
            if (levelDb < -40.0) {
                val expectedGainChange = (levelDb - -40.0) * (2f - 1f)
                val expectedOutput = inRms + expectedGainChange
                assertTrue("Expander should attenuate properly below threshold. Expected: $expectedOutput, Actual: $outRms", abs(outRms - expectedOutput) < 2.0)
            } else {
                assertTrue("Expander should pass signal above threshold", abs(gainChange) < 1.0)
            }
        }
        processor.release()
    }
}
