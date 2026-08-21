package com.audiotune.studio.domain.model

data class EqualizerBand(
    val index: Int,
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val qFactor: Float = 1.414f,
    val type: BandType = BandType.PEAKING
)

enum class BandType {
    LOW_SHELF,
    PEAKING,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS
}
