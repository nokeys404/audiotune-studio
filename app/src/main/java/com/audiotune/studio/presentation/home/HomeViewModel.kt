package com.audiotune.studio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiotune.studio.data.repository.AudioRepositoryImpl
import com.audiotune.studio.domain.model.Track
import com.audiotune.studio.domain.repository.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val audioRepository: AudioRepository = AudioRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecentlyPlayed()
    }

    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            audioRepository.getRecentlyPlayedTracks().collect { tracks ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        recentlyPlayed = tracks,
                        selectedTrack = currentState.selectedTrack ?: tracks.firstOrNull()
                    )
                }
            }
        }
    }

    fun onTrackSelected(track: Track) {
        _uiState.update { it.copy(selectedTrack = track) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
