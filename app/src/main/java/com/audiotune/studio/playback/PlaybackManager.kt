package com.audiotune.studio.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.audiotune.studio.domain.model.Track
import com.audiotune.studio.domain.repository.AudioRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager(
    private val context: Context,
    private val audioRepository: AudioRepository
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    
    private val playlist = mutableListOf<Track>()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            updateState()
            startProgressUpdate()
        }, MoreExecutors.directExecutor())
    }
    
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateState()
            mediaItem?.mediaId?.let { trackId ->
                scope.launch {
                    audioRepository.markTrackAsPlayed(trackId, System.currentTimeMillis())
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) {
                startProgressUpdate()
            } else {
                progressJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updateState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updateState()
        }
    }

    private fun updateState() {
        mediaController?.let { player ->
            val trackId = player.currentMediaItem?.mediaId
            val currentTrack = playlist.find { it.id == trackId }
            _playbackState.update {
                it.copy(
                    currentTrack = currentTrack,
                    isPlaying = player.isPlaying,
                    playbackPositionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L),
                    shuffleModeEnabled = player.shuffleModeEnabled,
                    repeatMode = player.repeatMode
                )
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.update {
                            it.copy(
                                playbackPositionMs = player.currentPosition,
                                durationMs = player.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun playTrack(track: Track) {
        if (!playlist.contains(track)) {
            playlist.add(track)
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()
        
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        playlist.clear()
        playlist.addAll(tracks)
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.audioUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems, startIndex, 0)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.update { it.copy(playbackPositionMs = positionMs) }
    }

    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun toggleRepeat() {
        mediaController?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
        }
    }

    fun release() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        progressJob?.cancel()
        scope.cancel()
    }
}
