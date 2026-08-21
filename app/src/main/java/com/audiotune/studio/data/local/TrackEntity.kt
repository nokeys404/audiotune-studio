package com.audiotune.studio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUri: String,
    val sampleRate: Int,
    val bitRateKbps: Int,
    val format: String,
    val playedAt: Long
)
