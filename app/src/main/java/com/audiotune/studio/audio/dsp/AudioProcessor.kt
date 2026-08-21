package com.audiotune.studio.audio.dsp

import java.nio.ByteBuffer

/**
 * Base interface for processing audio buffers.
 */
interface AudioProcessor {
    /**
     * Processes the input buffer and returns the output buffer.
     */
    fun process(inputBuffer: ByteBuffer): ByteBuffer

    /**
     * Flushes any internal state.
     */
    fun flush()

    /**
     * Releases any resources.
     */
    fun release()
}
