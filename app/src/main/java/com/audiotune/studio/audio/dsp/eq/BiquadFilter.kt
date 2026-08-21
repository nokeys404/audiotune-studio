package com.audiotune.studio.audio.dsp.eq

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class BiquadFilter {
    val coefficients = BiquadCoefficients()

    // State variables
    private var x1L: Float = 0f
    private var x2L: Float = 0f
    private var y1L: Float = 0f
    private var y2L: Float = 0f

    private var x1R: Float = 0f
    private var x2R: Float = 0f
    private var y1R: Float = 0f
    private var y2R: Float = 0f

    fun calculateCoefficients(sampleRate: Float, band: EqBand) {
        if (sampleRate <= 0) return
        val w0 = 2.0 * Math.PI * band.frequencyHz / sampleRate
        val cosW0 = cos(w0).toFloat()
        val sinW0 = sin(w0).toFloat()
        val alpha = sinW0 / (2.0f * band.q)
        val a = 10.0.pow(band.gainDb / 40.0).toFloat()

        var b0 = 1f
        var b1 = 0f
        var b2 = 0f
        var a0 = 1f
        var a1 = 0f
        var a2 = 0f

        when (band.type) {
            EqFilterType.PEAKING -> {
                b0 = 1.0f + alpha * a
                b1 = -2.0f * cosW0
                b2 = 1.0f - alpha * a
                a0 = 1.0f + alpha / a
                a1 = -2.0f * cosW0
                a2 = 1.0f - alpha / a
            }
            EqFilterType.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                val sqrtA2Alpha = 2.0f * sqrtA * alpha
                b0 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 + sqrtA2Alpha)
                b1 = 2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0)
                b2 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 - sqrtA2Alpha)
                a0 = (a + 1.0f) + (a - 1.0f) * cosW0 + sqrtA2Alpha
                a1 = -2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0)
                a2 = (a + 1.0f) + (a - 1.0f) * cosW0 - sqrtA2Alpha
            }
            EqFilterType.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                val sqrtA2Alpha = 2.0f * sqrtA * alpha
                b0 = a * ((a + 1.0f) + (a - 1.0f) * cosW0 + sqrtA2Alpha)
                b1 = -2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosW0)
                b2 = a * ((a + 1.0f) + (a - 1.0f) * cosW0 - sqrtA2Alpha)
                a0 = (a + 1.0f) - (a - 1.0f) * cosW0 + sqrtA2Alpha
                a1 = 2.0f * ((a - 1.0f) - (a + 1.0f) * cosW0)
                a2 = (a + 1.0f) - (a - 1.0f) * cosW0 - sqrtA2Alpha
            }
        }

        coefficients.b0 = b0 / a0
        coefficients.b1 = b1 / a0
        coefficients.b2 = b2 / a0
        coefficients.a1 = a1 / a0
        coefficients.a2 = a2 / a0
    }

    fun processMonoBlock(buffer: FloatArray, offset: Int, length: Int) {
        val b0 = coefficients.b0
        val b1 = coefficients.b1
        val b2 = coefficients.b2
        val a1 = coefficients.a1
        val a2 = coefficients.a2

        var lx1 = x1L
        var lx2 = x2L
        var ly1 = y1L
        var ly2 = y2L

        for (i in offset until offset + length) {
            val input = buffer[i]
            val output = b0 * input + b1 * lx1 + b2 * lx2 - a1 * ly1 - a2 * ly2
            lx2 = lx1
            lx1 = input
            ly2 = ly1
            ly1 = output
            buffer[i] = output
        }

        x1L = lx1
        x2L = lx2
        y1L = ly1
        y2L = ly2
    }

    fun processStereoBlock(buffer: FloatArray, offset: Int, length: Int) {
        val b0 = coefficients.b0
        val b1 = coefficients.b1
        val b2 = coefficients.b2
        val a1 = coefficients.a1
        val a2 = coefficients.a2

        var lx1 = x1L
        var lx2 = x2L
        var ly1 = y1L
        var ly2 = y2L

        var rx1 = x1R
        var rx2 = x2R
        var ry1 = y1R
        var ry2 = y2R

        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val inL = buffer[i]
            val outL = b0 * inL + b1 * lx1 + b2 * lx2 - a1 * ly1 - a2 * ly2
            lx2 = lx1
            lx1 = inL
            ly2 = ly1
            ly1 = outL
            buffer[i] = outL

            val inR = buffer[i + 1]
            val outR = b0 * inR + b1 * rx1 + b2 * rx2 - a1 * ry1 - a2 * ry2
            rx2 = rx1
            rx1 = inR
            ry2 = ry1
            ry1 = outR
            buffer[i + 1] = outR

            i += 2
        }

        x1L = lx1
        x2L = lx2
        y1L = ly1
        y2L = ly2

        x1R = rx1
        x2R = rx2
        y1R = ry1
        y2R = ry2
    }

    fun resetState() {
        x1L = 0f
        x2L = 0f
        y1L = 0f
        y2L = 0f
        x1R = 0f
        x2R = 0f
        y1R = 0f
        y2R = 0f
    }
}
