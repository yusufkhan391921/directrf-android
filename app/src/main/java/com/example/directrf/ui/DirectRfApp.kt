package com.example.directrf.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.directrf.DirectRfApplication
import com.example.directrf.data.transport.DiscoveredPeer
import com.example.directrf.domain.model.AppTab
import com.example.directrf.domain.model.CallRecord
import com.example.directrf.domain.model.CallState
import com.example.directrf.domain.model.CallUiState
import com.example.directrf.domain.model.DeviceInfo
import com.example.directrf.service.CallController
import com.example.directrf.service.CallControllerFactory
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun DirectRfApp(
    onRequestNearbyPermissions: (() -> Unit) -> Unit,
    onRequestCallPermissions: (() -> Unit) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val app = context as DirectRfApplication
    val factory = remember(app) {
        CallControllerFactory(context, app.transport, app.preferences, app.audioEvents)
    }
    val controller: CallController = viewModel(factory = factory)
    val state by controller.uiState.collectAsState()

    LaunchedEffect(state.callState) {
        if (state.callState == CallState.IN_CALL) {
            while (true) {
                delay(1000)
                controller.tickTimer()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !state.profileComplete -> ProfileSetupScreen(controller::saveProfile)
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppHeader(state)
                AppTabBar(state.selectedTab, controller::setSelectedTab)
                when (state.selectedTab) {
                    AppTab.HOME -> HomeScreen(
                        state = state,
                        onDiscover = { onRequestNearbyPermissions(controller::discover) },
                        onCall = { onRequestCallPermissions(controller::placeCall) },
                        onAccept = { onRequestCallPermissions(controller::acceptCall) },
                        onDecline = controller::declineCall,
                        onEnd = controller::endCall,
                        onMute = controller::toggleMute,
                        onSpeaker = controller::toggleSpeaker,
                        onDisconnect = controller::disconnect
                    )
                    AppTab.DEVICES -> DeviceListScreen(
                        state = state,
                        onDiscover = { onRequestNearbyPermissions(controller::discover) },
                        onConnect = controller::connect
                    )
                    AppTab.HISTORY -> CallHistoryScreen(state.callHistory)
                }
            }
        }
    }

    val pairingDigits = state.pairingDigits
    val pairingEndpointId = state.pairingEndpointId
    if (pairingDigits != null && pairingEndpointId != null) {
        PairingConfirmationDialog(
            peerName = state.pairingPeerName ?: "Nearby peer",
            digits = pairingDigits,
            onConfirm = { controller.confirmPairing(pairingEndpointId, true) },
            onReject = { controller.confirmPairing(pairingEndpointId, false) }
        )
    }
}

@Composable
private fun AppHeader(state: CallUiState) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("DIRECTRF", color = Color(0xFF67E8F9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(state.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(state.statusMessage, color = Color(0xFFB7E3F8), fontSize = 13.sp)
        state.errorMessage?.let {
            Text(it, color = Color(0xFFFCA5A5), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppTabBar(selectedTab: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppTab.entries.forEach { tab ->
            Button(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == tab) Color(0xFF0F766E) else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(tab.name.lowercase().replaceFirstChar { it.titlecase() })
            }
        }
    }
}

@Composable
private fun ProfileSetupScreen(onSave: (String) -> Unit) {
    var displayName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your nearby identity", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("This name is shared with a nearby phone when you connect.", color = Color(0xFF94A3B8))
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it.take(32) },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(displayName) },
            enabled = displayName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
        Text("Stored locally on this phone. No account required.", color = Color(0xFF64748B), fontSize = 12.sp)
    }
}

