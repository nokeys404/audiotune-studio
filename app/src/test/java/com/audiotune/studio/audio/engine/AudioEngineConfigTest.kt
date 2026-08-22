package com.audiotune.studio.audio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class AudioEngineConfigTest {

    private lateinit var audioEngine: AudioEngine
    private lateinit var adapter: Media3AudioProcessorAdapter

    @Before
    fun setup() {
        audioEngine = AudioEngine()
        adapter = Media3AudioProcessorAdapter(audioEngine)
    }

    @Test
    fun testReconfigurationPropagates() {
        // Initial config
        val format1 = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat1 = adapter.configure(format1)
        assertEquals(44100, outputFormat1.sampleRate)
        assertEquals(2, outputFormat1.channelCount)

        // Ensure flush doesn't throw
        adapter.flush()
        
        // Reconfigure
        val format2 = AudioFormat(48000, 1, C.ENCODING_PCM_16BIT)
        val outputFormat2 = adapter.configure(format2)
        assertEquals(48000, outputFormat2.sampleRate)
        assertEquals(1, outputFormat2.channelCount)
        
        // Let's process a small buffer to ensure no crashes due to mono vs stereo
        val buffer = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        adapter.queueInput(buffer)
        assertTrue(adapter.output.remaining() > 0)
    }

    @Test
    fun testZeroLengthBuffer() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        val buffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        adapter.queueInput(buffer)
        assertEquals(0, adapter.output.remaining())
    }

    @Test
    fun testEndOfStream() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        val buffer = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        adapter.queueInput(buffer)
        val out = adapter.output // Consume output
        
        adapter.queueEndOfStream()
        assertTrue(adapter.isEnded)
    }

    @Test
    fun testFlushResetsIsEnded() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        adapter.queueEndOfStream()
        assertTrue(adapter.isEnded)
        
        adapter.flush()
        assertTrue(!adapter.isEnded)
    }
}
