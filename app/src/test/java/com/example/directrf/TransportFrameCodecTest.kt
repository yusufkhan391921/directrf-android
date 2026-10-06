package com.example.directrf

import com.example.directrf.data.transport.CallSignal
import com.example.directrf.data.transport.AudioCodecConfiguration
import com.example.directrf.data.transport.EncodedAudioFrame
import com.example.directrf.data.transport.TransportFrame
import com.example.directrf.data.transport.TransportFrameCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransportFrameCodecTest {
    @Test
    fun opusConfiguration_roundTripsAllCodecSpecificData() {
        val configuration = AudioCodecConfiguration(
            csd0 = byteArrayOf(1, 2, 3),
            csd1 = byteArrayOf(4, 5),
            csd2 = byteArrayOf(6)
        )

        val decoded = TransportFrameCodec.decode(
            TransportFrameCodec.encodeAudioConfiguration(configuration)
        ) as TransportFrame.AudioConfiguration

        assertArrayEquals(configuration.csd0, decoded.configuration.csd0)
        assertArrayEquals(configuration.csd1, decoded.configuration.csd1)
        assertArrayEquals(configuration.csd2, decoded.configuration.csd2)
    }

    @Test
    fun audioFrame_roundTripsBinaryPayloadAndMetadata() {
        val frame = EncodedAudioFrame(91L, 123456L, byteArrayOf(0, 1, -1, 0x7f))

        val decoded = TransportFrameCodec.decode(TransportFrameCodec.encodeAudio(frame)) as TransportFrame.Audio

        assertEquals(frame.sequence, decoded.frame.sequence)
        assertEquals(frame.capturedAtMillis, decoded.frame.capturedAtMillis)
        assertArrayEquals(frame.encoded, decoded.frame.encoded)
    }

    @Test
    fun signal_roundTrips() {
        CallSignal.entries.forEach { signal ->
            assertEquals(
                TransportFrame.Signal(signal),
                TransportFrameCodec.decode(TransportFrameCodec.encodeSignal(signal))
            )
        }
    }

    @Test
    fun malformedFrame_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TransportFrameCodec.decode(byteArrayOf(0, 1, 2))
        }
    }
}
