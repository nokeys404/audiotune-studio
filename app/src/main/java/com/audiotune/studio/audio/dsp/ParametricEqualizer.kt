package com.audiotune.studio.audio.dsp

import com.audiotune.studio.domain.model.EqualizerBand

/**
 * Parametric Equalizer DSP filter chain stub.
 * Prepared for future stage biquad filter calculations and real-time coefficient updates.
 */
class ParametricEqualizer {
    private var isEnabled: Boolean = false
    private val bands = mutableListOf<EqualizerBand>()

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun updateBands(newBands: List<EqualizerBand>) {
        bands.clear()
        bands.addAll(newBands)
    }

    fun getBands(): List<EqualizerBand> = bands.toList()
}
