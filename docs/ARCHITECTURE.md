# Architecture

## 1. Android side

The Android app is the user-facing product. It handles:

- UI and navigation
- device identity and UI state
- call flow state machine
- audio capture and playback via Android APIs
- abstraction around RF transport
- future USB / hardware integration via host API adapters

The project uses modern Android patterns:

- Kotlin
- Jetpack Compose
- ViewModel + StateFlow
- Coroutines

## 2. RF side

The actual RF send/receive path is intentionally kept outside the app’s core logic. The app interacts with an RF transport abstraction instead of assuming a single chip or protocol.

This allows a future replacement of the underlying hardware without restructuring the app.

## 3. Simulation-first strategy

The repository contains a `MockRfTransport` implementation that is explicitly labeled simulation-only.

This allows the app to be developed, tested, and refined before actual RF hardware exists.

## 4. Real hardware architecture

Real hardware must be designed around:

Android phone -> USB-C host -> RF gateway -> digital RF transceiver -> air

This is the realistic boundary where Android can interact with external gateways.

## 5. Architecture boundaries

- `domain/model` holds state and data structures.
- `service` contains call flow logic and simulator coordination.
- `data/rf` contains transport and packet structures.
- `audio` contains local microphone and speaker pipeline logic.
- `ui` contains Compose screens and state rendering.

## 6. What this prototype proves

The prototype proves that the app can behave like a full-duplex radio-call app at the UI and call-control layer while remaining technologically honest about RF limitations.
