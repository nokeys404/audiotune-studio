package com.audiotune.studio.presentation.music

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audiotune.studio.di.AppContainer
import com.audiotune.studio.domain.model.Track
import com.audiotune.studio.domain.repository.AudioRepository
import com.audiotune.studio.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val audioRepository = AppContainer.audioRepository
    val playbackManager = AppContainer.playbackManager

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioRepository.getLibraryTracks().collect { tracks ->
                _uiState.update { it.copy(tracks = tracks) }
            }
        }
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
            }
        }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newTracks = uris.mapNotNull { uri ->
                createTrackFromUri(context, uri)
            }
            if (newTracks.isNotEmpty()) {
                val currentTracks = _uiState.value.tracks.toMutableList()
                currentTracks.addAll(0, newTracks)
                // In a real app we would save to repository/database here.
                _uiState.update { it.copy(tracks = currentTracks) }
                // Persist new tracks to database
                newTracks.forEach { track ->
                    audioRepository.saveTrackToHistory(track)
                }
            }
        }
    }

    private fun createTrackFromUri(context: Context, uri: Uri): Track? {
        var title = "Unknown Title"
        var artist = "Unknown Artist"
        var durationMs = 0L

        // Try to get filename
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    val displayName = cursor.getString(displayNameIndex)
                    title = displayName.substringBeforeLast(".")
                }
            }
        }

        // Try to get metadata
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { durationMs = it }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return Track(
            id = UUID.randomUUID().toString(),
            title = title,
            artist = artist,
            durationMs = durationMs,
            audioUri = uri.toString()
        )
    }

    fun playTrack(track: Track) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOf(track)
        if (index >= 0) {
            playbackManager.setPlaylist(tracks, index)
        } else {
            playbackManager.playTrack(track)
        }
    }
}

data class MusicUiState(
    val tracks: List<Track> = emptyList(),
    val playbackState: com.audiotune.studio.playback.PlaybackState = com.audiotune.studio.playback.PlaybackState()
)
