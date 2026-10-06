package com.example.directrf.domain.model

enum class CallState {
    IDLE,
    DISCOVERING,
    DEVICE_FOUND,
    CALL_REQUEST_SENT,
    RINGING,
    CALL_ACCEPTED,
    CONNECTING_AUDIO,
    ACTIVE_CALL,
    ENDING
}

data class DeviceInfo(
    val id: String,
    val name: String,
    val signal: String,
    val status: String = "Available"
)

data class CallUiState(
    val callState: CallState = CallState.IDLE,
    val deviceId: String = "RF-7F32A91C",
    val displayName: String = "Rahul",
    val connectedHardware: String = "RF Gateway / USB-C",
    val currentChannel: String = "CH-101",
    val nearbyDevices: List<DeviceInfo> = listOf(
        DeviceInfo("RF-A1F8", "Aarav", "-68 dBm"),
        DeviceInfo("RF-8D74", "Neha", "-72 dBm"),
        DeviceInfo("RF-C2B1", "Ishaan", "-75 dBm")
    ),
    val connectedSignal: String = "-68 dBm",
    val packetLoss: String = "1.2%",
    val latency: String = "42 ms",
    val batteryLevel: String = "72%",
    val remoteName: String = "Aarav",
    val muted: Boolean = false,
    val speakerOn: Boolean = false,
    val callDurationSeconds: Long = 0L,
    val rfLinkStatus: String = "SIMULATION ONLY"
)