@Composable
private fun HomeScreen(
    state: CallUiState,
    onDiscover: () -> Unit,
    onCall: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state.callState) {
            CallState.IN_CALL -> ActiveCallPanel(state, onMute, onSpeaker, onEnd)
            CallState.INCOMING_CALL -> {
                Text("Incoming call", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${state.remoteName ?: "Nearby peer"} is calling")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
                    Button(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                    ) { Text("Decline") }
                }
            }
            CallState.OUTGOING_CALL -> {
                Text("Calling ${state.remoteName ?: "nearby peer"}…", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Waiting for the other phone to accept.", color = Color(0xFF94A3B8))
                Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) { Text("Cancel call") }
            }
            CallState.AUDIO_CONNECTING -> {
                Text("Starting voice call", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Setting up microphone, Opus codec, and audio route.", color = Color(0xFF94A3B8))
                Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            CallState.RECONNECTING -> {
                Text("Peer connection lost", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Rediscover and verify the phone again to reconnect.", color = Color(0xFFFCA5A5))
                Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("Find nearby phones") }
            }
            else -> {
                Text(
                    text = if (state.callState == CallState.CONNECTED) {
                        "Connected to ${state.remoteName ?: "nearby phone"}"
                    } else "No peer connected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.callState == CallState.CONNECTED) {
                    Button(onClick = onCall, modifier = Modifier.fillMaxWidth()) { Text("Call") }
                    TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect peer") }
                } else {
                    Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.callState == CallState.DISCOVERING) "Searching…" else "Find nearby phones")
                    }
                }
                Text(
                    "Nearby calling requires two Android phones with Google Play services. Keep DirectRF open while waiting for a call.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ActiveCallPanel(
    state: CallUiState,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onEnd: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("VOICE CALL", color = Color(0xFF67E8F9), fontWeight = FontWeight.Bold)
        Text(state.remoteName ?: "Nearby peer", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(formatDuration(state.callDurationSeconds), color = Color(0xFFB7E3F8), fontSize = 20.sp)
        Text("Encrypted Nearby connection", color = Color(0xFF94A3B8), fontSize = 12.sp)
        if (state.audioInterrupted) {
            Text("Audio interrupted; waiting for audio focus", color = Color(0xFFFCA5A5), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMute, modifier = Modifier.weight(1f)) {
                Text(if (state.muted) "Unmute" else "Mute")
            }
            Button(onClick = onSpeaker, modifier = Modifier.weight(1f)) {
                Text(if (state.speakerEnabled) "Earpiece" else "Speaker")
            }
        }
        Button(
            onClick = onEnd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
        ) { Text("End call") }
    }
}

@Composable
private fun DeviceListScreen(
    state: CallUiState,
    onDiscover: () -> Unit,
    onConnect: (DeviceInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Nearby phones", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("Refresh discovery") }
        if (state.nearbyDevices.isEmpty()) {
            Text("No phones found yet. Both phones need discovery enabled.", color = Color(0xFF94A3B8))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.nearbyDevices, key = { it.endpointId }) { peer ->
                    PeerRow(peer, state.selectedEndpointId == peer.endpointId, onConnect)
                }
            }
        }
    }
}

@Composable
private fun PeerRow(peer: DeviceInfo, selected: Boolean, onConnect: (DeviceInfo) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(peer.name, fontWeight = FontWeight.Bold)
            Text("Nearby phone", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Button(onClick = { onConnect(peer) }, enabled = !selected && !peer.connected) {
            Text(if (peer.connected) "Connected" else if (selected) "Connecting" else "Connect")
        }
    }
}

@Composable
private fun PairingConfirmationDialog(
    peerName: String,
    digits: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = { Text("●", color = Color(0xFF34D399)) },
        title = { Text("Verify nearby phone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connecting to $peerName")
                Text(digits, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF67E8F9))
                Text("Compare this code with the other phone. Accept only if both codes match.", color = Color(0xFF94A3B8))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Codes match") } },
        dismissButton = { TextButton(onClick = onReject) { Text("Reject") } }
    )
}

@Composable
private fun CallHistoryScreen(records: List<CallRecord>) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text("Call history", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (records.isEmpty()) {
            Text("Completed calls will appear here.", color = Color(0xFF94A3B8))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { record -> CallHistoryRow(record) }
            }
        }
    }
}

@Composable
private fun CallHistoryRow(record: CallRecord) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(record.remoteName, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.endedAtMillis)),
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(record.durationSeconds))
            Text(record.outcome, color = Color(0xFF34D399), fontSize = 12.sp)
        }
    }
}

private fun formatDuration(seconds: Long): String =
    String.format("%02d:%02d", seconds / 60, seconds % 60)
