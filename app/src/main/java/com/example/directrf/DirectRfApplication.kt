package com.example.directrf

import android.app.Application
import com.example.directrf.data.preferences.SharedPreferencesDirectRfPreferences
import com.example.directrf.data.transport.NearbyConnectionsTransport
import com.example.directrf.data.transport.Transport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DirectRfApplication : Application() {
    val preferences by lazy {
        SharedPreferencesDirectRfPreferences(getSharedPreferences("direct_rf_preferences", MODE_PRIVATE))
    }
    val transport: Transport by lazy { NearbyConnectionsTransport(this) }

    private val mutableAudioEvents = MutableSharedFlow<AudioServiceEvent>(extraBufferCapacity = 8)
    val audioEvents = mutableAudioEvents.asSharedFlow()

    fun reportAudioEvent(event: AudioServiceEvent) {
        mutableAudioEvents.tryEmit(event)
    }
}

sealed interface AudioServiceEvent {
    data object Started : AudioServiceEvent
    data object Stopped : AudioServiceEvent
    data class Failed(val reason: String) : AudioServiceEvent
    data class AudioFocusChanged(val interrupted: Boolean) : AudioServiceEvent
}
