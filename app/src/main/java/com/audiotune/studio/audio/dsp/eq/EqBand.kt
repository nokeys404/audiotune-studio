package com.audiotune.studio.audio.dsp.eq

data class EqBand(
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 1f,
    val type: EqFilterType = EqFilterType.PEAKING,
    val isEnabled: Boolean = true
)
