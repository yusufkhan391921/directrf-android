# DirectRF

DirectRF is a phase-based Android prototype for a direct radio voice system that uses a dedicated RF gateway and RF transceiver chain instead of cellular, Wi-Fi, Bluetooth, or cloud infrastructure.

This project is intentionally built around a realistic architecture:

- Android app manages the UX, call flow, audio pipeline, and device logic.
- External RF hardware performs the actual over-the-air radio transmission.
- The app includes a simulation layer so development can proceed before real RF hardware exists.

The core design goal is: a normal call-like full-duplex voice experience over an offline RF link, without pretending that Android can directly drive arbitrary cellular RF modems.

## What the app does

- Display connected RF hardware and device identity
- Simulate nearby device discovery
- Show call states and a simple call lifecycle
- Support simulated incoming and outgoing calls
- Demonstrate a full-duplex call UI without a push-to-talk button
- Provide local microphone loopback and call control state flow
- Keep the architecture modular so an RF transport can later be swapped for real hardware

## Realistic system constraint

A normal Android app cannot directly access the phone’s internal cellular modem or arbitrarily control the RF stack for custom voice networking. The realistic design is:

Android phone -> USB host -> RF gateway/controller -> digital RF transceiver -> RF link -> remote side

This repository does not fake unsupported APIs or claim that Android is transmitting arbitrary RF directly.

## Architecture overview

The application follows this abstraction:

Android Application
  -> Call / Voice Layer
  -> Audio Capture
  -> Audio Codec
  -> Voice Packetizer
  -> RF Transport Layer
  -> USB / Serial Transport
  -> RF Gateway
  -> Digital RF Transceiver

The code is intentionally structured so RF hardware is replaceable and testable through `MockRfTransport`.

## RF engineering notes

The final system must be designed to comply with applicable Indian wireless regulations for frequency, power, bandwidth, antenna, and duty cycle. This project does not select arbitrary frequencies or claim a legal operating mode.

For real full-duplex voice, the preferred design is not LoRa in the normal voice-call sense. LoRa is generally not a good fit for low-latency, high-throughput, full-duplex voice. A real implementation typically needs a digital narrowband or broadband transceiver architecture with enough throughput and low enough latency to support bidirectional speech safely.

## Why this project is phase-based

This repository intentionally follows a staged build strategy:

1. Android project and UI shell
2. Device identity and settings
3. RF simulator and discovery
4. Call state machine
5. Local audio capture/playback
6. Simulated full-duplex call flow
7. USB-based RF transport abstraction
8. Real hardware integration later
9. Encryption and channels after the core path is proven

## Android architecture

This project uses:

- Kotlin
- Jetpack Compose
- MVVM-style screen state via `ViewModel`
- StateFlow for UI state
- Coroutines
- AudioRecord / AudioTrack abstraction for the local audio pipeline

## Files created

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/java/com/example/directrf/MainActivity.kt`
- `app/src/main/java/com/example/directrf/service/CallController.kt`
- `app/src/main/java/com/example/directrf/data/rf/MockRfTransport.kt`
- `app/src/main/java/com/example/directrf/audio/AudioLoopback.kt`
- `app/src/main/java/com/example/directrf/ui/DirectRfApp.kt`
- `app/src/main/java/com/example/directrf/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/example/directrf/ui/screens/CallScreen.kt`

## How to build

Use a Java 17 environment and Android SDK already present on the machine. Then run:

```bash
cd "/Users/yusufkhan/Documents/rf app"
export JAVA_HOME="/Users/yusufkhan/.local/java/jdk17/Contents/Home"
export ANDROID_HOME="/Users/yusufkhan/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
./gradlew assembleDebug
```

The APK will be generated under:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## RF simulator mode

Developer mode is labeled explicitly as simulation-only. The app includes a mock RF transport and a state machine to emulate:

- latency
- packet loss
- signal strength
- call lifecycle progression
- RF link health

This makes the Android application testable even without physical hardware.

## Hardware requirements for the real system

Future real hardware must include:

- USB-C / host-capable interface on the phone side
- RF gateway controller board
- Digital RF transceiver with enough throughput and latency budget
- Legal antenna and matching network
- Appropriate power and packaging

This project deliberately avoids describing unsupported direct Android RF APIs.

## Legal and regulatory note

The real RF design must comply with all relevant Indian wireless regulations, including frequency selection, emissions, transmit power, antenna requirements, bandwidth, and duty cycle. This repository does not give legal operating instructions beyond the requirement to comply.

## Documentation

See the docs folder for deeper architecture notes on the RF protocol, audio pipeline, hardware integration, security model, and phase plan.

## GitHub

This workspace is not automatically connected to a GitHub repository. To publish it later, create a remote repository and run:

```bash
git init
git add .
git commit -m "Initial DirectRF prototype"
git branch -M main
git remote add origin <your-github-repo-url>
git push -u origin main
```
