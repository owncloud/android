## Why

After 3 wrong passcode attempts, the brute-force protection countdown activates but the number keyboard remains visible and tappable — giving the user false affordance since taps have no effect. Additionally, when the countdown ends the error message stays on the screen, so users still see the error. When the countdown is over, the error should be gone. Both issues degrade the security UX and should be fixed together since they share the same countdown lifecycle.

## What Changes

- The `NumberKeyboard` view is hidden when the brute-force countdown starts and restored when the countdown ends.
- The error message (`passcodeError`) is cleared when the countdown ends so the screen is clean when the user can try again.

## Capabilities

### New Capabilities
- `auth/passcode-brute-force`: Specifies the required UI state during and after the brute-force protection countdown on the passcode screen.

### Modified Capabilities
<!-- None — no existing spec file exists for this capability yet. -->

## Impact

- `owncloudApp/src/main/java/com/owncloud/android/presentation/security/passcode/PassCodeActivity.kt`:
  - `lockScreen()` — hide `numberKeyboard` when locking
  - `getFinishedTimeToUnlockLiveData` observer — restore `numberKeyboard` visibility and preserve `passcodeError` visibility when countdown ends

## Non-goals

- Changing the brute-force countdown duration or attempt threshold.
- Adding animations or transitions to the keyboard show/hide.
- Modifying the biometric or CREATE/REMOVE passcode flows.

---

> Affects both oCIS and Classic — passcode lock is a client-side feature independent of server backend.
