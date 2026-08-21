package com.audiotune.studio.audio.engine

/**
 * AudioEngineManager interface stub.
 * Prepared for future audio rendering, low-latency OpenSL ES/Oboe/AAudio integration.
 */
interface AudioEngineManager {
    fun initialize(sampleRate: Int, bufferSize: Int): Boolean
    fun startProcessing()
    fun stopProcessing()
    fun release()
    fun isRunning(): Boolean
}
