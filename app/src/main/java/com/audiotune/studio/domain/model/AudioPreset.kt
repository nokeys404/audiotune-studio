package com.audiotune.studio.domain.model

data class AudioPreset(
    val id: String,
    val name: String,
    val description: String = "",
    val isCustom: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostDb: Float = 0f,
    val trebleBoostDb: Float = 0f,
    val preampDb: Float = 0f
)
