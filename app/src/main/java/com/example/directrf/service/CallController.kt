package com.example.directrf.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.directrf.data.rf.MockRfTransport
import com.example.directrf.data.rf.RfPacket
import com.example.directrf.domain.model.CallState
import com.example.directrf.domain.model.CallUiState
import com.example.directrf.domain.model.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallController : ViewModel() {
    private val mockTransport = MockRfTransport()
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        discoverNearbyDevices()
    }

    fun discoverNearbyDevices() {
        _uiState.value = CallUiState(
            callState = CallState.DISCOVERING,
            nearbyDevices = listOf(
                DeviceInfo("RF-A1F8", "Aarav", "-68 dBm"),
                DeviceInfo("RF-8D74", "Neha", "-72 dBm"),
                DeviceInfo("RF-C2B1", "Ishaan", "-75 dBm")
            )
        )
    }

    fun connectRf() {
        viewModelScope.launch {
            mockTransport.connect()
            _uiState.update { it.copy(rfLinkStatus = "RF Simulator active") }
            delay(600)
            _uiState.update { it.copy(callState = CallState.DEVICE_FOUND) }
        }
    }

    fun placeCall(targetId: String) {
        _uiState.update {
            it.copy(
                callState = CallState.CALL_REQUEST_SENT,
                remoteName = "Aarav",
                connectedSignal = "-68 dBm",
                rfLinkStatus = "Calling via RF simulator"
            )
        }

        viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(callState = CallState.RINGING) }
            delay(1200)
            _uiState.update { it.copy(callState = CallState.CALL_ACCEPTED) }
            delay(900)
            _uiState.update { it.copy(callState = CallState.ACTIVE_CALL, callDurationSeconds = 0L) }
            mockTransport.send(
                RfPacket(
                    packetType = "CALL_ACCEPT",
                    sessionId = "sim-001",
                    senderId = "RF-A1F8",
                    receiverId = _uiState.value.deviceId,
                    payload = "audio_ready"
                )
            )
        }
    }

    fun acceptCall() {
        _uiState.update { it.copy(callState = CallState.CALL_ACCEPTED) }
        viewModelScope.launch {
            delay(1100)
            _uiState.update { it.copy(callState = CallState.ACTIVE_CALL) }
        }
    }

    fun rejectCall() {
        _uiState.update { it.copy(callState = CallState.IDLE) }
    }

    fun endCall() {
        _uiState.update { it.copy(callState = CallState.ENDING) }
        viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(callState = CallState.IDLE, remoteName = "Aarav") }
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(muted = !it.muted) }
    }

    fun toggleSpeaker() {
        _uiState.update { it.copy(speakerOn = !it.speakerOn) }
    }

    fun tickTimer() {
        if (_uiState.value.callState == CallState.ACTIVE_CALL) {
            _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
        }
    }
}
