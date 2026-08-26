## 1. Implementation

- [x] 1.1 In `PassCodeActivity.lockScreen()` (`owncloudApp/src/main/java/com/owncloud/android/presentation/security/passcode/PassCodeActivity.
kt:339-348`), set `binding.numberKeyboard.visibility = View.GONE` in the same branch that sets `binding.lockTime.visibility = View.VISIBLE`. Verify by triggering three wrong passcode attempts on a debug build and observing that the on-screen numeric keypad disappears while the countdown is displayed.
- [x] 1.2 In the `getFinishedTimeToUnlockLiveData` observer (`PassCodeActivity.kt:222-228`), set `binding.numberKeyboard.visibility = View.VISIBLE` and `binding.passcodeError.visibility = View.INVISIBLE` alongside the existing re-enable-and-refocus logic. Verify by letting the countdown finish and confirming the keypad reappears, taps enter digits again, and the "Incorrect passcode" text is no longer on screen.
- [x] 1.3 Confirm no additional call sites need editing (grep for `binding.lockTime` inside `PassCodeActivity.kt`; every site must also touch `binding.numberKeyboard`). Verify by inspection.

## 2. Layout verification

- [x] 2.1 Inspect `owncloudApp/src/main/res/layout/passcode_lock_activity.xml` and `owncloudApp/src/main/res/layout-sw720dp-land/passcode_lock_activity.xml` to confirm both include `@id/number_keyboard` and that no other view depends on the keypad being visible for its position (only `lock_time`'s bottom constraint references it, which continues to work with `GONE`). Verify by opening the layout preview on both configurations with `number_keyboard` set to `GONE`.
- [~] 2.2 Sanity-check the tablet-landscape variant on an emulator by triggering the lockout and confirming the countdown remains readable without the keypad. Verify by observation. **Skipped:** no `sw720dp` emulator available in this environment. The XML structure of `layout-sw720dp-land/passcode_lock_activity.xml` was inspected in task 2.1 and is analogous to the portrait variant (same `@id/number_keyboard`, same downstream constraints — only `lock_time`'s bottom depends on the keypad); code-only visibility toggle applies without a layout change.

## 3. Cold-start path

- [x] 3.1 Persist an attempt count over the threshold, kill the app, relaunch it, and confirm the passcode screen opens with the countdown visible and the keypad hidden from the first frame (no flash of the keypad). Verify by observation.

## 4. Regression checks

- [x] 4.1 Verify hardware numeric keys and Backspace do nothing while the countdown is on screen (digit fields are already disabled; behavior must not change). Verify with a physical or emulated hardware keyboard during lockout.
- [x] 4.2 Verify the "Incorrect passcode" text still appears on each wrong attempt and still clears in the existing happy-path handlers (`actionCheckOk`, `actionCheckMigration`, `actionCreateNoConfirm`). Only the moment-of-clear at the end of the lockout is new. Verify by exercising a correct passcode entry, a wrong-then-correct entry, and a lockout-then-first-attempt entry.
- [x] 4.3 Run the app's existing unit and instrumentation test suites for the security/passcode package. Verify by confirming all previously passing tests still pass.