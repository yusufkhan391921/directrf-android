# Security Architecture

## 1. Security requirements

The final production system must support:

- device authentication
- secure key exchange
- encrypted signaling
- encrypted audio
- replay protection
- packet authentication
- session keys

## 2. Recommended primitives

For a production system, the design should use well-known primitives such as:

- X25519 for key agreement
- AES-GCM or ChaCha20-Poly1305 for confidentiality and authentication
- secure random session keys
- Android Keystore for private key storage where appropriate

## 3. Important note

This repository does not implement the final cryptographic layer yet. The current project is intentionally limited to the simulation and call-state architecture. The secure layer is planned after the core call flow is proven.

## 4. Key storage rules

Private keys must not be kept in plaintext. They should be backed by Android Keystore or an equivalent secure storage mechanism when the app targets a production security path.
