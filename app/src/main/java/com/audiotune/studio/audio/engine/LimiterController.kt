package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.dynamics.LimiterProcessor

class LimiterController(private val processor: LimiterProcessor) {
    fun setEnabled(enabled: Boolean) {
        processor.isEnabled = enabled
    }

    fun isEnabled(): Boolean = processor.isEnabled

    fun setCeilingDb(ceilingDb: Float) {
        processor.ceilingDb = ceilingDb
    }

    fun getCeilingDb(): Float = processor.ceilingDb

    fun setReleaseMs(releaseMs: Float) {
        processor.releaseMs = releaseMs
    }

    fun getReleaseMs(): Float = processor.releaseMs
}
