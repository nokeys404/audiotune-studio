package com.audiotune.studio.audio.dsp.eq

import com.audiotune.studio.audio.dsp.DspProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ParametricEqProcessor(
    override val id: String = "parametric_eq",
    private var sampleRate: Float = 48000f,
    private var channels: Int = 2
) : DspProcessor {
    override var isEnabled: Boolean = true

    private val bands = Array(10) { index ->
        val freqs = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        EqBand(frequencyHz = freqs[index], gainDb = 0f, q = 1f, type = EqFilterType.PEAKING, isEnabled = true)
    }

    private val filters = Array(10) { BiquadFilter() }
    
    // FloatArray cache for converting ByteBuffer to FloatArray without allocating every block
    private var floatBuffer = FloatArray(0)

    init {
        updateAllCoefficients()
    }

    override fun configure(sampleRate: Float, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        updateAllCoefficients()
    }

    fun updateBand(index: Int, band: EqBand) {
        if (index in bands.indices) {
            bands[index] = band
            filters[index].calculateCoefficients(sampleRate, band)
        }
    }
    
    fun getBand(index: Int): EqBand {
        return bands[index]
    }

    private fun updateAllCoefficients() {
        for (i in bands.indices) {
            filters[i].calculateCoefficients(sampleRate, bands[i])
        }
    }

    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        if (!isEnabled) return inputBuffer
        
        // Convert ByteBuffer (16-bit PCM native order) to FloatArray
        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val numSamples = shortBuffer.remaining()
        
        if (floatBuffer.size < numSamples) {
            floatBuffer = FloatArray(numSamples)
        }
        
        for (i in 0 until numSamples) {
            floatBuffer[i] = shortBuffer.get(i).toFloat() / 32768f
        }
        
        processFloat(floatBuffer, numSamples)
        
        // Convert back
        for (i in 0 until numSamples) {
            var sample = floatBuffer[i] * 32768f
            if (sample > 32767f) sample = 32767f
            if (sample < -32768f) sample = -32768f
            shortBuffer.put(i, sample.toInt().toShort())
        }
        
        return inputBuffer
    }
    
    fun processFloat(buffer: FloatArray, length: Int) {
        if (!isEnabled) return
        for (i in bands.indices) {
            if (bands[i].isEnabled && bands[i].gainDb != 0f) {
                if (channels == 2) {
                    filters[i].processStereoBlock(buffer, 0, length)
                } else if (channels == 1) {
                    filters[i].processMonoBlock(buffer, 0, length)
                }
            }
        }
    }

    override fun flush() {
        for (filter in filters) {
            filter.resetState()
        }
    }

    override fun release() {
        // Nothing to release natively
    }
}
