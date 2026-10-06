# Privacy Notice

DirectRF currently has no online accounts, backend, signaling server, analytics SDK, advertising SDK, or app `INTERNET` permission.

## Data stored on this phone

- The display name entered during setup is stored in app-private SharedPreferences and advertised to nearby endpoints during Nearby discovery/connection.
- Completed-call metadata (peer display name, call end time, duration, outcome) is stored locally in SharedPreferences, limited to the latest 50 records.
- Audio is processed in memory for live calls and is not intentionally recorded to persistent storage.
- Android backup is disabled for the application.

## Data sent to the other phone

During discovery/connection, Nearby Connections uses the local Bluetooth/Wi-Fi radios and exchanges endpoint names, connection metadata/authentication digits, call-control frames, and encoded Opus audio. The Nearby connection is encrypted. Audio and control payloads are not sent to an app-owned server.

Google Play services is required to use Nearby Connections. Its platform behavior and privacy handling are governed by Google's own terms and privacy notices; this app does not control service-level diagnostics collected by Play services or Android.

## Permissions

DirectRF requests Nearby/Bluetooth permissions when discovery is started, legacy location access only on Android versions where Nearby requires it, microphone access when a call is placed or accepted, and notification permission on Android 13+ for call notifications. Calls require two phones with Google Play services.

## Prototype limitations

This implementation has not been verified on two physical phones. Do not use it for sensitive or safety-critical communications until real-device, security, and privacy reviews are complete. This notice describes the code as currently implemented and must be reviewed before public distribution.