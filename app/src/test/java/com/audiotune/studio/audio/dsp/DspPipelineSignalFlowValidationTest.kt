package com.audiotune.studio.audio.dsp

import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.dsp.eq.EqFilterType
import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor
import com.audiotune.studio.audio.dsp.dynamics.CompressorProcessor
import com.audiotune.studio.audio.dsp.dynamics.LimiterProcessor
import com.audiotune.studio.audio.dsp.dynamics.NoiseGateProcessor
import com.audiotune.studio.audio.dsp.dynamics.ExpanderProcessor

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.Before

import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.max

class DspPipelineSignalFlowValidationTest {

    private val sampleRate = 48000f
    private val sampleRateInt = 48000
    private val channels = 2

    private lateinit var pipeline: DspPipeline
    
    // Processors
    private lateinit var eq: ParametricEqProcessor
    private lateinit var compressor: CompressorProcessor
    private lateinit var limiter: LimiterProcessor
    private lateinit var noiseGate: NoiseGateProcessor
    private lateinit var expander: ExpanderProcessor

    @Before
    fun setup() {
        pipeline = DspPipeline()
        
        eq = ParametricEqProcessor()
        compressor = CompressorProcessor()
        limiter = LimiterProcessor()
        noiseGate = NoiseGateProcessor()
        expander = ExpanderProcessor()
        
        // Add them out-of-order to test sorting
        pipeline.addProcessor(limiter)
        pipeline.addProcessor(compressor)
        pipeline.addProcessor(eq)
        pipeline.addProcessor(expander)
        pipeline.addProcessor(noiseGate)
        
        pipeline.configure(sampleRate, channels)
        
        // Disable all initially
        listOf(eq, compressor, limiter, noiseGate, expander).forEach { it.isEnabled = false }
    }

