## Why

After three wrong passcode attempts, the app enters brute-force protection and shows an incremental countdown before another attempt is allowed. Today the on-screen numeric keypad stays fully interactive during that countdown, which is inconsistent with the corresponding iOS behavior and lets users keep tapping keys that have no effect. Issue #4874 asks for the keypad to be hidden while the lockout timer is on screen.

## What Changes

- Hide the on-screen `NumberKeyboard` while the brute-force lockout countdown is displayed on the passcode screen.
- Restore the `NumberKeyboard` when the countdown finishes and passcode input is re-enabled.
- Clear the "Incorrect passcode" error text when the countdown finishes, so the user is presented with a clean screen for the next attempt.
- No change to the countdown timing, the passcode digit boxes, or the hardware-key input path.

## Capabilities

### New Capabilities
- `passcode-lock`: On-device passcode entry flow, including brute-force protection UX (attempt threshold, incremental lockout countdown, keypad availability during lockout).

### Modified Capabilities
<!-- None; this change introduces the first spec for this capability. -->

## Impact

- Code: `owncloudApp/src/main/java/com/owncloud/android/presentation/security/passcode/PassCodeActivity.kt` (lockout show/hide paths, plus the countdown-finished observer).
- APIs / data: none. No changes to `PassCodeViewModel` public surface, LiveData contracts, or persisted preferences.
- Dependencies: none.