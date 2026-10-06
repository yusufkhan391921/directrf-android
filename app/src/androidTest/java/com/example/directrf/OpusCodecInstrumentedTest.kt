package com.example.directrf

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class OpusCodecInstrumentedTest {
    @Test
    fun platformOpusEncoderProducesAudioFrames() {
        val (frame, _) = encodeSampleFrame()
        assertTrue("Opus encoder did not emit an audio packet", frame.isNotEmpty())
    }

    @Test
    fun platformOpusDecoderConsumesEncodedFrame() {
        val (encodedFrame, encoderOutputFormat) = encodeSampleFrame()
        val decoderFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48_000, 1).apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4_096)
            listOf("csd-0", "csd-1", "csd-2").forEach { key ->
                encoderOutputFormat.getByteBuffer(key)?.let { setByteBuffer(key, it.duplicate()) }
            }
        }
        assertNotNull("Opus encoder must provide decoder setup data", decoderFormat.getByteBuffer("csd-0"))

        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        try {
            decoder.configure(decoderFormat, null, null, 0)
            decoder.start()
            val info = MediaCodec.BufferInfo()
            var decodedBytes = 0
            val deadline = System.currentTimeMillis() + 3_000
            var queued = 0
            while (decodedBytes == 0 && System.currentTimeMillis() < deadline) {
                val inputIndex = decoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0 && queued < 10) {
                    val input = decoder.getInputBuffer(inputIndex)!!
                    input.clear()
                    input.put(encodedFrame)
                    decoder.queueInputBuffer(inputIndex, 0, encodedFrame.size, queued * 20_000L, 0)
                    queued++
                }
                val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)
                if (outputIndex >= 0) {
                    decodedBytes += info.size
                    decoder.releaseOutputBuffer(outputIndex, false)
                }
            }
            assertTrue("Opus decoder did not produce PCM", decodedBytes > 0)
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
        }
    }

    private fun encodeSampleFrame(): Pair<ByteArray, MediaFormat> {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48_000, 1).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 24_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4_096)
        }
        val encoderName = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .firstOrNull { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, true) }
            }
            ?.name
        assertNotNull("An Opus encoder is required for calls", encoderName)

        val encoder = MediaCodec.createByCodecName(encoderName!!)
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            val info = MediaCodec.BufferInfo()
            var frame: ByteArray? = null
            var outputFormat: MediaFormat? = null
            val deadline = System.currentTimeMillis() + 3_000
            var submittedFrames = 0

            while (frame == null && System.currentTimeMillis() < deadline) {
                val inputIndex = encoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0 && submittedFrames < 10) {
                    val input = encoder.getInputBuffer(inputIndex)!!
                    input.clear()
                    val pcm = ByteBuffer.allocate(960 * 2).order(ByteOrder.LITTLE_ENDIAN)
                    repeat(960) { pcm.putShort(0) }
                    input.put(pcm.array())
                    encoder.queueInputBuffer(inputIndex, 0, pcm.capacity(), submittedFrames * 20_000L, 0)
                    submittedFrames++
                }

                when (val outputIndex = encoder.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = encoder.outputFormat
                    else -> if (outputIndex >= 0) {
                        val output = encoder.getOutputBuffer(outputIndex)
                        if (output != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            frame = ByteArray(info.size)
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            output.get(frame)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            assertTrue("Opus encoder did not emit an audio packet", frame?.isNotEmpty() == true)
            val configuredOutput = outputFormat
                ?: error("Opus encoder did not publish its output format")
            assertNotNull("Opus encoder did not publish decoder setup data", configuredOutput.getByteBuffer("csd-0"))
            return frame!! to configuredOutput
        } finally {
            runCatching { encoder.stop() }
            encoder.release()
        }
    }
}
