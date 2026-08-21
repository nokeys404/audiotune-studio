package com.audiotune.studio.audio.dsp.eq

class BiquadCoefficients {
    var b0: Float = 1f
    var b1: Float = 0f
    var b2: Float = 0f
    var a1: Float = 0f
    var a2: Float = 0f

    fun update(other: BiquadCoefficients) {
        this.b0 = other.b0
        this.b1 = other.b1
        this.b2 = other.b2
        this.a1 = other.a1
        this.a2 = other.a2
    }
}
