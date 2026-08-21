package com.audiotune.studio.audio.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class DspPipelineTest {
    private lateinit var pipeline: DspPipeline

    class TestProcessor(override val id: String, private val multiplier: Byte) : DspProcessor {
        override var isEnabled: Boolean = true
        var flushCalled = false
        var releaseCalled = false

        override fun process(inputBuffer: ByteBuffer): ByteBuffer {
            val capacity = inputBuffer.capacity()
            val outputBuffer = ByteBuffer.allocate(capacity)
            for (i in 0 until capacity) {
                outputBuffer.put((inputBuffer.get(i) * multiplier).toByte())
            }
            outputBuffer.flip()
            return outputBuffer
        }

        override fun flush() { flushCalled = true }
        override fun release() { releaseCalled = true }
    }

    @Before
    fun setup() {
        pipeline = DspPipeline()
    }

    @Test
    fun testEmptyPipeline() {
        val input = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val output = pipeline.process(input)
        
        assertEquals(1.toByte(), output.get(0))
        assertEquals(2.toByte(), output.get(1))
        assertEquals(3.toByte(), output.get(2))
    }

    @Test
    fun testSingleProcessor() {
        pipeline.addProcessor(TestProcessor("proc1", 2))
        val input = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val output = pipeline.process(input)
        
        assertEquals(2.toByte(), output.get(0))
        assertEquals(4.toByte(), output.get(1))
        assertEquals(6.toByte(), output.get(2))
    }

    @Test
    fun testMultipleProcessorsOrdering() {
        // proc1 multiplies by 2
        pipeline.addProcessor(TestProcessor("proc1", 2))
        // proc2 multiplies by 3
        pipeline.addProcessor(TestProcessor("proc2", 3))
        
        val input = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val output = pipeline.process(input)
        
        // 1 * 2 * 3 = 6
        assertEquals(6.toByte(), output.get(0))
        // 2 * 2 * 3 = 12
        assertEquals(12.toByte(), output.get(1))
        // 3 * 2 * 3 = 18
        assertEquals(18.toByte(), output.get(2))
    }

    @Test
    fun testEnableDisableProcessor() {
        val proc1 = TestProcessor("proc1", 2)
        val proc2 = TestProcessor("proc2", 3)
        pipeline.addProcessor(proc1)
        pipeline.addProcessor(proc2)
        
        pipeline.setProcessorEnabled("proc1", false)
        
        val input = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val output = pipeline.process(input)
        
        // proc1 is disabled, so only proc2 (multiplies by 3) runs
        assertEquals(3.toByte(), output.get(0))
        assertEquals(6.toByte(), output.get(1))
        assertEquals(9.toByte(), output.get(2))
    }

    @Test
    fun testPipelineLifecycle() {
        val proc1 = TestProcessor("proc1", 2)
        pipeline.addProcessor(proc1)
        
        pipeline.flush()
        assertTrue(proc1.flushCalled)
        
        pipeline.release()
        assertTrue(proc1.releaseCalled)
        assertTrue(pipeline.pipelineState.value.processorIds.isEmpty())
    }
}
