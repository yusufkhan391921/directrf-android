# Hardware Architecture

## 1. Accepted reality

Android cannot directly command the phone’s internal cellular modem for arbitrary private RF networking.

The realistic architecture is therefore:

Android phone -> USB host -> RF gateway/controller -> digital RF transceiver -> RF link

## 2. Prototype architecture

For the prototype, use:

- one Android phone
- USB-C connection to an external gateway board
- simulation layer in the app until hardware is ready

## 3. Future production requirements

A production design will need:

- host-capable USB-C connectivity
- external RF transceiver with legal operating band
- appropriate antennas and matching network
- RF link management logic
- power management and battery awareness
- hardware health and USB status tracking

## 4. Legal requirement

The final RF frequency, power, bandwidth, antenna, and duty cycle must comply with applicable Indian wireless regulation. Do not select arbitrary frequencies or claim a legal mode without checking the actual rules.

## 5. What the code does not claim

This project does not claim to create arbitrary RF transmissions directly from the Android app itself. The app only provides the UI and control layer while the external hardware performs the actual RF path.
