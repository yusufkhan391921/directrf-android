package com.example.directrf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.directrf.ui.DirectRfApp
import com.example.directrf.ui.theme.DirectRfTheme

class MainActivity : ComponentActivity() {
    private var permissionContinuation: (() -> Unit)? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val continuation = permissionContinuation
        permissionContinuation = null
        continuation?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DirectRfTheme {
                DirectRfApp(
                    onRequestNearbyPermissions = ::withNearbyPermissions,
                    onRequestCallPermissions = ::withCallPermissions
                )
            }
        }
    }

    private fun withNearbyPermissions(onGranted: () -> Unit) {
        val missing = nearbyPermissions().filterNot(::isGranted)
        if (missing.isEmpty()) {
            onGranted()
        } else {
            permissionContinuation = {
                if (hasNearbyPermissions()) onGranted()
                else Toast.makeText(this, "Nearby permissions are required to discover peers.", Toast.LENGTH_LONG).show()
            }
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun withCallPermissions(onGranted: () -> Unit) {
        val missing = buildList {
            if (!isGranted(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isEmpty()) {
            onGranted()
        } else {
            permissionContinuation = {
                if (isGranted(Manifest.permission.RECORD_AUDIO)) onGranted()
                else Toast.makeText(this, "Microphone permission is required for voice calls.", Toast.LENGTH_LONG).show()
            }
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun nearbyPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT in 29..31) add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 32) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    private fun hasNearbyPermissions() = nearbyPermissions().all(::isGranted)

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
