package com.example.directrf.data.rf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MockRfTransport {
    private val _events = MutableSharedFlow<RfPacket>(extraBufferCapacity = 64)
    val events: Flow<RfPacket> = _events.asSharedFlow()

    var connected: Boolean = false
        private set

    var latencyMs: Int = 50
    var packetLossPercent: Double = 2.0
    var signalStrengthDbm: Int = -70
    var bandwidthKbps: Int = 128

    suspend fun connect() {
        connected = true
    }

    suspend fun disconnect() {
        connected = false
    }

    suspend fun send(packet: RfPacket) {
        if (!connected) return
        _events.emit(packet)
    }

    fun statusText(): String = if (connected) "Connected" else "Disconnected"
}

data class RfPacket(
    val packetType: String,
    val sessionId: String,
    val senderId: String,
    val receiverId: String,
    val payload: String = "",
    val channelId: String = "CH-101"
)
