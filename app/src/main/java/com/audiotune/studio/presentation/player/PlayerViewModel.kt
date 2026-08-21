package com.audiotune.studio.presentation.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiotune.studio.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val playbackManager = AppContainer.playbackManager

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
            }
        }
    }

    fun play() = playbackManager.play()
    fun pause() = playbackManager.pause()
    fun next() = playbackManager.next()
    fun previous() = playbackManager.previous()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun toggleRepeat() = playbackManager.toggleRepeat()
}

data class PlayerUiState(
    val playbackState: com.audiotune.studio.playback.PlaybackState = com.audiotune.studio.playback.PlaybackState()
)
