# Audio Pipeline

## Implemented path

- Mono 48 kHz PCM capture from `AudioRecord` using `VOICE_COMMUNICATION`.
- Android `MediaCodec` Opus encoder configured for 24 kbit/s and 20 ms input frames.
- Sequenced, timestamped, length-checked audio frames sent by the selected `Transport`.
- Received frames are passed through a bounded eight-frame jitter buffer with a three-frame startup target, decoded as Opus, then written to `AudioTrack`.
- Microphone mute zeroes captured PCM. Speaker/earpiece routing uses Android communication-device routing where available.
- Android `AcousticEchoCanceler` and `NoiseSuppressor` are enabled only when available for the recording session.
- Audio runs in a microphone/connected-device foreground service and requests audio focus.

## Compatibility and limits

Android documents Opus decoding from Android 5 and encoding from Android 10; this app sets minimum SDK 29. Runtime encoder availability is checked and failure is surfaced. OEM codec behavior, echo-control support, earpiece routing, and performance vary and must be checked on target devices.

The frame protocol and JVM jitter tests are locally testable. Codec behavior is covered by `OpusCodecInstrumentedTest`, but that test has not been run on a physical phone here. Audio quality, echo, interruption recovery, and two-way operation are not verified until the manual two-phone procedure passes.