package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.dynamics.NoiseGateProcessor

class NoiseGateController(private val processor: NoiseGateProcessor) {
    fun setEnabled(enabled: Boolean) { processor.isEnabled = enabled }
    fun isEnabled(): Boolean = processor.isEnabled
    fun setThresholdDb(thresholdDb: Float) { processor.thresholdDb = thresholdDb }
    fun getThresholdDb(): Float = processor.thresholdDb
    fun setAttackMs(attackMs: Float) { processor.attackMs = attackMs }
    fun getAttackMs(): Float = processor.attackMs
    fun setHoldMs(holdMs: Float) { processor.holdMs = holdMs }
    fun getHoldMs(): Float = processor.holdMs
    fun setReleaseMs(releaseMs: Float) { processor.releaseMs = releaseMs }
    fun getReleaseMs(): Float = processor.releaseMs
    fun setRangeDb(rangeDb: Float) { processor.rangeDb = rangeDb }
    fun getRangeDb(): Float = processor.rangeDb
}
