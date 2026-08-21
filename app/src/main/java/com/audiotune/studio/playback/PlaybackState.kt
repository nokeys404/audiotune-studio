package com.audiotune.studio.playback

import com.audiotune.studio.domain.model.Track

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = 0 // 0: Off, 1: One, 2: All
)

