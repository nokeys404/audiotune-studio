package com.audiotune.studio.audio.dsp.dynamics

import com.audiotune.studio.audio.dsp.DspProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class ExpanderProcessor(
    override val id: String = "expander",
    private var sampleRate: Float = 48000f,
    private var channels: Int = 2
) : DspProcessor {
    override var isEnabled: Boolean = false

    var thresholdDb: Float = -40f
    var ratio: Float = 2f
    var attackMs: Float = 5f
    var releaseMs: Float = 50f

    private var env: Float = 0f
    private var floatBuffer = FloatArray(0)

    override fun configure(sampleRate: Float, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
    }

    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        if (!isEnabled) return inputBuffer

        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val numSamples = shortBuffer.remaining()

        if (floatBuffer.size < numSamples) {
            floatBuffer = FloatArray(numSamples)
        }

        for (i in 0 until numSamples) {
            floatBuffer[i] = shortBuffer.get(i).toFloat() / 32768f
        }

        processFloat(floatBuffer, numSamples)

        for (i in 0 until numSamples) {
            var sample = floatBuffer[i] * 32768f
            if (sample > 32767f) sample = 32767f
            if (sample < -32768f) sample = -32768f
            shortBuffer.put(i, sample.toInt().toShort())
        }

        return inputBuffer
    }

    private fun processFloat(buffer: FloatArray, length: Int) {
        val alphaA = exp(-1.0 / (sampleRate * (attackMs.coerceAtLeast(0.1f)) / 1000.0)).toFloat()
        val alphaR = exp(-1.0 / (sampleRate * (releaseMs.coerceAtLeast(0.1f)) / 1000.0)).toFloat()

        if (channels == 2) {
            var i = 0
            while (i < length - 1) {
                val l = buffer[i]
                val r = buffer[i + 1]
                val maxAbs = max(abs(l), abs(r))

                env = if (maxAbs > env) {
                    alphaA * env + (1f - alphaA) * maxAbs
                } else {
                    alphaR * env + (1f - alphaR) * maxAbs
                }

                val envDb = if (env > 1e-6f) 20f * log10(env) else -120f
                var gainDb = 0f
                if (envDb < thresholdDb) {
                    gainDb = (envDb - thresholdDb) * (ratio - 1f)
                }

                val linearGain = 10.0.pow(gainDb / 20.0).toFloat()
                buffer[i] = l * linearGain
                buffer[i + 1] = r * linearGain
                i += 2
            }
        } else if (channels == 1) {
            for (i in 0 until length) {
                val x = buffer[i]
                val absX = abs(x)

                env = if (absX > env) {
                    alphaA * env + (1f - alphaA) * absX
                } else {
                    alphaR * env + (1f - alphaR) * absX
                }

                val envDb = if (env > 1e-6f) 20f * log10(env) else -120f
                var gainDb = 0f
                if (envDb < thresholdDb) {
                    gainDb = (envDb - thresholdDb) * (ratio - 1f)
                }

                val linearGain = 10.0.pow(gainDb / 20.0).toFloat()
                buffer[i] = x * linearGain
            }
        }
    }

    override fun flush() {
        env = 0f
    }

    override fun release() {}
}
