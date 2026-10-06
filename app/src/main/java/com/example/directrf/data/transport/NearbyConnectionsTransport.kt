package com.example.directrf.data.transport

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NearbyConnectionsTransport(context: Context) : Transport {
    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    private val _status = MutableStateFlow(TransportStatus.DISCONNECTED)
    private val endpointNames = ConcurrentHashMap<String, String>()
    @Volatile
    private var connectedEndpointId: String? = null
    private var localDisplayName = "DirectRF user"

    override val events = _events.asSharedFlow()
    override val discoveredPeers = _discoveredPeers.asStateFlow()
    override val status = _status.asStateFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            runCatching { TransportFrameCodec.decode(bytes) }
                .onSuccess { message ->
                    when (message) {
                        is TransportFrame.Audio -> _events.tryEmit(TransportEvent.AudioReceived(message.frame))
                        is TransportFrame.AudioConfiguration -> _events.tryEmit(
                            TransportEvent.AudioConfigurationReceived(message.configuration)
                        )
                        is TransportFrame.Signal -> _events.tryEmit(TransportEvent.SignalReceived(message.signal))
                    }
                }
                .onFailure { _events.tryEmit(TransportEvent.Failure("Invalid data received from peer")) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val peer = DiscoveredPeer(endpointId, info.endpointName)
            endpointNames[endpointId] = info.endpointName
            _status.value = TransportStatus.PAIRING
            _events.tryEmit(TransportEvent.PairingRequested(peer, info.authenticationDigits))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpointId = endpointId
                _status.value = TransportStatus.CONNECTED
                _events.tryEmit(TransportEvent.Connected(peerFor(endpointId)))
                stopDiscoveryWork()
            } else {
                connectedEndpointId = null
                _status.value = TransportStatus.ERROR
                _events.tryEmit(TransportEvent.Failure("Nearby connection failed (${result.status.statusCode})"))
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (connectedEndpointId == endpointId) connectedEndpointId = null
            _status.value = TransportStatus.DISCONNECTED
            _events.tryEmit(TransportEvent.Disconnected(endpointId))
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            endpointNames[endpointId] = info.endpointName
            val peer = DiscoveredPeer(endpointId, info.endpointName)
            _discoveredPeers.update { current ->
                (current.filterNot { it.endpointId == endpointId } + peer)
            }
            _events.tryEmit(TransportEvent.PeerFound(peer))
        }

        override fun onEndpointLost(endpointId: String) {
            if (endpointId == connectedEndpointId) return
            endpointNames.remove(endpointId)
            _discoveredPeers.update { peers -> peers.filterNot { it.endpointId == endpointId } }
            _events.tryEmit(TransportEvent.PeerLost(endpointId))
        }
    }

    override suspend fun discover(localDisplayName: String) {
        this.localDisplayName = localDisplayName.trim().take(32).ifBlank { "DirectRF user" }
        _status.value = TransportStatus.DISCOVERING
        try {
            client.startAdvertising(
                this.localDisplayName,
                SERVICE_ID,
                lifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
            ).awaitResult()
            client.startDiscovery(
                SERVICE_ID,
                discoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
            ).awaitResult()
        } catch (error: Exception) {
            _status.value = TransportStatus.ERROR
            _events.emit(TransportEvent.Failure(error.message ?: "Nearby discovery could not start"))
            runCatching { client.stopAdvertising() }
            throw error
        }
    }

    override suspend fun connect(endpointId: String, localDisplayName: String) {
        this.localDisplayName = localDisplayName.trim().take(32).ifBlank { "DirectRF user" }
        _status.value = TransportStatus.PAIRING
        try {
            client.requestConnection(this.localDisplayName, endpointId, lifecycleCallback).awaitResult()
        } catch (error: Exception) {
            _status.value = TransportStatus.ERROR
            _events.emit(TransportEvent.Failure(error.message ?: "Could not request peer connection"))
            throw error
        }
    }

    override suspend fun confirmPairing(endpointId: String, accepted: Boolean) {
        if (accepted) {
            client.acceptConnection(endpointId, payloadCallback).awaitResult()
        } else {
            client.rejectConnection(endpointId).awaitResult()
            _status.value = TransportStatus.DISCONNECTED
        }
    }

    override suspend fun sendAudioFrame(frame: EncodedAudioFrame) {
        send(TransportFrameCodec.encodeAudio(frame))
    }

    override suspend fun sendAudioConfiguration(configuration: AudioCodecConfiguration) {
        send(TransportFrameCodec.encodeAudioConfiguration(configuration))
    }

    override suspend fun sendSignal(signal: CallSignal) {
        send(TransportFrameCodec.encodeSignal(signal))
    }

    private suspend fun send(message: ByteArray) {
        val endpointId = connectedEndpointId
            ?: throw IllegalStateException("No authenticated Nearby peer is connected")
        client.sendPayload(endpointId, Payload.fromBytes(message)).awaitResult()
    }

    override suspend fun disconnect() {
        connectedEndpointId = null
        runCatching { client.stopDiscovery() }
        runCatching { client.stopAdvertising() }
        runCatching { client.stopAllEndpoints() }
        endpointNames.clear()
        _discoveredPeers.value = emptyList()
        _status.value = TransportStatus.DISCONNECTED
    }

    private fun stopDiscoveryWork() {
        client.stopDiscovery()
        client.stopAdvertising()
    }

    private fun peerFor(endpointId: String) = DiscoveredPeer(
        endpointId = endpointId,
        displayName = endpointNames[endpointId] ?: "Nearby DirectRF peer"
    )

    private companion object {
        const val SERVICE_ID = "com.example.directrf.nearby"
    }
}

private suspend fun Task<*>.awaitResult(): Unit = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        val error = task.exception
        if (task.isSuccessful) {
            continuation.resume(Unit)
        } else {
            continuation.resumeWithException(error ?: IllegalStateException("Nearby operation failed"))
        }
    }
}
