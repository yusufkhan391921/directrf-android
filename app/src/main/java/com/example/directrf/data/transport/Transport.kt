package com.example.directrf.data.transport

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class DiscoveredPeer(
    val endpointId: String,
    val displayName: String
)

data class EncodedAudioFrame(
    val sequence: Long,
    val capturedAtMillis: Long,
    val encoded: ByteArray
)

data class AudioCodecConfiguration(
    val csd0: ByteArray,
    val csd1: ByteArray? = null,
    val csd2: ByteArray? = null
)

enum class CallSignal {
    CALL,
    ACCEPT,
    DECLINE,
    END
}

sealed interface TransportEvent {
    data class PeerFound(val peer: DiscoveredPeer) : TransportEvent
    data class PeerLost(val endpointId: String) : TransportEvent
    data class PairingRequested(
        val peer: DiscoveredPeer,
        val authenticationDigits: String
    ) : TransportEvent
    data class Connected(val peer: DiscoveredPeer) : TransportEvent
    data class Disconnected(val endpointId: String) : TransportEvent
    data class AudioReceived(val frame: EncodedAudioFrame) : TransportEvent
    data class AudioConfigurationReceived(val configuration: AudioCodecConfiguration) : TransportEvent
    data class SignalReceived(val signal: CallSignal) : TransportEvent
    data class Failure(val message: String) : TransportEvent
}

enum class TransportStatus {
    DISCONNECTED,
    DISCOVERING,
    PAIRING,
    CONNECTED,
    ERROR
}

interface Transport {
    val events: SharedFlow<TransportEvent>
    val discoveredPeers: StateFlow<List<DiscoveredPeer>>
    val status: StateFlow<TransportStatus>

    suspend fun discover(localDisplayName: String)
    suspend fun connect(endpointId: String, localDisplayName: String)
    suspend fun confirmPairing(endpointId: String, accepted: Boolean)
    suspend fun sendAudioConfiguration(configuration: AudioCodecConfiguration)
    suspend fun sendAudioFrame(frame: EncodedAudioFrame)
    suspend fun sendSignal(signal: CallSignal)
    suspend fun disconnect()
}
