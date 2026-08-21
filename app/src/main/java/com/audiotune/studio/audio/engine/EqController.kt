package com.audiotune.studio.audio.engine

import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.audio.dsp.eq.ParametricEqProcessor

class EqController(private val eqProcessor: ParametricEqProcessor) {
    
    fun setEnabled(enabled: Boolean) {
        eqProcessor.isEnabled = enabled
    }

    fun isEnabled(): Boolean = eqProcessor.isEnabled

    fun updateBand(index: Int, band: EqBand) {
        eqProcessor.updateBand(index, band)
    }

    fun getBand(index: Int): EqBand {
        return eqProcessor.getBand(index)
    }

    fun updateBandFrequency(index: Int, frequencyHz: Float) {
        val band = eqProcessor.getBand(index)
        eqProcessor.updateBand(index, band.copy(frequencyHz = frequencyHz))
    }

    fun updateBandGain(index: Int, gainDb: Float) {
        val band = eqProcessor.getBand(index)
        eqProcessor.updateBand(index, band.copy(gainDb = gainDb))
    }

    fun updateBandQ(index: Int, q: Float) {
        val band = eqProcessor.getBand(index)
        eqProcessor.updateBand(index, band.copy(q = q))
    }
}
