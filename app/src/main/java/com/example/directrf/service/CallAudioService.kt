package com.example.directrf.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.directrf.AudioServiceEvent
import com.example.directrf.DirectRfApplication
import com.example.directrf.MainActivity
import com.example.directrf.R
import com.example.directrf.audio.OpusAudioEngine
import com.example.directrf.data.transport.TransportEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallAudioService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val failureReported = AtomicBoolean(false)
    private var audioEngine: OpusAudioEngine? = null
    private var receiveJob: Job? = null

    private val directRfApplication: DirectRfApplication
        get() = application as DirectRfApplication

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCallAudio()
            ACTION_SET_MUTED -> audioEngine?.setMuted(intent.getBooleanExtra(EXTRA_ENABLED, false))
            ACTION_SET_SPEAKER -> audioEngine?.setSpeakerEnabled(intent.getBooleanExtra(EXTRA_ENABLED, true))
            ACTION_STOP -> {
                stopCallAudio()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startCallAudio() {
        if (audioEngine != null) return
        var engine: OpusAudioEngine? = null
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            engine = OpusAudioEngine(this) { interrupted ->
                directRfApplication.reportAudioEvent(AudioServiceEvent.AudioFocusChanged(interrupted))
            }
            audioEngine = engine
            failureReported.set(false)

            receiveJob = serviceScope.launch(start = CoroutineStart.UNDISPATCHED) {
                directRfApplication.transport.events.collect { event ->
                    when (event) {
                        is TransportEvent.AudioConfigurationReceived -> {
                            runCatching { engine.configureIncomingAudio(event.configuration) }
                                .onFailure { reportAudioFailure(engine, it) }
                        }
                        is TransportEvent.AudioReceived -> engine.offerIncomingFrame(event.frame)
                        else -> Unit
                    }
                }
            }

            serviceScope.launch {
                try {
                    engine.start(
                        onCodecConfiguration = { configuration ->
                            directRfApplication.transport.sendAudioConfiguration(configuration)
                        },
                        onEncodedFrame = { frame ->
                            directRfApplication.transport.sendAudioFrame(frame)
                        },
                        onFailure = { error -> reportAudioFailure(engine, error) }
                    )
                    if (!failureReported.get()) directRfApplication.reportAudioEvent(AudioServiceEvent.Started)
                } catch (error: Exception) {
                    reportAudioFailure(engine, error)
                }
            }
        } catch (error: Exception) {
            if (engine != null) {
                reportAudioFailure(engine, error)
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                directRfApplication.reportAudioEvent(
                    AudioServiceEvent.Failed(error.message ?: "Microphone or Opus audio could not start")
                )
                stopSelf()
            }
        }
    }

    private fun reportAudioFailure(engine: OpusAudioEngine, error: Throwable) {
        if (!failureReported.compareAndSet(false, true)) return
        serviceScope.launch {
            if (audioEngine === engine) {
                engine.stop()
                audioEngine = null
                receiveJob?.cancel()
                receiveJob = null
                directRfApplication.reportAudioEvent(
                    AudioServiceEvent.Failed(error.message ?: "Microphone or Opus audio failed")
                )
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun stopCallAudio() {
        receiveJob?.cancel()
        receiveJob = null
        audioEngine?.stop()
        audioEngine = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        directRfApplication.reportAudioEvent(AudioServiceEvent.Stopped)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_direct_rf)
            .setContentTitle("DirectRF call active")
            .setContentText("Encrypted nearby voice connection")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps an active DirectRF voice call running" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopCallAudio()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "direct_rf_calls"
        private const val NOTIFICATION_ID = 41
        private const val ACTION_START = "com.example.directrf.action.START_CALL_AUDIO"
        private const val ACTION_SET_MUTED = "com.example.directrf.action.SET_MUTED"
        private const val ACTION_SET_SPEAKER = "com.example.directrf.action.SET_SPEAKER"
        private const val ACTION_STOP = "com.example.directrf.action.STOP_CALL_AUDIO"
        private const val EXTRA_ENABLED = "enabled"

        fun start(context: Context) {
            val intent = Intent(context, CallAudioService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun setMuted(context: Context, muted: Boolean) {
            context.startService(
                Intent(context, CallAudioService::class.java)
                    .setAction(ACTION_SET_MUTED)
                    .putExtra(EXTRA_ENABLED, muted)
            )
        }

        fun setSpeaker(context: Context, enabled: Boolean) {
            context.startService(
                Intent(context, CallAudioService::class.java)
                    .setAction(ACTION_SET_SPEAKER)
                    .putExtra(EXTRA_ENABLED, enabled)
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CallAudioService::class.java).setAction(ACTION_STOP))
        }
    }
}
