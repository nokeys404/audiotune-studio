package com.audiotune.studio.presentation.equalizer

import androidx.lifecycle.ViewModel
import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.engine.CompressorController
import com.audiotune.studio.audio.engine.EqController
import com.audiotune.studio.audio.engine.ExpanderController
import com.audiotune.studio.audio.engine.LimiterController
import com.audiotune.studio.audio.engine.NoiseGateController
import com.audiotune.studio.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DspTab {
    NOISE_GATE, EXPANDER, EQ, COMPRESSOR, LIMITER
}

data class DspUiState(
    val currentTab: DspTab = DspTab.EQ,
    // EQ
    val eqEnabled: Boolean = true,
    val eqBands: List<EqBand> = emptyList(),
    val selectedBandIndex: Int = 0,
    // Compressor
    val compEnabled: Boolean = false,
    val compThresholdDb: Float = -20f,
    val compRatio: Float = 4f,
    val compAttackMs: Float = 10f,
    val compReleaseMs: Float = 100f,
    val compMakeupGainDb: Float = 0f,
    // Limiter
    val limEnabled: Boolean = false,
    val limCeilingDb: Float = -1f,
    val limReleaseMs: Float = 50f,
    // Noise Gate
    val gateEnabled: Boolean = false,
    val gateThresholdDb: Float = -60f,
    val gateAttackMs: Float = 5f,
    val gateReleaseMs: Float = 50f,
    // Expander
    val expEnabled: Boolean = false,
    val expThresholdDb: Float = -40f,
    val expRatio: Float = 2f,
    val expAttackMs: Float = 5f,
    val expReleaseMs: Float = 50f
)

