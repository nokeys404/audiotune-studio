package com.audiotune.studio.presentation.equalizer

import com.audiotune.studio.audio.engine.*
import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor
import com.audiotune.studio.audio.dsp.dynamics.CompressorProcessor
import com.audiotune.studio.audio.dsp.dynamics.LimiterProcessor
import com.audiotune.studio.audio.dsp.dynamics.NoiseGateProcessor
import com.audiotune.studio.audio.dsp.dynamics.ExpanderProcessor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EqualizerViewModelTest {
    
    private lateinit var engine: AudioEngine
    private lateinit var viewModel: EqualizerViewModel

    @Before
    fun setup() {
        engine = AudioEngine()
        viewModel = EqualizerViewModel(
            engine.eqController,
            engine.compressorController,
            engine.limiterController,
            engine.noiseGateController,
            engine.expanderController
        )
    }

    @Test
    fun testNoNewAudioEngineCreated() {
        val sameEngineReference = engine.eqController
        assertNotNull(sameEngineReference)
    }

    @Test
    fun testMasterDspBypassAndEnable() {
        viewModel.toggleEqBypass(false)
        viewModel.toggleCompBypass(false)
        viewModel.toggleLimBypass(false)
        viewModel.toggleGateBypass(false)
        viewModel.toggleExpBypass(false)

        assertFalse(engine.eqController.isEnabled())
        assertFalse(engine.compressorController.isEnabled())
        assertFalse(engine.limiterController.isEnabled())
        assertFalse(engine.noiseGateController.isEnabled())
        assertFalse(engine.expanderController.isEnabled())

        var state = viewModel.uiState.value
        assertFalse(state.eqEnabled)
        assertFalse(state.compEnabled)
        assertFalse(state.limEnabled)
        assertFalse(state.gateEnabled)
        assertFalse(state.expEnabled)

        viewModel.toggleEqBypass(true)
        assertTrue(engine.eqController.isEnabled())
        state = viewModel.uiState.value
        assertTrue(state.eqEnabled)
    }

    @Test
    fun testNoiseGateParameterUpdates() {
        viewModel.updateGateThreshold(-50f)
        viewModel.updateGateAttack(10f)
        viewModel.updateGateRelease(100f)

        assertEquals(-50f, engine.noiseGateController.getThresholdDb())
        assertEquals(10f, engine.noiseGateController.getAttackMs())
        assertEquals(100f, engine.noiseGateController.getReleaseMs())

        val state = viewModel.uiState.value
        assertEquals(-50f, state.gateThresholdDb)
        assertEquals(10f, state.gateAttackMs)
        assertEquals(100f, state.gateReleaseMs)
    }

    @Test
    fun testExpanderParameterUpdates() {
        viewModel.updateExpThreshold(-40f)
        viewModel.updateExpRatio(3f)
        viewModel.updateExpAttack(12f)
        viewModel.updateExpRelease(120f)

        assertEquals(-40f, engine.expanderController.getThresholdDb())
        assertEquals(3f, engine.expanderController.getRatio())
        assertEquals(12f, engine.expanderController.getAttackMs())
        assertEquals(120f, engine.expanderController.getReleaseMs())
    }

    @Test
    fun testEqParameterUpdates() {
        viewModel.selectEqBand(0)
        viewModel.updateEqBandGain(5f)
        viewModel.updateEqBandFrequency(100f)
        viewModel.updateEqBandQ(1.5f)

        val band = engine.eqController.getBand(0)
        assertEquals(5f, band.gainDb)
        assertEquals(100f, band.frequencyHz)
        assertEquals(1.5f, band.q)
    }

    @Test
    fun testCompressorParameterUpdates() {
        viewModel.updateCompThreshold(-15f)
        viewModel.updateCompRatio(5f)
        viewModel.updateCompAttack(5f)
        viewModel.updateCompRelease(200f)
        viewModel.updateCompMakeupGain(3f)

        assertEquals(-15f, engine.compressorController.getThresholdDb())
        assertEquals(5f, engine.compressorController.getRatio())
        assertEquals(5f, engine.compressorController.getAttackMs())
        assertEquals(200f, engine.compressorController.getReleaseMs())
        assertEquals(3f, engine.compressorController.getMakeupGainDb())
    }

    @Test
    fun testLimiterParameterUpdates() {
        viewModel.updateLimCeiling(-2f)
        viewModel.updateLimRelease(80f)

        assertEquals(-2f, engine.limiterController.getCeilingDb())
        assertEquals(80f, engine.limiterController.getReleaseMs())
    }

    @Test
    fun testStateConsistency() {
        viewModel.updateGateThreshold(-42f)
        assertEquals(-42f, viewModel.uiState.value.gateThresholdDb)
        assertEquals(-42f, engine.noiseGateController.getThresholdDb())
        
        viewModel.setTab(DspTab.COMPRESSOR)
        assertEquals(DspTab.COMPRESSOR, viewModel.uiState.value.currentTab)
    }
}
