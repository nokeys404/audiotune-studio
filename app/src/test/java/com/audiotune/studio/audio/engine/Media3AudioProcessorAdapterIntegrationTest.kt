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

class Media3AudioProcessorAdapterIntegrationTest {

    private lateinit var audioEngine: AudioEngine
    private lateinit var adapter: Media3AudioProcessorAdapter

    @Before
    fun setup() {
        audioEngine = AudioEngine()
        adapter = Media3AudioProcessorAdapter(audioEngine)
    }

    @Test
    fun testConfigurePropagatesToAudioEngine() {
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        adapter.configure(format)
        
        // Disable processors to bypass
        audioEngine.dspPipeline.setProcessorEnabled("noise_gate", false)
        audioEngine.dspPipeline.setProcessorEnabled("expander", false)
        audioEngine.dspPipeline.setProcessorEnabled("parametric_eq", false)
        audioEngine.dspPipeline.setProcessorEnabled("compressor", false)
        audioEngine.dspPipeline.setProcessorEnabled("limiter", false)

        val input = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder())
        adapter.queueInput(input)
        val output = adapter.output
        assertTrue(output.capacity() >= 100)
    }
}
