package com.example.directrf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.directrf.ui.DirectRfApp
import com.example.directrf.ui.theme.DirectRfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DirectRfTheme {
                DirectRfApp()
            }
        }
    }
}
