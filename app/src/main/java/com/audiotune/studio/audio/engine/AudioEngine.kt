package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.DspPipeline
import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor
import com.audiotune.studio.audio.dsp.dynamics.CompressorProcessor
import com.audiotune.studio.audio.dsp.dynamics.LimiterProcessor
import com.audiotune.studio.audio.dsp.dynamics.NoiseGateProcessor
import com.audiotune.studio.audio.dsp.dynamics.ExpanderProcessor
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

    private val noiseGateProcessor = NoiseGateProcessor()
    val noiseGateController = NoiseGateController(noiseGateProcessor)

    private val expanderProcessor = ExpanderProcessor()
    val expanderController = ExpanderController(expanderProcessor)

    private val eqProcessor = ParametricEqProcessor()
    val eqController = EqController(eqProcessor)

    private val compressorProcessor = CompressorProcessor()
    val compressorController = CompressorController(compressorProcessor)

    private val limiterProcessor = LimiterProcessor()
    val limiterController = LimiterController(limiterProcessor)

    init {
        dspPipeline.addProcessor(noiseGateProcessor)
        dspPipeline.addProcessor(expanderProcessor)
        dspPipeline.addProcessor(eqProcessor)
        dspPipeline.addProcessor(compressorProcessor)
        dspPipeline.addProcessor(limiterProcessor)
    }

    fun configure(sampleRate: Float, channels: Int) {
        dspPipeline.configure(sampleRate, channels)
    }

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
