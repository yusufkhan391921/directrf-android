# Development Phases

## Phase 1: project and UI

- Create Android project
- Add Compose UI shell
- Implement basic device identity and settings state
- Add mock RF transport layer

## Phase 2: discovery

- Simulate nearby devices
- Present RF status and connection state
- Prepare for future call initiation

## Phase 3: call signaling

- Request call
- Ringing state
- Accept and reject flow
- End call flow

## Phase 4: local audio pipeline

- microphone capture
- playback path
- basic audio loopback and call controls

## Phase 5: simulated RF audio transport

- packet delivery simulation
- latency and packet loss behavior
- connection health monitoring

## Phase 6: full-duplex call testing

- simultaneous bidirectional call handling in the UI layer
- state tracking for active call and timers

## Phase 7: USB gateway integration

- add host USB abstraction
- define gateway interface
- keep app hardware-independent

## Phase 8: real hardware

- connect to a legal RF gateway and transceiver
- validate with a real hardware test bench

## Phase 9: encryption

- key exchange and session keys
- secure audio and signaling

## Phase 10: channels and future features

- logical channel layer
- multi-user group communication as a future feature
