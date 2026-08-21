package com.audiotune.studio.audio.dsp.eq

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.audiotune.studio.audio.engine.AudioEngine
import com.audiotune.studio.audio.engine.Media3AudioProcessorAdapter
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DspSafetyTest {

    @Test
    fun testPcm16BitNativeOrderIsPreserved() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        adapter.configure(format)
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 50) {
            input.putShort(i.toShort())
        }
        input.flip()
        
        // Ensure EQ is flat so it doesn't change anything
        for (i in 0 until 10) {
            engine.eqController.updateBandGain(i, 0f)
        }
        
        adapter.queueInput(input)
        
        val output = adapter.getOutput()
        assertEquals(100, output.remaining())
        
        // Output should match input
        for (i in 0 until 50) {
            assertEquals(i.toShort(), output.getShort(i * 2))
        }
    }

    @Test
    fun testByteBufferPositionAndLimit() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input.limit(100)
        input.position(20) // 80 bytes remaining
        
        val initialPosition = input.position()
        val initialLimit = input.limit()
        
        adapter.queueInput(input)
        
        // Adapter should consume all remaining bytes
        assertEquals(initialLimit, input.position())
        
        val output = adapter.getOutput()
        assertEquals(0, output.position())
        assertEquals(80, output.limit())
    }

    @Test
    fun testBypassReturnsUnchanged() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        engine.eqController.setEnabled(false)
        
        val input = ByteBuffer.allocateDirect(200).order(ByteOrder.nativeOrder())
        for (i in 0 until 100) {
            input.putShort(i.toShort())
        }
        input.flip()
        
        adapter.queueInput(input)
        val output = adapter.getOutput()
        
        for (i in 0 until 100) {
            assertEquals(i.toShort(), output.getShort(i * 2))
        }
    }

    @Test
    fun testMonoAndStereoHandling() {
        val engine = AudioEngine()
        
        // Test Mono
        var adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 1, C.ENCODING_PCM_16BIT))
        var input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input.limit(100)
        adapter.queueInput(input)
        var output = adapter.getOutput()
        assertEquals(100, output.remaining())
        
        // Test Stereo
        adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input.limit(100)
        adapter.queueInput(input)
        output = adapter.getOutput()
        assertEquals(100, output.remaining())
    }
    
    @Test
    fun testFilterStateAcrossBlocks() {
        val engine = AudioEngine()
        val adapter = Media3AudioProcessorAdapter(engine)
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        // Turn up bass
        engine.eqController.updateBandGain(0, 10f)
        
        val input1 = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input1.limit(100) // All zeros
        
        val input2 = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input2.putShort(0, 1000) // Put impulse in second block
        input2.limit(100)
        
        adapter.queueInput(input1)
        adapter.getOutput()
        
        adapter.queueInput(input2)
        val output2 = adapter.getOutput()
        
        // Because of the impulse, output should not be zero
        var hasNonZero = false
        for (i in 0 until 50) {
            if (output2.getShort(i * 2) != 0.toShort()) {
                hasNonZero = true
                break
            }
        }
        assertTrue(hasNonZero)
        
        adapter.flush()
        
        val input3 = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        input3.limit(100)
        adapter.queueInput(input3)
        val output3 = adapter.getOutput()
        
        // After flush, feeding zeros should result in zeros (state reset)
        for (i in 0 until 50) {
            assertEquals(0.toShort(), output3.getShort(i * 2))
        }
    }
}
