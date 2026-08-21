package com.audiotune.studio.audio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Media3AudioProcessorAdapter(
    private val audioEngine: AudioEngine
) : AudioProcessor {

    private var pendingAudioFormat = AudioFormat.NOT_SET
    private var activeAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        pendingAudioFormat = inputAudioFormat
        
        audioEngine.configure(inputAudioFormat.sampleRate.toFloat(), inputAudioFormat.channelCount)
        
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return pendingAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return

        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        buffer.put(inputBuffer)
        buffer.flip()

        // Process audio in place
        audioEngine.processAudio(buffer)

        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
        audioEngine.flush()
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        activeAudioFormat = pendingAudioFormat
        audioEngine.flush()
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        pendingAudioFormat = AudioFormat.NOT_SET
        activeAudioFormat = AudioFormat.NOT_SET
        audioEngine.release()
    }
}
