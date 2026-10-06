# RF Protocol Design

## 1. Packet structure

A production packet should include fields such as:

- MAGIC
- VERSION
- PACKET_TYPE
- SESSION_ID
- SENDER_ID
- RECEIVER_ID
- CHANNEL_ID
- SEQUENCE_NUMBER
- TIMESTAMP
- TTL
- PAYLOAD_LENGTH
- PAYLOAD
- AUTH_TAG

The current repository uses a simplified in-memory packet object for the simulation layer and is not a production-transmission stack.

## 2. Packet types

The target protocol includes:

- HELLO
- DISCOVERY
- CALL_REQUEST
- CALL_ACCEPT
- CALL_REJECT
- CALL_END
- AUDIO
- ACK
- HEARTBEAT
- CHANNEL_JOIN
- CHANNEL_LEAVE
- DEVICE_INFO
- ERROR

## 3. Real-time audio transport

For live voice, the app should not rely on TCP semantics. A UDP-like or best-effort packet model is appropriate:

- low latency
- packet loss tolerance
- jitter tolerance
- real-time voice is not a reliable stream

## 4. Important caveat

This repository does not simulate the final actual wireless radio stack or the RF hardware protocol. It simulates call flow and device state only.

## 5. Future work

A production implementation will need:

- duplicate detection
- reordering protection
- sequence validation
- heartbeat monitoring
- packet validation/authentication
- encryption metadata and replay protection
