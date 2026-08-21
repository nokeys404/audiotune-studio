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

    companion object {
        // Explicitly defined order of DSP processing
        val EXPECTED_ORDER = listOf(
            "noise_gate",
            "expander",
            "parametric_eq",
            "compressor",
            "limiter"
        )
    }

    private val processorsMap = mutableMapOf<String, DspProcessor>()
    
    @Volatile
    private var orderedProcessors = emptyArray<DspProcessor>()

    private val _pipelineState = MutableStateFlow(PipelineState())
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    @Synchronized
    fun addProcessor(processor: DspProcessor) {
        processorsMap[processor.id] = processor
        rebuildArray()
        updateState()
    }

    @Synchronized
    fun removeProcessor(processorId: String) {
        processorsMap.remove(processorId)
        rebuildArray()
        updateState()
    }

    @Synchronized
    fun setProcessorEnabled(processorId: String, enabled: Boolean) {
        processorsMap[processorId]?.isEnabled = enabled
        updateState()
    }

    @Synchronized
    fun clear() {
        processorsMap.clear()
        rebuildArray()
        updateState()
    }

    @Synchronized
    fun configure(sampleRate: Float, channels: Int) {
        // Configure using the deterministic ordered array
        val procs = orderedProcessors
        for (i in procs.indices) {
            procs[i].configure(sampleRate, channels)
        }
    }

    private fun rebuildArray() {
        val list = mutableListOf<DspProcessor>()
        // Add known processors in explicit order
        for (id in EXPECTED_ORDER) {
            processorsMap[id]?.let { list.add(it) }
        }
        // Add any unknown processors at the end
        for ((id, processor) in processorsMap) {
            if (id !in EXPECTED_ORDER) {
                list.add(processor)
            }
        }
        orderedProcessors = list.toTypedArray()
    }

    // No @Synchronized here to avoid blocking real-time audio thread
    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        var currentBuffer = inputBuffer
        val procs = orderedProcessors
        for (i in procs.indices) {
            val processor = procs[i]
            if (processor.isEnabled) {
                currentBuffer = processor.process(currentBuffer)
            }
        }
        return currentBuffer
    }

    @Synchronized
    override fun flush() {
        val procs = orderedProcessors
        for (i in procs.indices) {
            procs[i].flush()
        }
    }

    @Synchronized
    override fun release() {
        val procs = orderedProcessors
        for (i in procs.indices) {
            procs[i].release()
        }
        clear()
    }

    private fun updateState() {
        _pipelineState.value = PipelineState(
            processorIds = orderedProcessors.map { it.id },
            enabledMap = processorsMap.mapValues { it.value.isEnabled }
        )
    }
}
