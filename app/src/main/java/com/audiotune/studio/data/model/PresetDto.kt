package com.audiotune.studio.data.model

data class PresetDto(
    val id: String,
    val name: String,
    val description: String = "",
    val isCustom: Boolean = false,
    val bandsGain: List<Float> = emptyList()
)
