## 1. Hide keyboard during brute-force countdown

- [x] 1.1 In `PassCodeActivity.lockScreen()`, set `binding.numberKeyboard.visibility = View.INVISIBLE` after disabling the EditTexts
- [x] 1.2 In the `getFinishedTimeToUnlockLiveData` observer, set `binding.numberKeyboard.visibility = View.VISIBLE` alongside the existing EditText re-enable logic

## 2. Clear error message when countdown ends

- [x] 2.1 In the `getFinishedTimeToUnlockLiveData` observer, set `binding.passcodeError.visibility = View.INVISIBLE` so the error is cleared when the user is allowed to try again

## 3. Verify

- [x] 3.1 Run `./gradlew detekt` and confirm no new warnings
- [ ] 3.2 Build and manually test the brute-force flow (see Test Plan below)

## 4. Changelog

- [x] 4.1 Add a calens entry at `changelog/XXXX.md` (replace XXXX with the PR number once created)

---

## Test Plan

| Test Name | Steps | Expected Result | Outcome | Comments |
|-----------|-------|-----------------|---------|----------|
| Keyboard hidden on lockout | 1. Open app with passcode enabled. 2. Enter 3 wrong passcodes. | The number keyboard disappears as soon as the countdown starts. | | |
| Keyboard non-interactive during countdown | 1. Reach the lockout state. 2. Tap the area where the keyboard was. | No digit is entered; no visual response. | | |
| Keyboard restored after countdown | 1. Reach the lockout state. 2. Wait for the countdown to reach zero. | The number keyboard reappears and digits can be entered. | | |
| Error message cleared after countdown | 1. Enter 3 wrong passcodes (error message appears). 2. Wait for countdown to finish. | The error message is no longer visible when the countdown ends. | | |
| Correct passcode accepted after lockout | 1. Reach lockout, wait for countdown. 2. Enter the correct passcode. | App unlocks successfully. | | |
