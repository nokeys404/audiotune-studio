package com.audiotune.studio.audio.dsp

/**
 * An individual Digital Signal Processor module within the DspPipeline.
 */
interface DspProcessor : AudioProcessor {
    val id: String
    var isEnabled: Boolean
    fun configure(sampleRate: Float, channels: Int)
}
