package com.audiotune.studio.presentation.equalizer

import androidx.lifecycle.ViewModel
import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.engine.EqController
import com.audiotune.studio.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqUiState(
    val isEnabled: Boolean = true,
    val bands: List<EqBand> = emptyList(),
    val selectedBandIndex: Int = 0
)

class EqualizerViewModel(
    private val eqController: EqController = AppContainer.audioEngine.eqController
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EqUiState())
    val uiState: StateFlow<EqUiState> = _uiState.asStateFlow()

    init {
        loadStateFromController()
    }

    private fun loadStateFromController() {
        val bands = List(10) { eqController.getBand(it) }
        _uiState.update { 
            it.copy(
                isEnabled = eqController.isEnabled(),
                bands = bands
            )
        }
    }

    fun toggleBypass(enabled: Boolean) {
        eqController.setEnabled(enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun selectBand(index: Int) {
        if (index in 0 until 10) {
            _uiState.update { it.copy(selectedBandIndex = index) }
        }
    }

    fun updateBandGain(gainDb: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandGain(index, gainDb)
        updateBandInState(index)
    }

    fun updateBandFrequency(frequencyHz: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandFrequency(index, frequencyHz)
        updateBandInState(index)
    }

    fun updateBandQ(q: Float) {
        val index = _uiState.value.selectedBandIndex
        eqController.updateBandQ(index, q)
        updateBandInState(index)
    }

    fun toggleBandEnabled(index: Int) {
        val band = eqController.getBand(index)
        eqController.updateBand(index, band.copy(isEnabled = !band.isEnabled))
        updateBandInState(index)
    }

    fun applyFlatPreset() {
        for (i in 0 until 10) {
            eqController.updateBandGain(i, 0f)
        }
        loadStateFromController()
    }

    private fun updateBandInState(index: Int) {
        val newBand = eqController.getBand(index)
        _uiState.update { state ->
            val newBands = state.bands.toMutableList()
            newBands[index] = newBand
            state.copy(bands = newBands)
        }
    }
}

