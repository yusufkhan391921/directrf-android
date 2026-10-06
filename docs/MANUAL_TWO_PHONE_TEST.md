# Manual Two-Phone Verification

**Status: Not run.** This workspace currently has no Android phone or emulator attached. Do not describe calls, range, latency, reconnection, or audio quality as verified until this script passes on two physical phones.

## Requirements

- Two Android phones running Android 10 (API 29) or later, with Google Play services installed and updated.
- The same signed APK installed on both phones.
- Microphones and speakers/headsets available.
- Nearby Devices/Bluetooth permissions granted on both phones; location is required on Android 10-12. Microphone permission is required before a call.
- For the offline check, keep Bluetooth and Wi-Fi radios enabled, but disconnect both phones from internet access. Nearby Connections may use local Bluetooth/Wi-Fi links; no app server is configured.
- Record each phone model, Android version, distance, environment, call setup time, and any audio interruption. Do not infer a guaranteed range from a single run.

## Test Steps

1. Open DirectRF on both phones, create different local display names, and open **Devices**.
2. Tap **Find nearby phones** on both phones. Keep both screens open. Within 60 seconds, each phone should discover the other using its actual profile name. **Pass:** the other phone appears. **Fail:** no peer appears, a permission error remains, or a placeholder name appears.
3. On Phone A, tap **Connect** on Phone B's discovered row. Both phones should display a pairing prompt and authentication digits. Compare the digits by voice or visually. Accept only if they match; tap **Codes match** on both. **Pass:** both show the other phone as securely connected. **Fail:** digits differ, either side connects without confirmation, or either side cannot accept.
4. On Phone A, tap **Call**. Phone B should show an incoming-call screen with Phone A's actual profile name. **Pass:** Phone B can accept or decline; Phone A stays in outgoing state until acceptance. **Fail:** either phone reports an active call before the peer accepts.
5. Accept on Phone B. Speak distinct phrases in turn, then speak simultaneously for at least 30 seconds. Verify intelligible two-way audio and no repeated echo/howling. **Pass:** both directions carry live voice. Repeat with a wired/Bluetooth headset if available.
6. During the call, toggle mute on each phone independently. The other phone should stop receiving that phone's microphone while the local receive path continues. Toggle speaker/earpiece and confirm output actually changes.
7. Lock each screen for 60 seconds in separate runs. Confirm the foreground call notification remains, the call continues, and both phones can still end the call. **Pass:** no capture/playback stops when screen-off.
8. End from Phone A. Phone B should leave the call state and return to the peer-connected state; both call-history screens should record the completed call. Repeat ending from Phone B.
9. Reconnect test: move one phone out of range or temporarily disable its Bluetooth/Wi-Fi radio. The other phone must show a peer-lost error and stop audio; it must not keep claiming the call is active. Restore the radios, rediscover, compare codes, reconnect, and place another call. **Pass:** fresh authenticated connection restores calling. **Fail:** stale peer is treated as connected or audio is routed to the old session.
10. Decline test: place another call and decline it on the receiving phone. **Pass:** caller sees declined and both phones return to connected state without starting audio.
11. Permission test: revoke Nearby permission and try discovery, then revoke microphone permission and try calling. **Pass:** the app explains the specific missing permission and does not report discovery/call/audio success.
12. Repeat discovery and a 2-minute call with mobile data off and both phones disconnected from Wi-Fi access points while leaving Wi-Fi/Bluetooth radios on. **Pass:** peer connection and call still work without internet.
13. Repeat at measured distances (for example 5 m, 10 m, 30 m, and 50 m) in open and obstructed environments. Record pass/fail and reconnect behavior at each distance. These observations are device/environment-specific, not a guaranteed service range.

## Results Record

| Run | Phone models / Android versions | Distance and environment | Internet disconnected | Pairing | Two-way audio | Screen-off | Reconnect | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Not run | Not recorded | Not measured | Not tested | Not tested | Not tested | Not tested | Not tested | Requires two physical Android phones |
