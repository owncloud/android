## Context

See proposal.md — Why. Relevant current state on `PassCodeActivity`:

- The screen has three input surfaces: per-digit `EditText` boxes (`passcode_value`), an on-screen custom keypad (`R.id.number_keyboard`, `com.owncloud.android.presentation.security.passcode.NumberKeyboard`), and hardware keys handled in `onKeyUp` (`PassCodeActivity.kt:449-473`).
- System soft input is force-hidden at `PassCodeActivity.kt:87`, so "hide the keyboard" in the issue refers to this on-screen `NumberKeyboard`, not the system IME.
- The lockout is entered in three places:
  - `onCreate` at line 126–128 when a persisted attempt counter is already over the threshold on cold start.
  - `actionCheckError` at line 298–300 immediately after the third+ wrong attempt in-session.
  - Both call `lockScreen()` (lines 339–348), which sets `binding.lockTime.visibility = VISIBLE`, disables digit `EditText`s, and starts the ViewModel timer.
- The lockout is exited in `getFinishedTimeToUnlockLiveData` observer (lines 222–228), which hides `lockTime` and re-enables digit fields.
- Two `passcode_lock_activity.xml` layouts exist (portrait / `sw720dp-land`); both include the `number_keyboard` view with the same id.

Constraint: `PassCodeViewModel` already exposes the two LiveData events needed (start-of-lockout, end-of-lockout) and does not know about views. Keep it that way.

## Goals / Non-Goals

**Goals:**
- Toggle `NumberKeyboard` visibility strictly in lockstep with the existing `lock_time` visibility.
- Clear `passcode_error` when the countdown finishes, at the same site that already hides `lock_time` and re-enables input.
- Change nothing about timing, attempt counting, or persistence.
- Cover the three lockout-entry paths and the one lockout-exit path with a single mechanism.

**Non-Goals:**
- No changes to `PassCodeViewModel`, its LiveData, or `SharedPreferences` keys.
- No changes to when `passcode_error` first appears or to its content — only when it disappears at the natural end of the lockout.
- No new components, no changes to the `NumberKeyboard` custom view itself.
- No layout restructuring beyond what visibility toggling requires.

## Decisions

### Decision 1: Toggle visibility from `PassCodeActivity`, not from the layout

- **Choice**: Set `binding.numberKeyboard.visibility` in the same code paths that already toggle `binding.lockTime` (`lockScreen()` → set `GONE`; `getFinishedTimeToUnlockLiveData` observer → set `VISIBLE`).
- **Why**: Those two sites are already the single source of truth for lockout UI state; adding one line to each keeps the two views in sync without introducing new state.
- **Alternative considered**: Bind `numberKeyboard` visibility to `lockTime` via a `ConstraintSet` / motion layout. Rejected — heavier than needed for two call sites.
- **Alternative considered**: Add a new LiveData in `PassCodeViewModel` (`isKeypadVisible`). Rejected — the two existing events already carry this signal; adding a third LiveData would duplicate state.

### Decision 2: Use `View.GONE`, not `View.INVISIBLE`

- **Choice**: `GONE` when the countdown is on, `VISIBLE` when it ends.
- **Why**: `INVISIBLE` would keep the keypad occupying its slot below `lock_time`, defeating the visual goal (the iOS reference removes the keypad). `GONE` collapses the space so the countdown sits naturally in the layout.
- **Constraint check**: `lock_time` in the portrait layout constrains its bottom to `number_keyboard` top (`passcode_lock_activity.xml:104`). With `number_keyboard` `GONE`, ConstraintLayout still treats it as a reference for constraints, so `lock_time` positioning is unaffected. Verify the same on the `sw720dp-land` variant when implementing.

### Decision 3: Apply the toggle on cold-start lockout too

- **Choice**: The `onCreate` cold-start path (line 126–128) calls `lockScreen()`; adding the visibility change inside `lockScreen()` covers it automatically. No extra call site needed.

### Decision 4: Clear `passcode_error` in the countdown-finished observer, not on a timer

- **Choice**: In the `getFinishedTimeToUnlockLiveData` observer (`PassCodeActivity.kt:222-228`), set `binding.passcodeError.visibility = View.INVISIBLE` alongside the existing hide-`lockTime` / re-enable-inputs logic.
- **Why**: This is the single point at which the app already knows the lockout has ended. `passcodeError` is already toggled to `INVISIBLE` in the other happy-path handlers (`actionCheckOk`, `actionCheckMigration`, `actionCreateNoConfirm`); using `INVISIBLE` here is consistent with the existing convention and keeps the layout stable.
- **Alternative considered**: Clear the error inside the `CountDownTimer.onFinish` in the ViewModel. Rejected — the ViewModel is view-agnostic and does not touch view state; the existing LiveData event already exposes the exact moment we need on the UI side.
- **Alternative considered**: Clear the error only when the user starts typing again. Rejected — the issue explicitly asks that the error be gone *when the counter finishes*, before the next attempt.

## Risks / Trade-offs

- **Risk**: A device orientation change (recreation) while the lockout is active could flash the keypad before the observer fires. → Mitigation: the visibility change lives inside `lockScreen()`, which `onCreate` calls before the countdown is displayed; the initial layout state is set before the first frame.
- **Risk**: `sw720dp-land` variant has a different layout tree; a code-only change could miss a variant-specific tweak. → Mitigation: verify at implementation time that the `number_keyboard` id and its constraints exist in both layouts and behave identically when set to `GONE`.
- **Trade-off**: We deliberately do not decouple keypad visibility from timer visibility. Any future feature that wants them independent will need a small refactor. Acceptable — no such feature is on the horizon.

## Migration Plan

Not applicable. Pure UI behavior change, no persisted state, no rollout gating needed. Rollback is a code revert.