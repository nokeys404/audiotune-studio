package com.audiotune.studio.audio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.dsp.eq.EqFilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class AudioEngineIntegrationTest {

    private lateinit var audioEngine: AudioEngine
    private lateinit var adapter: Media3AudioProcessorAdapter

    @Before
    fun setup() {
        audioEngine = AudioEngine()
        adapter = Media3AudioProcessorAdapter(audioEngine)
    }

    private fun createSine(freq: Double, amp: Double, sampleRate: Int = 48000, channels: Int = 2, seconds: Double = 0.1): ByteBuffer {
        val frames = (sampleRate * seconds).toInt()
        val buffer = ByteBuffer.allocateDirect(frames * channels * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * freq * i / sampleRate) * amp).coerceIn(-1.0, 1.0)
            val pcm = (sample * 32767.0).toInt().toShort()
            repeat(channels) { buffer.putShort(pcm) }
        }
        buffer.flip()
        return buffer
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

    @Test
    fun testRealAudioEnginePathBypass() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        // Everything is off by default except parametric_eq (Wait, Eq is enabled by default? Let's turn all off just in case, but let's check Eq)
        audioEngine.eqController.setEnabled(false)
        audioEngine.compressorController.setEnabled(false)
        audioEngine.limiterController.setEnabled(false)
        audioEngine.noiseGateController.setEnabled(false)
        audioEngine.expanderController.setEnabled(false)

        val input = createSine(1000.0, 0.5)
        val originalRms = getRms(input)
        input.position(0)
        
        adapter.queueInput(input)
        val output = adapter.output
        
        val bypassRms = getRms(output)
        assertEquals("Bypass RMS should match original", originalRms, bypassRms, 1e-4)
    }

    @Test
    fun testRealAudioEnginePathEnabled() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        
        // Enable EQ and boost
        audioEngine.eqController.setEnabled(true)
        audioEngine.eqController.updateBand(0, EqBand(1000f, 12f, 1f, EqFilterType.PEAKING))
        
        val input = createSine(1000.0, 0.25)
        val originalRms = getRms(input)
        input.position(0)
        
        adapter.queueInput(input)
        val output = adapter.output
        
        val newRms = getRms(output)
        assertTrue("Enabled EQ should modify signal in adapter", newRms > originalRms * 1.5)
    }
    
    @Test
    fun testAdapterConsumesInputBuffer() {
        adapter.configure(AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        val input = createSine(1000.0, 0.5, seconds = 0.01)
        val originalCapacity = input.remaining()
        
        adapter.queueInput(input)
        
        assertEquals("Adapter should consume input buffer", 0, input.remaining())
        val output = adapter.output
        assertEquals("Output buffer should have same size as input", originalCapacity, output.remaining())
    }
}
