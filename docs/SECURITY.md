# Security

## Link and peer confirmation

Nearby Connections provides an encrypted connection. On each connection attempt, Nearby supplies the same authentication digits to both devices. DirectRF displays those digits and requires explicit user acceptance; users should reject the link if the codes differ or the peer is unexpected.

The endpoint name is a profile display name, not a cryptographic identity. Endpoint IDs are session-scoped. The app has no account system, long-term peer key pinning, contact trust store, or app-managed end-to-end key protocol. The security guarantee currently relies on Nearby's encrypted authenticated connection and the users comparing its digits.

## Data and permissions

The app requests Bluetooth/Nearby permissions for discovery, legacy location access only on Android versions that require it, microphone permission when calling, and notification permission on Android 13+ when starting call service. It has no app `INTERNET` permission and no signaling server.

The local profile and call metadata are stored in SharedPreferences. Audio is not intentionally recorded to persistent storage. See [PRIVACY.md](../PRIVACY.md).

## Verification

The code requests user confirmation before accepting Nearby connections. Security behavior has not been assessed by penetration testing or verified on two devices. Do not treat this prototype as a security-certified communications product.