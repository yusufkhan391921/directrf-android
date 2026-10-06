package com.example.directrf.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.directrf.AudioServiceEvent
import com.example.directrf.data.preferences.DirectRfPreferences
import com.example.directrf.data.transport.CallSignal
import com.example.directrf.data.transport.DiscoveredPeer
import com.example.directrf.data.transport.Transport
import com.example.directrf.data.transport.TransportEvent
import com.example.directrf.data.transport.TransportStatus
import com.example.directrf.domain.model.AppTab
import com.example.directrf.domain.model.CallEvent
import com.example.directrf.domain.model.CallRecord
import com.example.directrf.domain.model.CallState
import com.example.directrf.domain.model.CallStateMachine
import com.example.directrf.domain.model.CallUiState
import com.example.directrf.domain.model.DeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallController(
    context: Context,
    private val transport: Transport,
    private val preferences: DirectRfPreferences,
    audioEvents: Flow<AudioServiceEvent>
) : ViewModel() {
    private val appContext = context.applicationContext
    private val savedPreferences = preferences.load()
    private val _uiState = MutableStateFlow(
        CallUiState(
            displayName = savedPreferences.displayName ?: "Operator",
            profileComplete = !savedPreferences.displayName.isNullOrBlank(),
            callHistory = savedPreferences.callHistory
        )
    )
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    private var callStartedAtMillis: Long? = null

    init {
        viewModelScope.launch {
            transport.events.collect(::handleTransportEvent)
        }
        viewModelScope.launch {
            transport.status.collect { status ->
                if (status == TransportStatus.DISCONNECTED && _uiState.value.callState == CallState.CONNECTED) {
                    _uiState.update { it.copy(statusMessage = "Not connected") }
                }
            }
        }
        viewModelScope.launch {
            audioEvents.collect { event ->
                when (event) {
                    AudioServiceEvent.Started -> _uiState.update {
                        it.copy(
                            callState = CallStateMachine.transition(it.callState, CallEvent.AUDIO_STARTED),
                            statusMessage = "Encrypted voice call active",
                            errorMessage = null
                        )
                    }
                    AudioServiceEvent.Stopped -> _uiState.update { it.copy(audioInterrupted = false) }
                    is AudioServiceEvent.Failed -> {
                        _uiState.update {
                            it.copy(
                                callState = CallStateMachine.transition(it.callState, CallEvent.FAILURE),
                                statusMessage = "Audio unavailable",
                                errorMessage = event.reason
                            )
                        }
                    }
                    is AudioServiceEvent.AudioFocusChanged -> _uiState.update {
                        it.copy(
                            audioInterrupted = event.interrupted,
                            statusMessage = if (event.interrupted) "Audio paused by another app"
                            else if (it.callState == CallState.IN_CALL) "Voice call resumed" else it.statusMessage
                        )
                    }
                }
            }
        }
    }

    fun saveProfile(displayName: String) {
        val normalized = displayName.trim().take(32)
        if (normalized.isBlank()) return
        preferences.saveProfile(normalized)
        _uiState.update { it.copy(displayName = normalized, profileComplete = true, statusMessage = "Profile saved") }
    }

    fun setSelectedTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun discover() {
        val name = _uiState.value.displayName
        _uiState.update {
            it.copy(
                callState = CallStateMachine.transition(it.callState, CallEvent.START_DISCOVERY),
                errorMessage = null,
                statusMessage = "Looking for nearby DirectRF phones"
            )
        }
        viewModelScope.launch {
            runCatching { transport.discover(name) }
                .onFailure { showError(it.message ?: "Nearby discovery could not start") }
        }
    }

    fun connect(peer: DeviceInfo) {
        _uiState.update {
            it.copy(
                callState = CallStateMachine.transition(it.callState, CallEvent.CONNECT_PEER),
                selectedEndpointId = peer.endpointId,
                remoteName = peer.name,
                errorMessage = null,
                statusMessage = "Requesting secure connection to ${peer.name}"
            )
        }
        viewModelScope.launch {
            runCatching { transport.connect(peer.endpointId, _uiState.value.displayName) }
                .onFailure { showError(it.message ?: "Could not connect to ${peer.name}") }
        }
    }

    fun confirmPairing(endpointId: String, accepted: Boolean) {
        viewModelScope.launch {
            runCatching { transport.confirmPairing(endpointId, accepted) }
                .onFailure { showError(it.message ?: "Could not confirm peer identity") }
            if (!accepted) {
                _uiState.update {
                    it.copy(
                        callState = CallStateMachine.transition(it.callState, CallEvent.PAIRING_REJECTED),
                        pairingEndpointId = null,
                        pairingPeerName = null,
                        pairingDigits = null,
                        statusMessage = "Pairing declined"
                    )
                }
            }
        }
    }

    fun placeCall() {
        val endpointId = _uiState.value.selectedEndpointId
        if (endpointId == null || _uiState.value.callState != CallState.CONNECTED) {
            showError("Connect and verify a nearby phone before calling")
            return
        }
        _uiState.update {
            it.copy(
                callState = CallStateMachine.transition(it.callState, CallEvent.CALL_SENT),
                statusMessage = "Calling ${it.remoteName}",
                errorMessage = null
            )
        }
        viewModelScope.launch {
            runCatching { transport.sendSignal(CallSignal.CALL) }
                .onFailure { showError(it.message ?: "Call request could not be sent") }
        }
    }

    fun acceptCall() {
        if (_uiState.value.callState != CallState.INCOMING_CALL) return
        _uiState.update {
            it.copy(
                callState = CallStateMachine.transition(it.callState, CallEvent.ACCEPT_CALL),
                statusMessage = "Starting secure audio"
            )
        }
        viewModelScope.launch {
            runCatching {
                transport.sendSignal(CallSignal.ACCEPT)
                startAudioService()
            }.onFailure { showError(it.message ?: "Could not accept call") }
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            runCatching { transport.sendSignal(CallSignal.DECLINE) }
            _uiState.update {
                it.copy(
                    callState = CallStateMachine.transition(it.callState, CallEvent.CALL_DECLINED),
                    statusMessage = "Call declined"
                )
            }
        }
    }

    fun endCall() {
        val current = _uiState.value
        if (current.callState == CallState.IN_CALL) recordCall(current, "Completed")
        val hadCall = current.callState in setOf(
            CallState.IN_CALL,
            CallState.OUTGOING_CALL,
            CallState.INCOMING_CALL,
            CallState.AUDIO_CONNECTING
        )
        if (hadCall) CallAudioService.stop(appContext)
        viewModelScope.launch {
            if (hadCall) runCatching { transport.sendSignal(CallSignal.END) }
            _uiState.update {
                it.copy(
                    callState = if (hadCall) CallStateMachine.transition(it.callState, CallEvent.END_CALL) else it.callState,
                    callDurationSeconds = 0,
                    statusMessage = "Call ended"
                )
            }
            callStartedAtMillis = null
        }
    }

    fun toggleMute() {
        val muted = !_uiState.value.muted
        _uiState.update { it.copy(muted = muted) }
        CallAudioService.setMuted(appContext, muted)
    }

    fun toggleSpeaker() {
        val enabled = !_uiState.value.speakerEnabled
        _uiState.update { it.copy(speakerEnabled = enabled) }
        CallAudioService.setSpeaker(appContext, enabled)
    }

    fun disconnect() {
        CallAudioService.stop(appContext)
        viewModelScope.launch {
            runCatching { transport.disconnect() }
            _uiState.update {
                it.copy(
                    callState = CallState.IDLE,
                    selectedEndpointId = null,
                    remoteName = null,
                    nearbyDevices = emptyList(),
                    statusMessage = "Disconnected",
                    errorMessage = null
                )
            }
        }
    }

    fun tickTimer() {
        if (_uiState.value.callState == CallState.IN_CALL) {
            _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
        }
    }

    private suspend fun startAudioService() {
        CallAudioService.start(appContext)
        callStartedAtMillis = System.currentTimeMillis()
        _uiState.update { it.copy(statusMessage = "Starting encrypted audio") }
    }

    private fun handleTransportEvent(event: TransportEvent) {
        when (event) {
            is TransportEvent.PeerFound -> updatePeer(event.peer, connected = false)
            is TransportEvent.PeerLost -> {
                if (_uiState.value.selectedEndpointId == event.endpointId) CallAudioService.stop(appContext)
                _uiState.update { current ->
                    val peers = current.nearbyDevices.filterNot { it.endpointId == event.endpointId }
                    if (current.selectedEndpointId == event.endpointId) {
                        current.copy(
                            nearbyDevices = peers,
                            callState = CallStateMachine.transition(current.callState, CallEvent.PEER_LOST),
                            statusMessage = "Nearby peer lost",
                            errorMessage = "Peer went out of range or disconnected. Discover and reconnect."
                        )
                    } else current.copy(nearbyDevices = peers)
                }
            }
            is TransportEvent.PairingRequested -> _uiState.update {
                it.copy(
                    callState = CallStateMachine.transition(it.callState, CallEvent.PAIRING_REQUESTED),
                    pairingEndpointId = event.peer.endpointId,
                    pairingPeerName = event.peer.displayName,
                    pairingDigits = event.authenticationDigits,
                    selectedEndpointId = event.peer.endpointId,
                    remoteName = event.peer.displayName,
                    statusMessage = "Compare the code on both phones"
                )
            }
            is TransportEvent.Connected -> {
                updatePeer(event.peer, connected = true)
                _uiState.update {
                    it.copy(
                        callState = CallStateMachine.transition(it.callState, CallEvent.CONNECTION_ESTABLISHED),
                        selectedEndpointId = event.peer.endpointId,
                        remoteName = event.peer.displayName,
                        pairingEndpointId = null,
                        pairingPeerName = null,
                        pairingDigits = null,
                        statusMessage = "Secure peer connected",
                        errorMessage = null
                    )
                }
            }
            is TransportEvent.Disconnected -> {
                CallAudioService.stop(appContext)
                _uiState.update {
                    it.copy(
                        callState = CallStateMachine.transition(it.callState, CallEvent.PEER_LOST),
                        statusMessage = "Peer disconnected",
                        errorMessage = "Connection lost. Return to Devices to reconnect."
                    )
                }
            }
            is TransportEvent.AudioConfigurationReceived,
            is TransportEvent.AudioReceived -> Unit
            is TransportEvent.SignalReceived -> handleCallSignal(event.signal)
            is TransportEvent.Failure -> showError(event.message)
        }
    }

    private fun handleCallSignal(signal: CallSignal) {
        when (signal) {
            CallSignal.CALL -> _uiState.update {
                it.copy(
                    callState = CallStateMachine.transition(it.callState, CallEvent.CALL_RECEIVED),
                    statusMessage = "Incoming call from ${it.remoteName}",
                    errorMessage = null
                )
            }
            CallSignal.ACCEPT -> {
                if (_uiState.value.callState == CallState.OUTGOING_CALL) {
                    _uiState.update {
                        it.copy(
                            callState = CallStateMachine.transition(it.callState, CallEvent.REMOTE_ACCEPTED),
                            statusMessage = "Peer accepted; starting audio"
                        )
                    }
                    viewModelScope.launch {
                        runCatching { startAudioService() }
                            .onFailure { showError(it.message ?: "Audio could not start") }
                    }
                }
            }
            CallSignal.DECLINE -> _uiState.update {
                it.copy(
                    callState = CallStateMachine.transition(it.callState, CallEvent.CALL_DECLINED),
                    statusMessage = "Peer declined the call"
                )
            }
            CallSignal.END -> {
                val current = _uiState.value
                if (current.callState == CallState.IN_CALL) recordCall(current, "Completed")
                CallAudioService.stop(appContext)
                _uiState.update {
                    it.copy(
                        callState = CallStateMachine.transition(it.callState, CallEvent.END_CALL),
                        callDurationSeconds = 0,
                        statusMessage = "Peer ended the call"
                    )
                }
                callStartedAtMillis = null
            }
        }
    }

    private fun updatePeer(peer: DiscoveredPeer, connected: Boolean) {
        val device = DeviceInfo(peer.endpointId, peer.displayName, connected)
        _uiState.update { current ->
            val peers = current.nearbyDevices.filterNot { it.endpointId == peer.endpointId } + device
            current.copy(
                nearbyDevices = peers,
                callState = if (!connected && current.callState == CallState.DISCOVERING) {
                    CallStateMachine.transition(current.callState, CallEvent.PEER_FOUND)
                } else current.callState,
                statusMessage = if (connected) "Secure peer connected" else "Found ${peer.displayName}"
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                callState = CallStateMachine.transition(it.callState, CallEvent.FAILURE),
                statusMessage = "Connection error",
                errorMessage = message
            )
        }
    }

    private fun recordCall(state: CallUiState, outcome: String) {
        val now = System.currentTimeMillis()
        val record = CallRecord(
            remoteName = state.remoteName ?: "Nearby peer",
            endedAtMillis = now,
            durationSeconds = callStartedAtMillis?.let { ((now - it) / 1000L).coerceAtLeast(0L) }
                ?: state.callDurationSeconds,
            outcome = outcome
        )
        preferences.addCallRecord(record)
        _uiState.update { it.copy(callHistory = listOf(record) + it.callHistory) }
    }
}
