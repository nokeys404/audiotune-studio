package com.audiotune.studio.di

import android.app.Application
import com.audiotune.studio.data.local.AudioTuneDatabase
import com.audiotune.studio.data.repository.AudioRepositoryImpl
import com.audiotune.studio.domain.repository.AudioRepository
import com.audiotune.studio.playback.PlaybackManager

object AppContainer {
    private var application: Application? = null
    lateinit var playbackManager: PlaybackManager
        private set
    lateinit var audioRepository: AudioRepository
        private set

    fun init(app: Application) {
        application = app
        val database = AudioTuneDatabase.getInstance(app)
        audioRepository = AudioRepositoryImpl(database.trackDao())
        playbackManager = PlaybackManager(app)
    }
}
