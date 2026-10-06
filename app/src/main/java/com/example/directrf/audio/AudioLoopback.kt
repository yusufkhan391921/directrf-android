package com.example.directrf.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AudioLoopback {
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var job: Job? = null
    private var running = false

    fun start() {
        if (running) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (minBuffer <= 0) return

        record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBuffer * 2
        )

        val trackBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        track = AudioTrack(
            android.media.AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            audioFormat,
            trackBuffer * 2,
            AudioTrack.MODE_STREAM
        )

        record?.startRecording()
        track?.play()
        running = true

        job = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(minBuffer / 2)
            while (running) {
                val read = record?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    track?.write(buffer, 0, read)
                }
            }
        }
    }

    fun stop() {
        running = false
        job?.cancel()
        record?.stop()
        record?.release()
        track?.stop()
        track?.release()
        record = null
        track = null
    }
}
