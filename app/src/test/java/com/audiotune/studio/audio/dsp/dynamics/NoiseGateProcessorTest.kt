package com.audiotune.studio.audio.dsp.dynamics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NoiseGateProcessorTest {
    private lateinit var gate: NoiseGateProcessor

    @Before
    fun setup() {
        gate = NoiseGateProcessor()
        gate.configure(48000f, 2)
        gate.isEnabled = true
    }

    @Test
    fun testBypassReturnsUnchanged() {
        gate.isEnabled = false
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { input.putShort((i * 10).toShort()) }
        input.flip()
        val output = gate.process(input)
        for (i in 0 until 50) { assertEquals((i * 10).toShort(), output.getShort(i * 2)) }
    }

    @Test
    fun testGateOpensAboveThreshold() {
        gate.thresholdDb = -10f
        gate.attackMs = 0.1f
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { input.putShort(30000.toShort()) } // High signal
        input.flip()
        
        val output = gate.process(input)
        val lastSample = output.getShort(98)
        // Should be completely open (gain near 1.0)
        assertTrue("Expected gate to open, got $lastSample", lastSample > 29000)
    }

    @Test
    fun testGateAttenuatesBelowThreshold() {
        gate.thresholdDb = -10f // -10 dBFS is around 10000
        gate.rangeDb = -80f
        gate.releaseMs = 0.1f
        gate.holdMs = 0f
        
        // First open the gate
        val inputOpen = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { inputOpen.putShort(30000.toShort()) }
        inputOpen.flip()
        gate.process(inputOpen)

        // Now send low signal
        val inputClosed = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { inputClosed.putShort(1000.toShort()) } // Low signal
        inputClosed.flip()
        
        val output = gate.process(inputClosed)
        val lastSample = output.getShort(98)
        // Should be heavily attenuated
        assertTrue("Expected gate to attenuate, got $lastSample", lastSample < 10)
    }

    @Test
    fun testMonoProcessing() {
        gate.configure(48000f, 1)
        gate.thresholdDb = -10f
        gate.holdMs = 0f
        gate.releaseMs = 0.1f
        gate.rangeDb = -80f

        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { input.putShort(1000.toShort()) }
        input.flip()

        val output = gate.process(input)
        val lastSample = output.getShort(98)
        assertTrue("Expected mono signal to be attenuated, got $lastSample", lastSample < 10)
    }

    @Test
    fun testFlushResetsState() {
        gate.thresholdDb = -10f
        gate.holdMs = 100f
        gate.releaseMs = 100f

        val inputOpen = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) { inputOpen.putShort(30000.toShort()) }
        inputOpen.flip()
        gate.process(inputOpen)
        
        gate.flush()
        
        val inputClosed = ByteBuffer.allocateDirect(10).order(ByteOrder.nativeOrder())
        for (i in 0 until 5) { inputClosed.putShort(1000.toShort()) }
        inputClosed.flip()
        
        val output = gate.process(inputClosed)
        // Since we flushed, holdSamplesRemaining = 0.
        // Target gain will be rangeLinear.
        // currentGain will decay towards rangeLinear.
        // Let's assert it decays (is less than 1000).
        val firstSample = output.getShort(0)
        assertTrue("Expected decay to start, got $firstSample", firstSample < 1000)
    }
}
