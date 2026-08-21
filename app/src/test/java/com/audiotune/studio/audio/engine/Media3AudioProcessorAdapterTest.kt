package com.audiotune.studio.audio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Media3AudioProcessorAdapterTest {

    private lateinit var audioEngine: AudioEngine
    private lateinit var adapter: Media3AudioProcessorAdapter

    @Before
    fun setup() {
        audioEngine = AudioEngine()
        adapter = Media3AudioProcessorAdapter(audioEngine)
    }

    @Test
    fun testConfigureValidFormat() {
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        val result = adapter.configure(format)
        
        assertEquals(format, result)
        assertTrue(adapter.isActive())
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun testConfigureInvalidFormat() {
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        adapter.configure(format)
    }

    @Test
    fun testQueueInputAndGetOutput() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        val inputBuffer = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        for (i in 0 until 100) {
            inputBuffer.put(i.toByte())
        }
        inputBuffer.flip()

        adapter.queueInput(inputBuffer)
        
        val outputBuffer = adapter.getOutput()
        assertEquals(100, outputBuffer.remaining())
        for (i in 0 until 100) {
            assertEquals(i.toByte(), outputBuffer.get(i))
        }
    }
    
    @Test
    fun testEqControllerUpdatesParameters() {
        val controller = audioEngine.eqController
        controller.setEnabled(false)
        assertFalse(controller.isEnabled())
        
        controller.updateBandGain(0, 5f)
        assertEquals(5f, controller.getBand(0).gainDb, 0.001f)
        
        controller.updateBandFrequency(1, 150f)
        assertEquals(150f, controller.getBand(1).frequencyHz, 0.001f)
        
        controller.updateBandQ(2, 2.5f)
        assertEquals(2.5f, controller.getBand(2).q, 0.001f)
    }
}
