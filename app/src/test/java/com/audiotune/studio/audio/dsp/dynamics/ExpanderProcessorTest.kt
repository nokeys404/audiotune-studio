package com.audiotune.studio.audio.dsp.dynamics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ExpanderProcessorTest {
    private lateinit var expander: ExpanderProcessor

    @Before
    fun setup() {
        expander = ExpanderProcessor()
        expander.configure(48000f, 2)
        expander.isEnabled = true
    }

    @Test
    fun testBypassReturnsUnchanged() {
        expander.isEnabled = false
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { input.putShort((i * 10).toShort()) }
        input.flip()
        val output = expander.process(input)
        for (i in 0 until 50) { assertEquals((i * 10).toShort(), output.getShort(i * 2)) }
    }

    @Test
    fun testGainReductionBelowThreshold() {
        expander.thresholdDb = -20f
        expander.ratio = 2f
        expander.attackMs = 0.1f
        expander.releaseMs = 0.1f

        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) {
            input.putShort(3276.toShort()) // Approx -20dBFS
        }
        input.flip()
        
        // First process to settle the envelope
        expander.process(input)
        
        val lowInput = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            lowInput.putShort(327.toShort()) // Approx -40dBFS
        }
        lowInput.flip()
        
        val output = expander.process(lowInput)
        val lastSample = output.getShort(98)
        
        // If input is -40dBFS (327), threshold is -20dBFS.
        // Ratio = 2, so output should be -20 - 2*(20) = -60dBFS (approx 32)
        assertTrue("Expected signal to be expanded (reduced), got $lastSample", lastSample in 10..100)
    }

    @Test
    fun testNoReductionAboveThreshold() {
        expander.thresholdDb = -40f
        expander.ratio = 2f
        
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) {
            input.putShort(16000.toShort()) // Above -40dBFS
        }
        input.flip()

        val output = expander.process(input)
        val lastSample = output.getShort(398)
        // Should be un-attenuated
        assertTrue("Expected signal to pass unattenuated, got $lastSample", lastSample > 15000)
    }

    @Test
    fun testMonoProcessing() {
        expander.configure(48000f, 1)
        expander.thresholdDb = -20f
        expander.ratio = 2f
        expander.releaseMs = 0.1f
        
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) { input.putShort(327.toShort()) }
        input.flip()

        val output = expander.process(input)
        val lastSample = output.getShort(198)
        assertTrue("Expected mono signal to be expanded, got $lastSample", lastSample < 100)
    }
}
