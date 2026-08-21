package com.audiotune.studio.presentation.home

import com.audiotune.studio.domain.model.Track

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentlyPlayed: List<Track> = emptyList(),
    val sampleRate: Int = 48000,
    val bitDepth: Int = 24,
    val engineActive: Boolean = true,
    val selectedTrack: Track? = null,
    val userMessage: String? = null
)
