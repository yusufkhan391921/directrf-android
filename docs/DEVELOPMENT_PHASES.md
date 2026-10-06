# Development and Verification Status

## Implemented in source

- Nearby Connections `P2P_POINT_TO_POINT` discovery and connection.
- User-confirmed matching authentication digits before connection acceptance.
- A transport interface and bounded binary audio/control frame format.
- Real platform Opus encoder/decoder setup, audio capture/playback, bounded jitter buffer, available AEC/noise-suppression effects, mute/output routing and foreground call service.
- Event-driven call states for discovery, pairing, incoming/outgoing calls, audio startup, peer loss and errors.
- JVM tests for frame codec, jitter buffer and state machine; an instrumented Opus encoder test.

## Not verified

No physical Android devices were attached during development. The Nearby API integration has only been compiled, not exercised over the air. The instrumented codec test has only been compiled. Do not claim successful pairing, two-way voice, internet independence, screen-off survival, range, latency, or reconnection until the test script passes on actual target devices.

## Required before release

1. Run the manual two-phone script on multiple Android models and record results.
2. Fix any codec, permission, foreground-service, route, lifecycle or reconnect failures observed.
3. Measure actual connection setup, end-to-end latency and usable range in documented conditions.
4. Complete a privacy/security review and select a source license.
5. Configure release signing outside Git and confirm repository ownership/remote before publishing.