package com.example.directrf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

private val DirectRfColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF67E8F9),
    secondary = androidx.compose.ui.graphics.Color(0xFF1D4ED8),
    background = androidx.compose.ui.graphics.Color(0xFF06121A),
    surface = androidx.compose.ui.graphics.Color(0xFF0B1D2A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF041319),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE2F6FF),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2F6FF)
)

@Composable
fun DirectRfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DirectRfColors,
        content = {
            Surface(color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    )
}
