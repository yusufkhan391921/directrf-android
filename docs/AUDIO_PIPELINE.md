# Audio Pipeline

## 1. Local audio capture

The app uses Android `AudioRecord` to capture microphone input.

## 2. Local playback

The app uses Android `AudioTrack` to play the received audio to the speaker path.

## 3. Prototype implementation

This repository includes a loopback-style `AudioLoopback` class that demonstrates the capture and playback path at a basic level.

This is not a production codec chain yet. It is a local audio pipeline foundation.

## 4. Real production path

The real implementation should be:

Microphone -> AudioRecord -> PCM frames -> VAD -> codec -> encryption -> RF packetizer -> RF transport

Receive side:

RF transport -> packet validation -> decryption -> jitter buffer -> packet loss handling -> audio decode -> AudioTrack -> speaker

## 5. Why jitter buffer is required

VoIP and RF voice systems can experience variable arrival times. A jitter buffer smooths these timing differences and reduces audible dropouts caused by packet delay swings.

## 6. Full-duplex requirement

The design must allow simultaneous bidirectional audio streams. The app must not rely on push-to-talk semantics.

## 7. Codec choice and notes

Opus is a strong candidate for a production implementation because it is designed for low-latency real-time audio and adapts well to variable network conditions. However, the chosen hardware must still support the required throughput and latency budget, which must be validated against the actual RF path.