class EqualizerViewModel(
    private val eqController: EqController = AppContainer.audioEngine.eqController,
    private val compressorController: CompressorController = AppContainer.audioEngine.compressorController,
    private val limiterController: LimiterController = AppContainer.audioEngine.limiterController,
    private val noiseGateController: NoiseGateController = AppContainer.audioEngine.noiseGateController,
    private val expanderController: ExpanderController = AppContainer.audioEngine.expanderController
) : ViewModel() {

    private val _uiState = MutableStateFlow(DspUiState())
    val uiState: StateFlow<DspUiState> = _uiState.asStateFlow()

    init {
        loadStateFromControllers()
    }

    private fun loadStateFromControllers() {
        val bands = List(10) { eqController.getBand(it) }
        _uiState.update { state ->
            state.copy(
                eqEnabled = eqController.isEnabled(),
                eqBands = bands,
                compEnabled = compressorController.isEnabled(),
                compThresholdDb = compressorController.getThresholdDb(),
                compRatio = compressorController.getRatio(),
                compAttackMs = compressorController.getAttackMs(),
                compReleaseMs = compressorController.getReleaseMs(),
                compMakeupGainDb = compressorController.getMakeupGainDb(),
                limEnabled = limiterController.isEnabled(),
                limCeilingDb = limiterController.getCeilingDb(),
                limReleaseMs = limiterController.getReleaseMs(),
                gateEnabled = noiseGateController.isEnabled(),
                gateThresholdDb = noiseGateController.getThresholdDb(),
                gateAttackMs = noiseGateController.getAttackMs(),
                gateReleaseMs = noiseGateController.getReleaseMs(),
                expEnabled = expanderController.isEnabled(),
                expThresholdDb = expanderController.getThresholdDb(),
                expRatio = expanderController.getRatio(),
                expAttackMs = expanderController.getAttackMs(),
                expReleaseMs = expanderController.getReleaseMs()
            )
        }
    }

    fun setTab(tab: DspTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // EQ
    fun toggleEqBypass(enabled: Boolean) {
        eqController.setEnabled(enabled)
        _uiState.update { it.copy(eqEnabled = enabled) }
    }

    fun selectEqBand(index: Int) {
        if (index in 0 until 10) {
            _uiState.update { it.copy(selectedBandIndex = index) }
        }
    }

    fun updateEqBandGain(gainDb: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandGain(index, gainDb)
        updateBandInState(index)
    }

    fun updateEqBandFrequency(frequencyHz: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandFrequency(index, frequencyHz)
        updateBandInState(index)
    }

    fun updateEqBandQ(q: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandQ(index, q)
        updateBandInState(index)
    }

    fun toggleEqBandEnabled(index: Int) {
        val band = eqController.getBand(index)
        eqController.updateBand(index, band.copy(isEnabled = !band.isEnabled))
        updateBandInState(index)
    }

    fun applyEqFlatPreset() {
        for (i in 0 until 10) {
            eqController.updateBandGain(i, 0f)
        }
        loadStateFromControllers()
    }

    private fun updateBandInState(index: Int) {
        val newBand = eqController.getBand(index)
        _uiState.update { state ->
            val newBands = state.eqBands.toMutableList()
            newBands[index] = newBand
            state.copy(eqBands = newBands)
        }
    }

    // Compressor
    fun toggleCompBypass(enabled: Boolean) {
        compressorController.setEnabled(enabled)
        _uiState.update { it.copy(compEnabled = enabled) }
    }
    fun updateCompThreshold(value: Float) {
        compressorController.setThresholdDb(value)
        _uiState.update { it.copy(compThresholdDb = value) }
    }
    fun updateCompRatio(value: Float) {
        compressorController.setRatio(value)
        _uiState.update { it.copy(compRatio = value) }
    }
    fun updateCompAttack(value: Float) {
        compressorController.setAttackMs(value)
        _uiState.update { it.copy(compAttackMs = value) }
    }
    fun updateCompRelease(value: Float) {
        compressorController.setReleaseMs(value)
        _uiState.update { it.copy(compReleaseMs = value) }
    }
    fun updateCompMakeupGain(value: Float) {
        compressorController.setMakeupGainDb(value)
        _uiState.update { it.copy(compMakeupGainDb = value) }
    }

    // Limiter
    fun toggleLimBypass(enabled: Boolean) {
        limiterController.setEnabled(enabled)
        _uiState.update { it.copy(limEnabled = enabled) }
    }
    fun updateLimCeiling(value: Float) {
        limiterController.setCeilingDb(value)
        _uiState.update { it.copy(limCeilingDb = value) }
    }
    fun updateLimRelease(value: Float) {
        limiterController.setReleaseMs(value)
        _uiState.update { it.copy(limReleaseMs = value) }
    }

    // Noise Gate
    fun toggleGateBypass(enabled: Boolean) {
        noiseGateController.setEnabled(enabled)
        _uiState.update { it.copy(gateEnabled = enabled) }
    }
    fun updateGateThreshold(value: Float) {
        noiseGateController.setThresholdDb(value)
        _uiState.update { it.copy(gateThresholdDb = value) }
    }
    fun updateGateAttack(value: Float) {
        noiseGateController.setAttackMs(value)
        _uiState.update { it.copy(gateAttackMs = value) }
    }
    fun updateGateRelease(value: Float) {
        noiseGateController.setReleaseMs(value)
        _uiState.update { it.copy(gateReleaseMs = value) }
    }

    // Expander
    fun toggleExpBypass(enabled: Boolean) {
        expanderController.setEnabled(enabled)
        _uiState.update { it.copy(expEnabled = enabled) }
    }
    fun updateExpThreshold(value: Float) {
        expanderController.setThresholdDb(value)
        _uiState.update { it.copy(expThresholdDb = value) }
    }
    fun updateExpRatio(value: Float) {
        expanderController.setRatio(value)
        _uiState.update { it.copy(expRatio = value) }
    }
    fun updateExpAttack(value: Float) {
        expanderController.setAttackMs(value)
        _uiState.update { it.copy(expAttackMs = value) }
    }
    fun updateExpRelease(value: Float) {
        expanderController.setReleaseMs(value)
        _uiState.update { it.copy(expReleaseMs = value) }
    }
}
