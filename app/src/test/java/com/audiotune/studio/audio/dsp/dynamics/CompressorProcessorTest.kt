package com.audiotune.studio.audio.dsp.dynamics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CompressorProcessorTest {

    private lateinit var compressor: CompressorProcessor

    @Before
    fun setup() {
        compressor = CompressorProcessor()
        compressor.configure(48000f, 2)
        compressor.isEnabled = true
    }

    @Test
    fun testBypassReturnsUnchanged() {
        compressor.isEnabled = false
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort((i * 10).toShort())
        }
        input.flip()

        val output = compressor.process(input)
        
        for (i in 0 until 50) {
            assertEquals((i * 10).toShort(), output.getShort(i * 2))
        }
    }

    @Test
    fun testGainReduction() {
        compressor.thresholdDb = -20f
        compressor.ratio = 4f
        compressor.attackMs = 0.1f // Fast attack
        compressor.releaseMs = 100f
        compressor.makeupGainDb = 0f
        
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) {
            // Put large signal (0dBFS)
            input.putShort(32767.toShort())
        }
        input.flip()

        val output = compressor.process(input)
        
        // Output towards the end should be reduced significantly
        val lastSample = output.getShort(398)
        assertTrue("Expected signal to be compressed, got $lastSample", lastSample < 32767)
    }

    @Test
    fun testFlushResetsState() {
        compressor.thresholdDb = -60f // Extreme compression
        compressor.ratio = 10f
        compressor.attackMs = 0.1f
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort(32767.toShort())
        }
        input.flip()

        compressor.process(input)
        
        compressor.flush()
        
        // After flush, if we send a small impulse below threshold, it shouldn't be compressed
        val input2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input2.putShort(10) // 10 is very small, below -60dB threshold
        input2.putShort(10)
        input2.flip()
        
        val output2 = compressor.process(input2)
        assertEquals(10.toShort(), output2.getShort(0))
    }

    @Test
    fun testMonoProcessing() {
        compressor.configure(48000f, 1)
        compressor.thresholdDb = -10f
        compressor.attackMs = 0.1f // Fast attack to trigger quickly
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort(32767.toShort())
        }
        input.flip()

        val output = compressor.process(input)
        val lastSample = output.getShort(98)
        assertTrue("Expected mono signal to be compressed", lastSample < 32767)
    }
}
