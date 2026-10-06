package com.example.directrf.data.transport

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface TransportFrame {
    data class AudioConfiguration(val configuration: AudioCodecConfiguration) : TransportFrame
    data class Audio(val frame: EncodedAudioFrame) : TransportFrame
    data class Signal(val signal: CallSignal) : TransportFrame
}

object TransportFrameCodec {
    private const val MAGIC = 0x44524631
    private const val TYPE_AUDIO: Byte = 1
    private const val TYPE_SIGNAL: Byte = 2
    private const val TYPE_AUDIO_CONFIGURATION: Byte = 3
    private const val MAX_AUDIO_FRAME_BYTES = 4096
    private const val MAX_CSD_BYTES = 1024

    fun encodeAudioConfiguration(configuration: AudioCodecConfiguration): ByteArray {
        val csd0 = configuration.csd0
        val csd1 = configuration.csd1 ?: byteArrayOf()
        val csd2 = configuration.csd2 ?: byteArrayOf()
        require(csd0.isNotEmpty() && csd0.size <= MAX_CSD_BYTES)
        require(csd1.size <= MAX_CSD_BYTES && csd2.size <= MAX_CSD_BYTES)
        return ByteBuffer.allocate(11 + csd0.size + csd1.size + csd2.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(MAGIC)
            .put(TYPE_AUDIO_CONFIGURATION)
            .putShort(csd0.size.toShort())
            .putShort(csd1.size.toShort())
            .putShort(csd2.size.toShort())
            .put(csd0)
            .put(csd1)
            .put(csd2)
            .array()
    }

    fun encodeAudio(frame: EncodedAudioFrame): ByteArray {
        require(frame.encoded.isNotEmpty() && frame.encoded.size <= MAX_AUDIO_FRAME_BYTES)
        return ByteBuffer.allocate(4 + 1 + 8 + 8 + 2 + frame.encoded.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(MAGIC)
            .put(TYPE_AUDIO)
            .putLong(frame.sequence)
            .putLong(frame.capturedAtMillis)
            .putShort(frame.encoded.size.toShort())
            .put(frame.encoded)
            .array()
    }

    fun encodeSignal(signal: CallSignal): ByteArray = ByteBuffer.allocate(6)
        .order(ByteOrder.BIG_ENDIAN)
        .putInt(MAGIC)
        .put(TYPE_SIGNAL)
        .put(signal.ordinal.toByte())
        .array()

    fun decode(bytes: ByteArray): TransportFrame {
        require(bytes.size >= 5) { "Frame is shorter than its header" }
        val input = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        require(input.int == MAGIC) { "Unrecognized frame" }
        return when (val type = input.get()) {
            TYPE_AUDIO_CONFIGURATION -> {
                require(input.remaining() >= 6) { "Opus configuration header is incomplete" }
                val sizes = IntArray(3) { input.short.toInt() and 0xffff }
                require(sizes[0] in 1..MAX_CSD_BYTES && sizes.drop(1).all { it <= MAX_CSD_BYTES }) {
                    "Opus configuration size is invalid"
                }
                require(input.remaining() == sizes.sum()) { "Opus configuration length does not match" }
                val data = sizes.map { size -> ByteArray(size).also(input::get) }
                TransportFrame.AudioConfiguration(
                    AudioCodecConfiguration(data[0], data[1].takeIf { it.isNotEmpty() }, data[2].takeIf { it.isNotEmpty() })
                )
            }
            TYPE_AUDIO -> {
                require(input.remaining() >= 18) { "Audio frame header is incomplete" }
                val sequence = input.long
                val capturedAtMillis = input.long
                val frameSize = input.short.toInt() and 0xffff
                require(frameSize in 1..MAX_AUDIO_FRAME_BYTES && input.remaining() == frameSize) {
                    "Audio frame length is invalid"
                }
                val encoded = ByteArray(frameSize)
                input.get(encoded)
                TransportFrame.Audio(EncodedAudioFrame(sequence, capturedAtMillis, encoded))
            }
            TYPE_SIGNAL -> {
                require(input.remaining() == 1) { "Signal frame length is invalid" }
                val ordinal = input.get().toInt() and 0xff
                TransportFrame.Signal(CallSignal.entries.getOrNull(ordinal) ?: error("Unknown signal"))
            }
            else -> error("Unknown frame type: $type")
        }
    }
}
