package com.audiotune.studio.playback

import com.audiotune.studio.domain.model.Track

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class Playing(val track: Track, val positionMs: Long) : PlaybackState()
    data class Paused(val track: Track, val positionMs: Long) : PlaybackState()
    data class Buffering(val track: Track) : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
