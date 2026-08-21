package com.audiotune.studio.ai

import com.audiotune.studio.domain.model.AudioPreset

/**
 * AI Audio Assistant interface stub.
 * Prepared for future stage AI EQ curve generation and acoustic mastering recommendations.
 */
interface AiAudioAssistant {
    suspend fun recommendEqualizerPreset(
        genre: String,
        listeningEnvironment: String
    ): AudioPreset?

    suspend fun analyzeAudioClarity(
        audioSampleStats: Map<String, Float>
    ): String
}
