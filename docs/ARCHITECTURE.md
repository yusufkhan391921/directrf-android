# Architecture

## Runtime path

`Compose UI -> CallController -> Transport -> Nearby Connections -> peer phone`

The audio path is `AudioRecord (VOICE_COMMUNICATION) -> Android Opus encoder -> bounded framed bytes -> Nearby byte payload -> peer decoder -> jitter buffer -> AudioTrack`.

`Transport` is the boundary for peer discovery, connection, authentication confirmation, call/audio send/receive, disconnect and status. `NearbyConnectionsTransport` is the selected local, serverless implementation using `Strategy.P2P_POINT_TO_POINT`.

## Security boundary

Nearby Connections encrypts the established connection. Both devices receive the same authentication digits and the UI requires the user to compare and confirm before accepting. That authenticates this connection session, not a durable account or long-term pinned peer identity.

## Background operation

`CallAudioService` owns capture/playback during a call and runs as a foreground service with microphone and connected-device service types. The service reports startup failures to the controller; a call is not marked active until audio startup reports success.

## Verification boundary

JVM tests cover frame parsing, jitter ordering and state transitions. An instrumented test exercises the platform Opus encoder. No physical two-phone run has been performed; all actual Nearby radio behavior, codec compatibility, routing, interruption handling, range and latency remain unverified. See the manual test script.