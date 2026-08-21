package com.audiotune.studio.presentation.equalizer

import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor
import com.audiotune.studio.audio.engine.EqController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EqualizerViewModelTest {

    private lateinit var eqProcessor: ParametricEqProcessor
    private lateinit var eqController: EqController
    private lateinit var viewModel: EqualizerViewModel

    @Before
    fun setup() {
        eqProcessor = ParametricEqProcessor()
        eqController = EqController(eqProcessor)
        viewModel = EqualizerViewModel(eqController)
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertTrue(state.isEnabled)
        assertEquals(10, state.bands.size)
        assertEquals(0, state.selectedBandIndex)
    }

    @Test
    fun testToggleBypass() {
        viewModel.toggleBypass(false)
        assertFalse(viewModel.uiState.value.isEnabled)
        assertFalse(eqController.isEnabled())

        viewModel.toggleBypass(true)
        assertTrue(viewModel.uiState.value.isEnabled)
        assertTrue(eqController.isEnabled())
    }

    @Test
    fun testSelectBand() {
        viewModel.selectBand(5)
        assertEquals(5, viewModel.uiState.value.selectedBandIndex)
    }

    @Test
    fun testUpdateBandGain() {
        viewModel.selectBand(2)
        viewModel.updateBandGain(5.5f)
        
        val state = viewModel.uiState.value
        assertEquals(5.5f, state.bands[2].gainDb, 0.01f)
        assertEquals(5.5f, eqController.getBand(2).gainDb, 0.01f)
    }

    @Test
    fun testUpdateBandFrequency() {
        viewModel.selectBand(3)
        viewModel.updateBandFrequency(300f)
        
        val state = viewModel.uiState.value
        assertEquals(300f, state.bands[3].frequencyHz, 0.01f)
        assertEquals(300f, eqController.getBand(3).frequencyHz, 0.01f)
    }

    @Test
    fun testUpdateBandQ() {
        viewModel.selectBand(4)
        viewModel.updateBandQ(2.5f)
        
        val state = viewModel.uiState.value
        assertEquals(2.5f, state.bands[4].q, 0.01f)
        assertEquals(2.5f, eqController.getBand(4).q, 0.01f)
    }

    @Test
    fun testApplyFlatPreset() {
        // Set some random gains
        viewModel.selectBand(0)
        viewModel.updateBandGain(10f)
        viewModel.selectBand(9)
        viewModel.updateBandGain(-10f)

        // Apply flat preset
        viewModel.applyFlatPreset()

        val state = viewModel.uiState.value
        for (i in 0 until 10) {
            assertEquals(0f, state.bands[i].gainDb, 0.01f)
            assertEquals(0f, eqController.getBand(i).gainDb, 0.01f)
        }
    }
}
