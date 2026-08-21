package com.audiotune.studio.audio.dsp

/**
 * Dynamics Processor (Compressor / Limiter) stub.
 * Prepared for future audio mastering and peak limiting processing.
 */
class DynamicsProcessor {
    var thresholdDb: Float = -12f
    var ratio: Float = 4f
    var attackMs: Float = 10f
    var releaseMs: Float = 100f
    var makeupGainDb: Float = 0f
    var isLimiterEnabled: Boolean = true
}
