package com.example.directrf

import com.example.directrf.audio.JitterBuffer
import com.example.directrf.data.transport.EncodedAudioFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JitterBufferTest {
    @Test
    fun waitsForStartupAndReordersPackets() {
        val buffer = JitterBuffer(startupFrames = 3)
        val first = frame(1)
        val second = frame(2)
        val third = frame(3)

        assertTrue(buffer.offer(first))
        assertTrue(buffer.offer(third))
        assertNull(buffer.poll())
        assertTrue(buffer.offer(second))

        assertEquals(1L, buffer.poll()?.sequence)
        assertEquals(2L, buffer.poll()?.sequence)
        assertEquals(3L, buffer.poll()?.sequence)
    }

    @Test
    fun missingPacketAdvancesAfterOnePlayoutInterval() {
        val buffer = JitterBuffer(startupFrames = 2)
        buffer.offer(frame(4))
        buffer.offer(frame(5))
        assertEquals(4L, buffer.poll()?.sequence)
        assertEquals(5L, buffer.poll()?.sequence)
        buffer.offer(frame(7))

        assertNull(buffer.poll())
        assertEquals(7L, buffer.poll()?.sequence)
    }

    @Test
    fun duplicateAndLateFramesAreDropped() {
        val buffer = JitterBuffer(startupFrames = 1)
        val frame = frame(8)
        assertTrue(buffer.offer(frame))
        assertFalse(buffer.offer(frame(8)))
        assertEquals(8L, buffer.poll()?.sequence)
        assertFalse(buffer.offer(frame))
    }

    private fun frame(sequence: Long) = EncodedAudioFrame(sequence, sequence * 20, byteArrayOf(sequence.toByte()))
}
