package com.audiotune.studio.audio.dsp.eq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class ParametricEqProcessorTest {

    private fun generateSine(freq: Float, sampleRate: Float, length: Int): FloatArray {
        val arr = FloatArray(length)
        val w = 2.0 * PI * freq / sampleRate
        for (i in 0 until length) {
            arr[i] = sin(w * i).toFloat()
        }
        return arr
    }

    private fun computeRms(arr: FloatArray): Float {
        var sum = 0f
        for (x in arr) {
            sum += x * x
        }
        return sqrt(sum / arr.size)
    }

    @Test
    fun testZeroDbGainProducesUnchangedSignal() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        val input = generateSine(1000f, 48000f, 4800)
        val output = input.clone()
        processor.processFloat(output, output.size)
        
        val inputRms = computeRms(input)
        val outputRms = computeRms(output)
        
        assertEquals(inputRms, outputRms, 0.001f)
    }

    @Test
    fun testDisabledBandProducesUnchangedSignal() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        // 1kHz is band index 5
        processor.updateBand(5, EqBand(1000f, gainDb = 10f, isEnabled = false))
        
        val input = generateSine(1000f, 48000f, 4800)
        val output = input.clone()
        processor.processFloat(output, output.size)
        
        val inputRms = computeRms(input)
        val outputRms = computeRms(output)
        
        assertEquals(inputRms, outputRms, 0.001f)
    }

    @Test
    fun testPositiveGainChangesFrequencyRegion() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        processor.updateBand(5, EqBand(1000f, gainDb = 6f, type = EqFilterType.PEAKING))
        
        val input = generateSine(1000f, 48000f, 4800)
        val output = input.clone()
        processor.processFloat(output, output.size)
        
        val inputRms = computeRms(input)
        val outputRms = computeRms(output)
        
        // 6dB gain means RMS should approximately double (amplitude factor ~1.995)
        assertTrue("Output RMS $outputRms should be greater than Input RMS $inputRms", outputRms > inputRms * 1.5f)
    }

    @Test
    fun testNegativeGainReducesFrequencyRegion() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        processor.updateBand(5, EqBand(1000f, gainDb = -6f, type = EqFilterType.PEAKING))
        
        val input = generateSine(1000f, 48000f, 4800)
        val output = input.clone()
        processor.processFloat(output, output.size)
        
        val inputRms = computeRms(input)
        val outputRms = computeRms(output)
        
        assertTrue("Output RMS $outputRms should be less than Input RMS $inputRms", outputRms < inputRms * 0.75f)
    }

    @Test
    fun testDifferentQValuesAffectBandwidth() {
        val processor1 = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        processor1.updateBand(5, EqBand(1000f, gainDb = 10f, q = 0.5f, type = EqFilterType.PEAKING))
        
        val processor2 = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        processor2.updateBand(5, EqBand(1000f, gainDb = 10f, q = 10f, type = EqFilterType.PEAKING))
        
        // Test with a frequency slightly off-center, e.g., 800Hz
        val input = generateSine(800f, 48000f, 4800)
        
        val out1 = input.clone()
        processor1.processFloat(out1, out1.size)
        
        val out2 = input.clone()
        processor2.processFloat(out2, out2.size)
        
        val rms1 = computeRms(out1)
        val rms2 = computeRms(out2)
        
        // Lower Q = wider bandwidth, so 800Hz should be boosted more with Q=0.5 than Q=10
        assertTrue("RMS1 $rms1 should be > RMS2 $rms2", rms1 > rms2)
    }

    @Test
    fun testLowShelfWorks() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        // 125Hz is band index 2
        processor.updateBand(2, EqBand(125f, gainDb = 6f, type = EqFilterType.LOW_SHELF))
        
        // 50Hz should be boosted
        val inLow = generateSine(50f, 48000f, 4800)
        val outLow = inLow.clone()
        processor.processFloat(outLow, outLow.size)
        assertTrue(computeRms(outLow) > computeRms(inLow) * 1.5f)
        
        // 1000Hz should be unaffected
        val inHigh = generateSine(1000f, 48000f, 4800)
        val outHigh = inHigh.clone()
        processor.processFloat(outHigh, outHigh.size)
        assertEquals(computeRms(inHigh), computeRms(outHigh), 0.1f)
    }

    @Test
    fun testHighShelfWorks() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        // 4kHz is band index 7
        processor.updateBand(7, EqBand(4000f, gainDb = 6f, type = EqFilterType.HIGH_SHELF))
        
        // 8000Hz should be boosted
        val inHigh = generateSine(8000f, 48000f, 4800)
        val outHigh = inHigh.clone()
        processor.processFloat(outHigh, outHigh.size)
        assertTrue(computeRms(outHigh) > computeRms(inHigh) * 1.5f)
        
        // 1000Hz should be unaffected
        val inLow = generateSine(1000f, 48000f, 4800)
        val outLow = inLow.clone()
        processor.processFloat(outLow, outLow.size)
        assertEquals(computeRms(inLow), computeRms(outLow), 0.1f)
    }

    @Test
    fun testMultipleBandsWorkSequentially() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        // 125Hz (idx 2) +6dB
        processor.updateBand(2, EqBand(125f, gainDb = 6f, type = EqFilterType.PEAKING))
        // 4000Hz (idx 7) +6dB
        processor.updateBand(7, EqBand(4000f, gainDb = 6f, type = EqFilterType.PEAKING))
        
        val in125 = generateSine(125f, 48000f, 4800)
        val out125 = in125.clone()
        processor.processFloat(out125, out125.size)
        assertTrue(computeRms(out125) > computeRms(in125) * 1.5f)
        
        val in4000 = generateSine(4000f, 48000f, 4800)
        val out4000 = in4000.clone()
        processor.processFloat(out4000, out4000.size)
        assertTrue(computeRms(out4000) > computeRms(in4000) * 1.5f)
        
        val in1000 = generateSine(1000f, 48000f, 4800)
        val out1000 = in1000.clone()
        processor.processFloat(out1000, out1000.size)
        assertEquals(computeRms(in1000), computeRms(out1000), 0.1f)
    }

    @Test
    fun testFilterStatePersistsBetweenBlocks() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        processor.updateBand(5, EqBand(1000f, gainDb = 10f, type = EqFilterType.PEAKING))
        
        val fullInput = generateSine(1000f, 48000f, 4800)
        val fullOutput = fullInput.clone()
        processor.processFloat(fullOutput, fullOutput.size)
        
        // Reset state and process in blocks
        val blockProcessor = ParametricEqProcessor(sampleRate = 48000f, channels = 1)
        blockProcessor.updateBand(5, EqBand(1000f, gainDb = 10f, type = EqFilterType.PEAKING))
        
        val blockOutput = fullInput.clone()
        val blockSize = 1024
        for (i in 0 until blockOutput.size step blockSize) {
            val length = minOf(blockSize, blockOutput.size - i)
            val tmp = FloatArray(length)
            System.arraycopy(blockOutput, i, tmp, 0, length)
            blockProcessor.processFloat(tmp, length)
            System.arraycopy(tmp, 0, blockOutput, i, length)
        }
        
        // Compare fullOutput with blockOutput
        for (i in fullOutput.indices) {
            assertEquals("Mismatch at $i", fullOutput[i], blockOutput[i], 0.001f)
        }
    }

    @Test
    fun testStereoChannelsProcessedIndependently() {
        val processor = ParametricEqProcessor(sampleRate = 48000f, channels = 2)
        processor.updateBand(5, EqBand(1000f, gainDb = 6f, type = EqFilterType.PEAKING))
        
        // Stereo buffer: Left channel is 1000Hz, Right channel is 0 (silence)
        val length = 4800
        val input = FloatArray(length * 2)
        val w = 2.0 * PI * 1000f / 48000f
        for (i in 0 until length) {
            input[i * 2] = sin(w * i).toFloat()
            input[i * 2 + 1] = 0f
        }
        
        val output = input.clone()
        processor.processFloat(output, output.size)
        
        // Check Left channel is boosted
        val leftIn = FloatArray(length) { input[it * 2] }
        val leftOut = FloatArray(length) { output[it * 2] }
        assertTrue(computeRms(leftOut) > computeRms(leftIn) * 1.5f)
        
        // Check Right channel is still silence
        val rightOut = FloatArray(length) { output[it * 2 + 1] }
        assertEquals(0f, computeRms(rightOut), 0.0001f)
    }
}
