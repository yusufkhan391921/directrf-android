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
import com.example.directrf.domain.model.CallUiState

@Composable
fun CallScreen(
    state: CallUiState,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RF CALL",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF67E8F9)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF0F172A), CircleShape)
                .border(1.dp, Color(0xFF67E8F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.remoteName.take(1),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF67E8F9)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = state.remoteName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatDuration(state.callDurationSeconds),
            fontSize = 20.sp,
            color = Color(0xFFB7E3F8)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onSpeaker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.speakerOn) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(text = if (state.speakerOn) "🔊 Speaker ON" else "🔊 Speaker")
            }

            Button(
                onClick = onMute,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.muted) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(text = if (state.muted) "🎙 Muted" else "🎙 Mic")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onEnd,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
            Text(text = "End Call")
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
