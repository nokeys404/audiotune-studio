package com.audiotune.studio.audio.dsp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

data class PipelineState(
    val processorIds: List<String> = emptyList(),
    val enabledMap: Map<String, Boolean> = emptyMap()
)

/**
 * Orchestrates a sequential chain of DspProcessors.
 */
class DspPipeline : AudioProcessor {
    private val processors = mutableListOf<DspProcessor>()
    private val _pipelineState = MutableStateFlow(PipelineState())
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    @Synchronized
    fun addProcessor(processor: DspProcessor) {
        processors.add(processor)
        updateState()
    }

    @Synchronized
    fun removeProcessor(processorId: String) {
        processors.removeAll { it.id == processorId }
        updateState()
    }

    @Synchronized
    fun setProcessorEnabled(processorId: String, enabled: Boolean) {
        processors.find { it.id == processorId }?.isEnabled = enabled
        updateState()
    }

    @Synchronized
    fun clear() {
        processors.clear()
        updateState()
    }

    @Synchronized
    fun configure(sampleRate: Float, channels: Int) {
        processors.forEach { it.configure(sampleRate, channels) }
    }

    @Synchronized
    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        var currentBuffer = inputBuffer
        for (processor in processors) {
            if (processor.isEnabled) {
                currentBuffer = processor.process(currentBuffer)
            }
        }
        return currentBuffer
    }

    @Synchronized
    override fun flush() {
        processors.forEach { it.flush() }
    }

    @Synchronized
    override fun release() {
        processors.forEach { it.release() }
        clear()
    }

    private fun updateState() {
        _pipelineState.value = PipelineState(
            processorIds = processors.map { it.id },
            enabledMap = processors.associate { it.id to it.isEnabled }
        )
    }
}
