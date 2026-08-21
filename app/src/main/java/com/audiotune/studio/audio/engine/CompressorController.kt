package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.dynamics.CompressorProcessor

class CompressorController(private val processor: CompressorProcessor) {
    fun setEnabled(enabled: Boolean) {
        processor.isEnabled = enabled
    }

    fun isEnabled(): Boolean = processor.isEnabled

    fun setThresholdDb(thresholdDb: Float) {
        processor.thresholdDb = thresholdDb
    }

    fun getThresholdDb(): Float = processor.thresholdDb

    fun setRatio(ratio: Float) {
        processor.ratio = ratio
    }

    fun getRatio(): Float = processor.ratio

    fun setAttackMs(attackMs: Float) {
        processor.attackMs = attackMs
    }

    fun getAttackMs(): Float = processor.attackMs

    fun setReleaseMs(releaseMs: Float) {
        processor.releaseMs = releaseMs
    }

    fun getReleaseMs(): Float = processor.releaseMs

    fun setMakeupGainDb(makeupGainDb: Float) {
        processor.makeupGainDb = makeupGainDb
    }

    fun getMakeupGainDb(): Float = processor.makeupGainDb
}
