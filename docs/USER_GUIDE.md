# DirectRF User Guide

DirectRF connects two nearby Android phones through Google Nearby Connections. It requires Android 10+, Google Play services, Bluetooth/Wi-Fi radios, and microphone access for calls. No account, custom RF gateway, call server or internet connection is part of the call path. Actual two-phone operation remains unverified; use the manual test checklist before relying on it.

## Set up

1. Install the same APK on both phones.
2. Open DirectRF and enter a local display name on each phone.
3. Open Devices and start discovery on both phones. Grant the Nearby permissions Android requests.
4. Select the other phone and connect.
5. Compare the authentication digits displayed on both phones. Accept only if they match.

## Call

The connected phone can place a call. The other phone can accept or decline. Microphone permission is requested when placing or accepting. During a call, use Mute, Speaker/Earpiece, and End call. The call service displays an ongoing notification and is intended to keep audio running with the screen off.

## Distance and troubleshooting

Nearby phone-to-phone range is short. Roughly 10-100 m is an estimate under suitable conditions, not measured or guaranteed by this project. If no peer appears, keep both apps open, ensure Bluetooth/Wi-Fi and required Nearby permissions are enabled, and verify Google Play services is available. If audio cannot start, check microphone permission and Opus codec support on the phone.

See [MANUAL_TWO_PHONE_TEST.md](MANUAL_TWO_PHONE_TEST.md) for exact steps and pass/fail criteria. No results are recorded there yet.