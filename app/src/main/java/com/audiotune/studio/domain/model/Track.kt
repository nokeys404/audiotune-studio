package com.audiotune.studio.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val audioUri: String = "",
    val sampleRate: Int = 48000,
    val bitRateKbps: Int = 320,
    val format: String = "FLAC",
    val playedAt: Long = 0L
)
