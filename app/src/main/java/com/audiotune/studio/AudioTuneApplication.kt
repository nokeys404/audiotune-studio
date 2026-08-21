package com.audiotune.studio

import android.app.Application
import com.audiotune.studio.di.AppContainer

class AudioTuneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
