package com.example.directrf.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import com.example.directrf.data.transport.AudioCodecConfiguration
import com.example.directrf.data.transport.EncodedAudioFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OpusAudioEngine(
    context: Context,
    private val onAudioFocusInterruption: (Boolean) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)
    private val jitterBuffer = JitterBuffer(startupFrames = 3, capacityFrames = 8)
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private var encoder: MediaCodec? = null
    @Volatile
    private var decoder: MediaCodec? = null
    private var decoderConfigured = false
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var focusRequest: AudioFocusRequest? = null
    private var captureJob: Job? = null
    private var playoutJob: Job? = null
    @Volatile
    private var muted = false
    @Volatile
    private var focusInterrupted = false
    private var speakerEnabled = true
    private var frameSequence = 0L

    @Synchronized
    fun start(
        onCodecConfiguration: suspend (AudioCodecConfiguration) -> Unit,
        onEncodedFrame: suspend (EncodedAudioFrame) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (!running.compareAndSet(false, true)) return
        try {
            requireOpusEncoder()
            configureAudioFocus()
            val record = createRecorder().also { recorder = it }
            val audioTrack = createAudioTrack().also { track = it }
            val opusEncoder = createEncoder().also { encoder = it }
            echoCanceler = if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
            } else null
            noiseSuppressor = if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
            } else null

            opusEncoder.start()
            record.startRecording()
            audioTrack.play()
            routeOutput(speakerEnabled)
            captureJob = scope.launch {
                try {
                    captureAndEncode(record, opusEncoder, onCodecConfiguration, onEncodedFrame)
                } catch (error: Exception) {
                    if (running.get()) onFailure(error)
                }
            }
            playoutJob = scope.launch {
                try {
                    decodeAndPlay(audioTrack)
                } catch (error: Exception) {
                    if (running.get()) onFailure(error)
                }
            }
        } catch (error: Throwable) {
            stop()
            throw IllegalStateException(error.message ?: "Unable to start Opus audio", error)
        }
    }

    fun offerIncomingFrame(frame: EncodedAudioFrame) {
        if (running.get()) jitterBuffer.offer(frame)
    }

    @Synchronized
    fun configureIncomingAudio(configuration: AudioCodecConfiguration) {
        if (!running.get() || decoderConfigured) return
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNEL_COUNT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4_096)
            setByteBuffer("csd-0", ByteBuffer.wrap(configuration.csd0))
            configuration.csd1?.let { setByteBuffer("csd-1", ByteBuffer.wrap(it)) }
            configuration.csd2?.let { setByteBuffer("csd-2", ByteBuffer.wrap(it)) }
        }
        val opusDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        opusDecoder.configure(format, null, null, 0)
        opusDecoder.start()
        decoder = opusDecoder
        decoderConfigured = true
    }

    fun setMuted(value: Boolean) {
        muted = value
    }

    fun setSpeakerEnabled(value: Boolean) {
        speakerEnabled = value
        routeOutput(value)
    }

    @Synchronized
    fun stop() {
        if (!running.getAndSet(false)) return
        captureJob?.cancel()
        playoutJob?.cancel()
        captureJob = null
        playoutJob = null
        runCatching { recorder?.stop() }
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.stop() }
        runCatching { encoder?.stop() }
        runCatching { decoder?.stop() }
        recorder?.release()
        track?.release()
        encoder?.release()
        decoder?.release()
        echoCanceler?.release()
        noiseSuppressor?.release()
        recorder = null
        track = null
        encoder = null
        decoder = null
        decoderConfigured = false
        echoCanceler = null
        noiseSuppressor = null
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
        jitterBuffer.clear()
        scope.cancel()
    }

    private suspend fun captureAndEncode(
        record: AudioRecord,
        codec: MediaCodec,
        onCodecConfiguration: suspend (AudioCodecConfiguration) -> Unit,
        onEncodedFrame: suspend (EncodedAudioFrame) -> Unit
    ) {
        val samples = ShortArray(FRAME_SAMPLES)
        val pcm = ByteArray(FRAME_SAMPLES * BYTES_PER_SAMPLE)
        val info = MediaCodec.BufferInfo()
        var configurationSent = false
        while (scope.isActive && running.get()) {
            val sampleCount = record.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
            if (sampleCount <= 0) continue
            val pcmBuffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
            for (index in 0 until sampleCount) {
                pcmBuffer.putShort(if (muted || focusInterrupted) 0 else samples[index])
            }

            val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                val input = codec.getInputBuffer(inputIndex) ?: continue
                input.clear()
                input.put(pcm, 0, sampleCount * BYTES_PER_SAMPLE)
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    sampleCount * BYTES_PER_SAMPLE,
                    System.nanoTime() / 1_000L,
                    0
                )
            }

            while (true) {
                val outputIndex = codec.dequeueOutputBuffer(info, 0)
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    configurationSent = sendCodecConfiguration(codec.outputFormat, onCodecConfiguration)
                    continue
                }
                if (outputIndex < 0) break
                val output = codec.getOutputBuffer(outputIndex)
                if (output != null && info.size > 0) {
                    output.position(info.offset)
                    output.limit(info.offset + info.size)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        if (!configurationSent) {
                            val codecData = ByteArray(info.size)
                            output.get(codecData)
                            onCodecConfiguration(AudioCodecConfiguration(codecData))
                            configurationSent = true
                        }
                    } else {
                        if (!configurationSent) {
                            configurationSent = sendCodecConfiguration(codec.outputFormat, onCodecConfiguration)
                        }
                        check(configurationSent) { "Opus encoder did not provide decoder configuration" }
                        if (configurationSent) {
                            val encoded = ByteArray(info.size)
                            output.get(encoded)
                            onEncodedFrame(EncodedAudioFrame(frameSequence++, System.currentTimeMillis(), encoded))
                        }
                    }
                }
                codec.releaseOutputBuffer(outputIndex, false)
            }
        }
    }

    private suspend fun sendCodecConfiguration(
        format: MediaFormat,
        onCodecConfiguration: suspend (AudioCodecConfiguration) -> Unit
    ): Boolean {
        val csd0 = format.getByteBuffer("csd-0")?.let(::copyRemaining) ?: return false
        val csd1 = format.getByteBuffer("csd-1")?.let(::copyRemaining)
        val csd2 = format.getByteBuffer("csd-2")?.let(::copyRemaining)
        onCodecConfiguration(AudioCodecConfiguration(csd0, csd1, csd2))
        return true
    }

    private fun copyRemaining(source: ByteBuffer): ByteArray =
        ByteArray(source.remaining()).also(source.duplicate()::get)

    private suspend fun decodeAndPlay(audioTrack: AudioTrack) {
        val info = MediaCodec.BufferInfo()
        while (scope.isActive && running.get()) {
            val codec = decoder
            if (codec == null) {
                delay(FRAME_DURATION_MS)
                continue
            }
            val frame = jitterBuffer.poll()
            if (frame == null) {
                delay(FRAME_DURATION_MS)
                continue
            }
            val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                val input = codec.getInputBuffer(inputIndex)
                if (input != null) {
                    input.clear()
                    input.put(frame.encoded)
                    codec.queueInputBuffer(
                        inputIndex,
                        0,
                        frame.encoded.size,
                        frame.capturedAtMillis * 1_000L,
                        0
                    )
                }
            }
            while (true) {
                val outputIndex = codec.dequeueOutputBuffer(info, 0)
                if (outputIndex < 0) break
                val output = codec.getOutputBuffer(outputIndex)
                if (output != null && info.size > 0) {
                    output.position(info.offset)
                    output.limit(info.offset + info.size)
                    val decoded = ByteArray(info.size)
                    output.get(decoded)
                    if (focusInterrupted) decoded.fill(0)
                    audioTrack.write(decoded, 0, decoded.size, AudioTrack.WRITE_BLOCKING)
                }
                codec.releaseOutputBuffer(outputIndex, false)
            }
            delay(FRAME_DURATION_MS)
        }
    }

    private fun createRecorder(): AudioRecord {
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        check(minimum > 0) { "This device cannot record 48 kHz mono audio" }
        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minimum, FRAME_SAMPLES * BYTES_PER_SAMPLE * 4))
            .build()
            .also { check(it.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not be initialized" } }
    }

    private fun createAudioTrack(): AudioTrack {
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minimum = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        check(minimum > 0) { "This device cannot play 48 kHz mono audio" }
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minimum, FRAME_SAMPLES * BYTES_PER_SAMPLE * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { check(it.state == AudioTrack.STATE_INITIALIZED) { "Speaker output could not be initialized" } }
    }

    private fun createEncoder(): MediaCodec {
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, CHANNEL_COUNT).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, OPUS_BITRATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FRAME_SAMPLES * BYTES_PER_SAMPLE * 2)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return codec
    }

    private fun requireOpusEncoder() {
        val supported = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, ignoreCase = true) }
        }
        check(supported) { "This Android device does not expose an Opus encoder" }
    }

    private fun configureAudioFocus() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            focusInterrupted = false
                            onAudioFocusInterruption(false)
                        }
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            focusInterrupted = true
                            onAudioFocusInterruption(true)
                        }
                    }
                }
                .build()
            focusRequest = request
            check(audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                "Audio focus is unavailable"
            }
        }
    }

    private fun routeOutput(useSpeaker: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val deviceType = if (useSpeaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == deviceType }
            if (device != null) audioManager.setCommunicationDevice(device)
        } else {
            @Suppress("DEPRECATION")
            run { audioManager.isSpeakerphoneOn = useSpeaker }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNEL_COUNT = 1
        const val OPUS_BITRATE = 24_000
        const val FRAME_SAMPLES = 960
        const val BYTES_PER_SAMPLE = 2
        const val FRAME_DURATION_MS = 20L
        const val CODEC_TIMEOUT_US = 10_000L
    }
}
