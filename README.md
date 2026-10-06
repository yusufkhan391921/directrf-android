# DirectRF

DirectRF is an Android-to-Android nearby voice-call app built on Google Nearby Connections. It is designed for two Android phones in short range, without an app signaling server or internet access during calls. Nearby uses the phones' Bluetooth and Wi-Fi radios; this is not custom RF hardware and does not access the cellular modem.

## Current status

The code now includes a Nearby point-to-point transport, peer discovery and connection, on-device authentication-code confirmation, Opus `MediaCodec` capture/decoding, a bounded jitter buffer, optional Android echo cancellation/noise suppression, a microphone foreground service, mute and output routing, and call-state/unit/instrumented tests.

**Real-device status: UNVERIFIED.** This project has not yet been run on two physical Android phones. The build and local JVM tests do not prove discovery, pairing, Opus interoperability, two-way audio, screen-off behavior, range, latency, or reconnection. Follow [the manual two-phone test](docs/MANUAL_TWO_PHONE_TEST.md) before describing calls as working or publishing a release.

Expected useful range is short, roughly **10-100 m depending on devices and conditions**. That is an estimate, not a measurement from this project; walls, interference, phone models, radio state and environment can reduce it substantially. No range or latency guarantee is made.

## Requirements

- Two Android phones running Android 10 (API 29) or later.
- Google Play services on both phones for Nearby Connections.
- Bluetooth and Wi-Fi radios enabled; mobile data, an internet connection, accounts and an app server are not part of the call path.
- Microphone permission to call. Nearby/legacy location permissions are requested at runtime as required by Android version.

The app has no `INTERNET` permission. Google Play services must be present; downloading/updating the app or Play services may require internet before an offline session.

## Build

Use Java 17 and Android SDK platform 35 (or Android Studio). The Gradle wrapper pins Gradle 8.7. From the project root:

```sh
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Release signing is intentionally not configured in source; keep signing keys and credentials outside Git.

## Use

1. Install the same build on both phones and create a local display name.
2. Open **Devices** on both phones and start discovery.
3. Connect to the other phone. Compare the authentication digits shown on both screens and accept only if they match.
4. Place a call, accept it on the other phone, and test mute, speaker/earpiece, and hang up.

The profile name is local and is advertised to nearby endpoints. There are no online accounts or permanent peer identity/pinning yet; the displayed authentication digits verify the current Nearby session only.

## Implementation notes

- `Transport` defines discover, connect, pairing confirmation, control/audio exchange, disconnect and status.
- `NearbyConnectionsTransport` implements `P2P_POINT_TO_POINT`; Nearby Connections supplies the local link and encrypted connection.
- Opus is provided by Android `MediaCodec`; devices must expose a usable Opus encoder/decoder. Calls fail with an error instead of silently falling back to fake audio.
- Echo cancellation and noise suppression are enabled only when the device exposes those Android audio effects.
- The manual device procedure is in [docs/MANUAL_TWO_PHONE_TEST.md](docs/MANUAL_TWO_PHONE_TEST.md).
- Data handling is described in [PRIVACY.md](PRIVACY.md); connection authentication is in [docs/SECURITY.md](docs/SECURITY.md).

## GitHub publishing

No push has been made. Set the Git remote only after confirming the repository URL is real and ready. A license has not been selected; choose one before adding a `LICENSE` file or publishing the source.