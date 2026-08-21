package com.audiotune.studio.audio.analyzer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fast Fourier Transform / Real-Time Spectrum Analyzer stub.
 * Prepared for future stage audio visualizer feeds.
 */
class SpectrumAnalyzer {
    private val _frequencyMagnitudes = MutableStateFlow(FloatArray(64) { 0f })
    val frequencyMagnitudes: Flow<FloatArray> = _frequencyMagnitudes.asStateFlow()

    fun updateFftData(magnitudes: FloatArray) {
        _frequencyMagnitudes.value = magnitudes
    }
}
