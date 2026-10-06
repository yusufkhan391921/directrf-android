package com.example.directrf.domain.model

enum class CallState {
    IDLE,
    DISCOVERING,
    PEER_AVAILABLE,
    CONNECTING,
    PAIRING,
    CONNECTED,
    OUTGOING_CALL,
    INCOMING_CALL,
    AUDIO_CONNECTING,
    IN_CALL,
    RECONNECTING,
    ERROR
}

enum class AppTab {
    HOME,
    DEVICES,
    HISTORY
}

data class CallRecord(
    val remoteName: String,
    val endedAtMillis: Long,
    val durationSeconds: Long,
    val outcome: String
)

data class DeviceInfo(
    val endpointId: String,
    val name: String,
    val connected: Boolean = false
)

data class CallUiState(
    val callState: CallState = CallState.IDLE,
    val displayName: String = "Operator",
    val profileComplete: Boolean = false,
    val selectedTab: AppTab = AppTab.HOME,
    val nearbyDevices: List<DeviceInfo> = emptyList(),
    val selectedEndpointId: String? = null,
    val remoteName: String? = null,
    val muted: Boolean = false,
    val speakerEnabled: Boolean = false,
    val audioInterrupted: Boolean = false,
    val callDurationSeconds: Long = 0L,
    val pairingEndpointId: String? = null,
    val pairingPeerName: String? = null,
    val pairingDigits: String? = null,
    val callHistory: List<CallRecord> = emptyList(),
    val statusMessage: String = "Not connected",
    val errorMessage: String? = null
)
