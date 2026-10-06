package com.example.directrf.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.example.directrf.AudioServiceEvent
import com.example.directrf.data.preferences.DirectRfPreferences
import com.example.directrf.data.transport.Transport
import kotlinx.coroutines.flow.Flow

class CallControllerFactory(
    private val context: Context,
    private val transport: Transport,
    private val preferences: DirectRfPreferences,
    private val audioEvents: Flow<AudioServiceEvent>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CallController::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return CallController(context, transport, preferences, audioEvents) as T
    }
}