    private fun createSine(freq: Double, amp: Double, seconds: Double = 0.5): ByteBuffer {
        val frames = (sampleRateInt * seconds).toInt()
        val buffer = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * freq * i / sampleRateInt) * amp).coerceIn(-1.0, 1.0)
            val pcm = (sample * 32767.0).toInt().toShort()
            repeat(channels) { buffer.putShort(pcm) }
        }
        buffer.flip()
        return buffer
    }
    
    private fun createStereoSine(freqL: Double, ampL: Double, freqR: Double, ampR: Double, seconds: Double = 0.5): ByteBuffer {
        val frames = (sampleRateInt * seconds).toInt()
        val buffer = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until frames) {
            val sampleL = (sin(2.0 * PI * freqL * i / sampleRateInt) * ampL).coerceIn(-1.0, 1.0)
            val sampleR = (sin(2.0 * PI * freqR * i / sampleRateInt) * ampR).coerceIn(-1.0, 1.0)
            val pcmL = (sampleL * 32767.0).toInt().toShort()
            val pcmR = (sampleR * 32767.0).toInt().toShort()
            buffer.putShort(pcmL)
            buffer.putShort(pcmR)
        }
        buffer.flip()
        return buffer
    }

    private fun getBufferMaxDiff(b1: ByteBuffer, b2: ByteBuffer): Double {
        val d1 = b1.duplicate().order(ByteOrder.nativeOrder())
        val d2 = b2.duplicate().order(ByteOrder.nativeOrder())
        var maxDiff = 0.0
        while (d1.remaining() >= 2 && d2.remaining() >= 2) {
            val s1 = d1.short / 32768.0
            val s2 = d2.short / 32768.0
            maxDiff = max(maxDiff, abs(s1 - s2))
        }
        return maxDiff
    }

    private fun getRms(buffer: ByteBuffer): Double {
        val d = buffer.duplicate().order(ByteOrder.nativeOrder())
        var sum = 0.0
        var count = 0
        while (d.remaining() >= 2) {
            val s = d.short / 32768.0
            sum += s * s
            count++
        }
        return if (count == 0) 0.0 else sqrt(sum / count)
    }

    // 2. VERIFY PROCESSOR BYPASS BEHAVIOR
    @Test
    fun testBypassBehavior() {
        val input = createSine(1000.0, 0.5)
        val originalRms = getRms(input)
        
        val processors = listOf(eq, compressor, limiter, noiseGate, expander)
        
        for (p in processors) {
            p.isEnabled = false
            // Reset buffer position
            input.position(0)
            val output = pipeline.process(input)
            output.position(0)
            input.position(0)
            val diff = getBufferMaxDiff(input, output)
            println("BYPASS DIFF for ${p.id}: $diff")
            assertTrue("Processor ${p.id} modified signal while disabled", diff < 1e-4)
        }
    }

    // 3. VERIFY ENABLED PROCESSOR BEHAVIOR
    @Test
    fun testEnabledProcessing_EQ() {
        eq.isEnabled = true
        eq.updateBand(0, EqBand(1000f, 6f, 1f, EqFilterType.PEAKING))
        
        val input = createSine(1000.0, 0.25)
        val originalRms = getRms(input)
        input.position(0)
        
        val output = pipeline.process(input)
        output.position(0)
        
        val newRms = getRms(output)
        println("ENABLED EQ: InRms=$originalRms, OutRms=$newRms")
        assertTrue("EQ boost should increase RMS", newRms > originalRms * 1.5)
        eq.isEnabled = false
    }

    @Test
    fun testEnabledProcessing_Compressor() {
        compressor.isEnabled = true
        compressor.thresholdDb = -12f
        compressor.ratio = 4f
        
        // Below threshold (-20 dB approx)
        val inputBelow = createSine(1000.0, 0.1)
        inputBelow.position(0)
        val outBelow = pipeline.process(inputBelow)
        outBelow.position(0)
        inputBelow.position(0)
        assertTrue("Compressor modified below threshold", getBufferMaxDiff(inputBelow, outBelow) < 1e-3)
        
        // Above threshold (0 dB approx)
        val inputAbove = createSine(1000.0, 1.0)
        val originalRms = getRms(inputAbove)
        inputAbove.position(0)
        val outAbove = pipeline.process(inputAbove)
        val newRms = getRms(outAbove)
        assertTrue("Compressor didn't compress above threshold", newRms < originalRms * 0.8)
        compressor.isEnabled = false
    }

    // 4. VERIFY PIPELINE PROCESSOR ORDER
    @Test
    fun testPipelineOrder() {
        // We know expected is: Gate -> Expander -> EQ -> Compressor -> Limiter
        val state = pipeline.pipelineState.value
        assertEquals("noise_gate", state.processorIds[0])
        assertEquals("expander", state.processorIds[1])
        assertEquals("parametric_eq", state.processorIds[2])
        assertEquals("compressor", state.processorIds[3])
        assertEquals("limiter", state.processorIds[4])
    }

    // 5. VERIFY MULTIPLE PROCESSORS
    @Test
    fun testMultipleProcessors() {
        listOf(eq, compressor, limiter).forEach { it.isEnabled = true }
        eq.updateBand(0, EqBand(1000f, 10f, 1f, EqFilterType.PEAKING))
        compressor.thresholdDb = -20f
        compressor.ratio = 10f
        limiter.ceilingDb = -5f
        
        val input = createSine(1000.0, 0.5)
        val output = pipeline.process(input)
        
        val d = output.duplicate().order(ByteOrder.nativeOrder())
        d.position(0)
        var maxVal = 0.0
        while(d.remaining() >= 2) {
            val v = abs(d.short / 32768.0)
            assertTrue("NaN or Infinity encountered", !v.isNaN() && !v.isInfinite())
            maxVal = max(maxVal, v)
        }
        assertTrue("Output exceeded Limiter ceiling despite multi-processor chain", maxVal <= (Math.pow(10.0, -5.0/20.0)) + 0.05)
    }

    // 6. VERIFY STEREO CHANNEL INTEGRITY
    @Test
    fun testStereoIntegrity() {
        eq.isEnabled = true
        eq.updateBand(0, EqBand(1000f, 0f, 1f, EqFilterType.PEAKING))
        
        // L = 1kHz, R = 2kHz
        val input = createStereoSine(1000.0, 0.5, 2000.0, 0.1)
        val originalRms = getRms(input)
        input.position(0)
        val output = pipeline.process(input)
        output.position(0)
        
        // Manually calculate RMS for L and R
        val outDup = output.duplicate().order(ByteOrder.nativeOrder())
        var sumL = 0.0
        var sumR = 0.0
        var count = 0
        while (outDup.remaining() >= 4) {
            val sL = outDup.short / 32768.0
            val sR = outDup.short / 32768.0
            sumL += sL * sL
            sumR += sR * sR
            count++
        }
        val rmsL = sqrt(sumL / count)
        val rmsR = sqrt(sumR / count)
        
        assertTrue("Stereo Left channel broken", abs(rmsL - 0.353) < 0.05) // Sine RMS of 0.5 is approx 0.353
        assertTrue("Stereo Right channel broken", abs(rmsR - 0.070) < 0.05) // Sine RMS of 0.1 is approx 0.070
    }

    // 7. VERIFY STATEFUL PROCESSING
    @Test
    fun testStatefulProcessing() {
        compressor.isEnabled = true
        compressor.thresholdDb = -20f
        
        val inputFull = createSine(1000.0, 0.5, 1.0)
        inputFull.position(0)
        val outFull = pipeline.process(inputFull)
        
        pipeline.flush()
        
        val outChunksBuffer = ByteBuffer.allocate(inputFull.capacity()).order(ByteOrder.nativeOrder())
        val chunkSize = sampleRateInt * channels * 2 / 10 // 0.1s
        
        for (i in 0 until 10) {
            val chunk = createSine(1000.0, 0.5, 0.1)
            val outChunk = pipeline.process(chunk)
            outChunk.position(0)
            outChunksBuffer.put(outChunk)
        }
        outChunksBuffer.flip()
        
        // Output might not be perfectly identical due to bytebuffer slice/exact alignment, but should be close.
        val diff = getBufferMaxDiff(outFull, outChunksBuffer)
        println("STATEFUL MULTI-BLOCK DIFF: $diff")
        assertTrue("Stateful continuity broken", diff < 1e-3)
    }

    // 8. VERIFY CONFIGURE / RELEASE LIFECYCLE
    @Test
    fun testLifecycle() {
        pipeline.configure(sampleRate, channels)
        val out1 = pipeline.process(createSine(1000.0, 0.5, 0.1))
        pipeline.release()
        
        pipeline.configure(sampleRate, channels)
        val out2 = pipeline.process(createSine(1000.0, 0.5, 0.1))
        
        assertTrue(out2.limit() > 0)
    }

    // 9. VERIFY PIPELINE SAFETY
    @Test
    fun testPipelineSafety() {
        listOf(eq, compressor, limiter, noiseGate, expander).forEach { it.isEnabled = true }
        // Test short buffers
        val shortBuffer = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder())
        pipeline.process(shortBuffer)
        
        // Test silence
        val silence = createSine(1000.0, 0.0)
        pipeline.process(silence)
        
        // Alternate samples
        val altBuffer = ByteBuffer.allocate(1000).order(ByteOrder.nativeOrder())
        for (i in 0 until 500 step 2) {
            altBuffer.putShort(32767.toShort())
            altBuffer.putShort((-32768).toShort())
        }
        altBuffer.flip()
        pipeline.process(altBuffer)
        
        assertTrue(true) // No exceptions thrown means success
    }
}
