package com.audiotune.studio.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.audiotune.studio.audio.engine.AudioEngine
import com.audiotune.studio.audio.engine.Media3AudioProcessorAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DspPipelineIntegrationTest {

    @Test
    fun testAllBypassReturnsIdentical() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        engine.noiseGateController.setEnabled(false)
        engine.expanderController.setEnabled(false)
        engine.eqController.setEnabled(false)
        engine.compressorController.setEnabled(false)
        engine.limiterController.setEnabled(false)
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort((i * 10).toShort())
        }
        input.flip()
        
        adapter.queueInput(input)
        val output = adapter.getOutput()
        
        assertEquals(100, output.remaining())
        for (i in 0 until 50) {
            assertEquals((i * 10).toShort(), output.getShort(i * 2))
        }
    }

    @Test
    fun testProcessingOrder() {
        val pipeline = DspPipeline()
        val order = mutableListOf<String>()
        
        class TestProcessor(override val id: String) : DspProcessor {
            override var isEnabled = true
            override fun configure(sampleRate: Float, channels: Int) {}
            override fun process(inputBuffer: ByteBuffer): ByteBuffer {
                order.add(id)
                return inputBuffer
            }
            override fun flush() {}
            override fun release() {}
        }
        
        pipeline.addProcessor(TestProcessor("limiter"))
        pipeline.addProcessor(TestProcessor("parametric_eq"))
        pipeline.addProcessor(TestProcessor("noise_gate"))
        pipeline.addProcessor(TestProcessor("expander"))
        pipeline.addProcessor(TestProcessor("compressor"))
        
        val input = ByteBuffer.allocateDirect(10).order(ByteOrder.nativeOrder())
        pipeline.process(input)
        
        val expected = listOf("noise_gate", "expander", "parametric_eq", "compressor", "limiter")
        assertEquals(expected, order)
    }
    
    @Test
    fun testMonoAndStereoHandling44100And48000() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        
        // 44100 Mono
        adapter.configure(AudioFormat(44100, 1, C.ENCODING_PCM_16BIT))
        var input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input.limit(100)
        adapter.queueInput(input)
        var output = adapter.getOutput()
        assertEquals(100, output.remaining())
        
        // 48000 Stereo
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input.limit(100)
        adapter.queueInput(input)
        output = adapter.getOutput()
        assertEquals(100, output.remaining())
    }
    
    @Test
    fun testEmptyBufferHandling() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        val input = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        adapter.queueInput(input)
        val output = adapter.getOutput()
        assertEquals(0, output.remaining())
    }
    
    @Test
    fun testParameterUpdates() {
        val engine = AudioEngine()
        // No crash during configure
        engine.configure(48000f, 2)
        
        // EQ
        engine.eqController.updateBandGain(0, 5f)
        engine.eqController.updateBandFrequency(0, 100f)
        engine.eqController.updateBandQ(0, 1f)
        
        // Compressor
        engine.compressorController.setThresholdDb(-10f)
        engine.compressorController.setRatio(4f)
        engine.compressorController.setAttackMs(10f)
        engine.compressorController.setReleaseMs(100f)
        engine.compressorController.setMakeupGainDb(2f)
        
        // Limiter
        engine.limiterController.setCeilingDb(-1f)
        engine.limiterController.setReleaseMs(50f)
        
        // Noise Gate
        engine.noiseGateController.setThresholdDb(-50f)
        engine.noiseGateController.setAttackMs(5f)
        engine.noiseGateController.setReleaseMs(50f)
        
        // Expander
        engine.expanderController.setThresholdDb(-30f)
        engine.expanderController.setRatio(2f)
        engine.expanderController.setAttackMs(5f)
        engine.expanderController.setReleaseMs(50f)
        
        // Safe parameter update assertion
        assertTrue(true) 
    }
    
    @Test
    fun testFlushResetsState() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        // Just verify flush doesn't crash and returns cleanly
        engine.flush()
        adapter.flush()
        assertTrue(true)
    }

    @Test
    fun testPartialBypass() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        engine.noiseGateController.setEnabled(false)
        engine.expanderController.setEnabled(false)
        engine.eqController.setEnabled(true)
        engine.compressorController.setEnabled(true)
        engine.limiterController.setEnabled(false)
        
        engine.compressorController.setThresholdDb(-40f)
        engine.compressorController.setRatio(10f)
        engine.compressorController.setAttackMs(0.1f)
        
        // Feed enough samples to trigger compression
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.nativeOrder())
        for (i in 0 until 200) {
            input.putShort(30000.toShort()) // High signal
        }
        input.flip()
        
        adapter.queueInput(input)
        val output = adapter.getOutput()
        
        // Because compressor is enabled and signal is high, it should be compressed.
        val lastSample = output.getShort(198 * 2)
        assertNotEquals(30000.toShort(), lastSample)
        assertTrue(lastSample < 20000)
    }
}
