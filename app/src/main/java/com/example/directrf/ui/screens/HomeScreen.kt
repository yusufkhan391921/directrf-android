package com.example.directrf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.directrf.domain.model.CallState
import com.example.directrf.domain.model.CallUiState

@Composable
fun HomeScreen(
    state: CallUiState,
    onDiscover: () -> Unit,
    onConnect: () -> Unit,
    onCall: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit
) {
    val headerColor = if (state.rfLinkStatus == "RF Simulator active") Color(0xFF34D399) else Color(0xFF7DD3FC)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "RF connection", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(text = state.rfLinkStatus, color = headerColor, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(headerColor, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onDiscover) { Text("Discover") }
            Button(onClick = onConnect) { Text("Connect RF") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.callState == CallState.ACTIVE_CALL) {
            CallScreen(state = state, onMute = onMute, onSpeaker = onSpeaker, onEnd = onEnd)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(text = "Device ID", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(text = state.deviceId, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Connected RF hardware: ${state.connectedHardware}")
                Text(text = "Current channel: ${state.currentChannel}")
                Text(text = "Battery: ${state.batteryLevel}")
                Text(text = "Signal: ${state.connectedSignal}")
                Text(text = "Latency: ${state.latency}")
                Text(text = "Packet loss: ${state.packetLoss}")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Text("Call ${state.remoteName}")
                }

                if (state.callState == CallState.RINGING || state.callState == CallState.CALL_REQUEST_SENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))) {
                            Text("Accept")
                        }
                        Button(onClick = onReject, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))) {
                            Text("Reject")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(text = "Nearby Devices", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.nearbyDevices) { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(device.name)
                        Text(device.signal)
                    }
                }
            }
        }
    }
}
