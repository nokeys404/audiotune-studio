package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.DspPipeline
import java.nio.ByteBuffer

/**
 * Audio Engine responsible for managing the real-time DSP pipeline.
 *
 * INTEGRATION POINT:
 * To safely integrate this with Media3 / ExoPlayer without breaking the current architecture,
 * this class should be wrapped in a custom implementation of `androidx.media3.common.audio.AudioProcessor`.
 * 
 * The custom ExoPlayer AudioProcessor would then be provided via a custom `RenderersFactory` 
 * when building the `ExoPlayer` instance in `PlaybackService`:
 * 
 * ```
 * val renderersFactory = object : DefaultRenderersFactory(context) {
 *     override fun buildAudioSink(
 *         context: Context,
 *         enableFloatOutput: Boolean,
 *         enableAudioTrackPlaybackParams: Boolean
 *     ): AudioSink {
 *         return DefaultAudioSink.Builder(context)
 *             .setAudioProcessors(arrayOf(ExoPlayerAudioProcessorAdapter(audioEngine)))
 *             .build()
 *     }
 * }
 * exoPlayer = ExoPlayer.Builder(context, renderersFactory).build()
 * ```
 */
class AudioEngine {
    val dspPipeline = DspPipeline()

    fun processAudio(inputBuffer: ByteBuffer): ByteBuffer {
        return dspPipeline.process(inputBuffer)
    }

    fun flush() {
        dspPipeline.flush()
    }

    fun release() {
        dspPipeline.release()
    }
}
