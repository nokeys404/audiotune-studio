package com.audiotune.studio.audio.dsp.dynamics

import com.audiotune.studio.audio.dsp.DspProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class NoiseGateProcessor(
    override val id: String = "noise_gate",
    private var sampleRate: Float = 48000f,
    private var channels: Int = 2
) : DspProcessor {
    override var isEnabled: Boolean = false

    var thresholdDb: Float = -40f
    var attackMs: Float = 5f
    var holdMs: Float = 50f
    var releaseMs: Float = 100f
    var rangeDb: Float = -80f

    private var currentGain: Float = 1f
    private var holdSamplesRemaining: Int = 0
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
        val rangeLinear = 10.0.pow(rangeDb / 20.0).toFloat()
        val alphaA = exp(-1.0 / (sampleRate * (attackMs.coerceAtLeast(0.1f)) / 1000.0)).toFloat()
        val alphaR = exp(-1.0 / (sampleRate * (releaseMs.coerceAtLeast(0.1f)) / 1000.0)).toFloat()
        val holdSamplesTotal = (sampleRate * holdMs / 1000.0).toInt()

        if (channels == 2) {
            var i = 0
            while (i < length - 1) {
                val l = buffer[i]
                val r = buffer[i + 1]
                val maxAbs = max(abs(l), abs(r))
                
                val xDb = if (maxAbs > 1e-6f) 20f * log10(maxAbs) else -120f

                var targetGain = 1f
                if (xDb >= thresholdDb) {
                    holdSamplesRemaining = holdSamplesTotal
                    targetGain = 1f
                } else {
                    if (holdSamplesRemaining > 0) {
                        holdSamplesRemaining--
                        targetGain = 1f
                    } else {
                        targetGain = rangeLinear
                    }
                }

                if (targetGain > currentGain) {
                    currentGain = alphaA * currentGain + (1f - alphaA) * targetGain
                } else {
                    currentGain = alphaR * currentGain + (1f - alphaR) * targetGain
                }

                buffer[i] = l * currentGain
                buffer[i + 1] = r * currentGain
                i += 2
            }
        } else if (channels == 1) {
            for (i in 0 until length) {
                val x = buffer[i]
                val absX = abs(x)
                
                val xDb = if (absX > 1e-6f) 20f * log10(absX) else -120f

                var targetGain = 1f
                if (xDb >= thresholdDb) {
                    holdSamplesRemaining = holdSamplesTotal
                    targetGain = 1f
                } else {
                    if (holdSamplesRemaining > 0) {
                        holdSamplesRemaining--
                        targetGain = 1f
                    } else {
                        targetGain = rangeLinear
                    }
                }

                if (targetGain > currentGain) {
                    currentGain = alphaA * currentGain + (1f - alphaA) * targetGain
                } else {
                    currentGain = alphaR * currentGain + (1f - alphaR) * targetGain
                }

                buffer[i] = x * currentGain
            }
        }
    }

    override fun flush() {
        currentGain = 1f
        holdSamplesRemaining = 0
    }

    override fun release() {}
}
