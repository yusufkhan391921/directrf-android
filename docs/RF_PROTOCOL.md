# Nearby Frame Protocol

The selected transport is Google Nearby Connections using `P2P_POINT_TO_POINT`. Nearby Connections handles device discovery, connection lifecycle and encrypted local delivery; the app does not implement a raw Bluetooth or Wi-Fi protocol.

The app sends Nearby `BYTES` payloads with a compact binary envelope:

- Four-byte `DRF1` magic.
- One-byte frame type.
- Audio: 64-bit sequence, 64-bit capture timestamp, 16-bit encoded length, Opus packet.
- Control: one-byte call signal (`CALL`, `ACCEPT`, `DECLINE`, `END`).

The decoder rejects unknown headers/types, malformed lengths and frames larger than 4096 bytes. Audio frames are sequenced and passed to a bounded local jitter buffer. Nearby encrypts connection payloads; payload fields are not separately encrypted by this app.

This framing has local unit coverage. Interoperability, timing and delivery over physical phones remain unverified.