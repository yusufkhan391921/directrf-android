package com.example.directrf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.directrf.domain.model.CallState
import com.example.directrf.service.CallController
import com.example.directrf.ui.screens.HomeScreen
import kotlinx.coroutines.delay

@Composable
fun DirectRfApp(
    viewModel: CallController = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.callState) {
        if (state.callState == CallState.ACTIVE_CALL) {
            while (true) {
                delay(1000)
                viewModel.tickTimer()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeScreen(
                state = state,
                onDiscover = { viewModel.discoverNearbyDevices() },
                onConnect = { viewModel.connectRf() },
                onCall = { viewModel.placeCall("RF-A1F8") },
                onAccept = { viewModel.acceptCall() },
                onReject = { viewModel.rejectCall() },
                onEnd = { viewModel.endCall() },
                onMute = { viewModel.toggleMute() },
                onSpeaker = { viewModel.toggleSpeaker() }
            )
        }
    }
}
