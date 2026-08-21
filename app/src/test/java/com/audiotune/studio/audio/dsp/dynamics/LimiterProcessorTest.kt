package com.audiotune.studio.audio.dsp.dynamics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LimiterProcessorTest {

    private lateinit var limiter: LimiterProcessor

    @Before
    fun setup() {
        limiter = LimiterProcessor()
        limiter.configure(48000f, 2)
        limiter.isEnabled = true
    }

    @Test
    fun testBypassReturnsUnchanged() {
        limiter.isEnabled = false
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort((i * 10).toShort())
        }
        input.flip()

        val output = limiter.process(input)
        
        for (i in 0 until 50) {
            assertEquals((i * 10).toShort(), output.getShort(i * 2))
        }
    }

    @Test
    fun testCeilingLimitsPeaks() {
        limiter.ceilingDb = -6.02f // Approx 0.5 amplitude
        
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) {
            // Put 0dBFS signal
            input.putShort(32767.toShort())
        }
        input.flip()

        val output = limiter.process(input)
        
        // Output should not exceed ceiling
        for (i in 0 until 200) {
            val sample = output.getShort(i * 2)
            assertTrue("Expected sample to be limited to approx 16384, got $sample", sample <= 16500)
        }
    }

    @Test
    fun testMonoProcessing() {
        limiter.configure(48000f, 1)
        limiter.ceilingDb = -6.02f
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort(32767.toShort())
        }
        input.flip()

        val output = limiter.process(input)
        for (i in 0 until 50) {
            val sample = output.getShort(i * 2)
            assertTrue("Expected mono sample to be limited", sample <= 16500)
        }
    }
}
