package com.audiotune.studio.playback

import com.audiotune.studio.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PlaybackManager stub.
 * Prepared for future stage ExoPlayer / Media3 / Local Audio Player integration.
 */
class PlaybackManager {
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    fun play(track: Track) {
        _playbackState.value = PlaybackState.Playing(track, 0L)
    }

    fun pause() {
        val current = _playbackState.value
        if (current is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Paused(current.track, current.positionMs)
        }
    }

    fun resume() {
        val current = _playbackState.value
        if (current is PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Playing(current.track, current.positionMs)
        }
    }

    fun stop() {
        _playbackState.value = PlaybackState.Idle
    }

    fun seekTo(positionMs: Long) {
        val current = _playbackState.value
        if (current is PlaybackState.Playing) {
            _playbackState.value = PlaybackState.Playing(current.track, positionMs)
        } else if (current is PlaybackState.Paused) {
            _playbackState.value = PlaybackState.Paused(current.track, positionMs)
        }
    }
}
