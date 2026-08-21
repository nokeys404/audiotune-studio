package com.audiotune.studio.domain.repository

import com.audiotune.studio.domain.model.AudioPreset
import com.audiotune.studio.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun getRecentlyPlayedTracks(): Flow<List<Track>>
    fun getLibraryTracks(): Flow<List<Track>>
    fun getPresets(): Flow<List<AudioPreset>>
    suspend fun saveTrackToHistory(track: Track)
    suspend fun savePreset(preset: AudioPreset)
}
