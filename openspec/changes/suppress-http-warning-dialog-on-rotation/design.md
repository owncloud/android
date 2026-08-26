## Context

See proposal.md — Why. Relevant current state on `LoginActivity`:

- The insecure-HTTP warning is built and shown inline inside `LoginActivity.getServerInfoIsSuccess()` at `owncloudApp/src/main/java/com/owncloud/android/presentation/authentication/LoginActivity.kt:353-376`. There is no "already accepted" guard; every entry into the `!isSecureConnection` branch calls `show()` on a fresh `AlertDialog.Builder`.
- The observer wiring uses `Event.peekContent()` (`LoginActivity.kt:234`), so the retained `UIResult.Success<ServerInfo>` re-emits on rotation and re-runs `getServerInfoIsSuccess`. The `Event`'s `hasBeenHandled` guard (`owncloudDomain/src/main/java/com/owncloud/android/domain/utils/Event.kt:31-37`) is deliberately bypassed here — switching to `getContentIfNotHandled()` (or `EventObserver`) is one candidate mechanism.
- `onSaveInstanceState` (`LoginActivity.kt:901-904`) currently persists only `authTokenType`. `authenticationViewModel.serverInfo` is a `MediatorLiveData` retained across rotations via ViewModel scope (`AuthenticationViewModel.kt:82-83`), so any per-URL acceptance state must survive alongside it.
- The URL edit path already resets downstream login state via `doAfterTextChanged` on `binding.hostUrlInput` (`LoginActivity.kt:340-350`); acceptance state must be reset at the same site.
- There is no XML change involved — the dialog is built entirely in code and does not have layout variants.

Constraint: `AuthenticationViewModel` is view-agnostic and shared across screens. Keep it that way — do not add view-lifecycle acceptance state to it.

## Goals / Non-Goals

**Goals:**
- Suppress the dialog on any re-entry into `getServerInfoIsSuccess` for a URL the user has already accepted this login session.
- Preserve the current dialog rendering, wording, and both button paths.
- Cover configuration changes and any programmatic re-check of the same URL with the same mechanism.
- Discard the acceptance when the user edits the URL, at the existing reset site.

**Non-Goals:**
- No changes to `AuthenticationViewModel`, its LiveData, or any use case in `owncloudDomain` / `owncloudData`.
- No new SharedPreferences keys and no persistence across app restarts — acceptance is scoped to the current `LoginActivity` instance and its saved-state bundle.
- No refactor of the `Event` wrapper's semantics beyond swapping which method this observer calls.

## Decisions

### Decision 1: Track "accepted URL" as `LoginActivity` state, saved into `onSaveInstanceState`

- **Choice**: Add a nullable `private var acceptedInsecureHttpUrl: String? = null` on `LoginActivity`. Save it in `onSaveInstanceState` (`LoginActivity.kt:901-904`) under a new `KEY_INSECURE_HTTP_URL_ACCEPTED` constant, and restore it in `onCreate` (`LoginActivity.kt:137-144`) alongside `authTokenType`.
- **Why**: The acceptance is a UI decision tied to the current login session, not to server behavior — putting it on the Activity keeps `AuthenticationViewModel` clean and matches the existing pattern for `authTokenType`. Saving into the bundle is the standard survive-rotation mechanism and does not create any persistent state.
- **Alternative considered**: Add the flag to `AuthenticationViewModel`. Rejected — the ViewModel is shared with logic that has no view concept, and adding UI-acceptance state would leak view concerns into it.
- **Alternative considered**: Switch the observer to `getContentIfNotHandled()` / `EventObserver` so the `Event`'s built-in `hasBeenHandled` guard suppresses re-delivery. Rejected as the sole mechanism — the same `Event` also drives `getServerInfoIsLoading` / `getServerInfoIsError` handling on rotation for the *secure* branch and for error states via `peekContent`; flipping this observer would silently skip re-drawing the "connection established" / error text on rotation. It could be combined with Decision 1, but the state-flag alone is enough and keeps the observer's re-draw semantics unchanged.
- **Alternative considered**: A SharedPreferences flag keyed by URL. Rejected — acceptance persisting across app restarts contradicts the proposal's Non-Goals and reduces the effective friction of the warning.

### Decision 2: Guard `show()` inside `getServerInfoIsSuccess`, keyed on the current base URL

- **Choice**: In `getServerInfoIsSuccess` (`LoginActivity.kt:333-378`), before entering the `else` branch that builds and shows the dialog, compare `acceptedInsecureHttpUrl` to `baseUrl` (from the incoming `ServerInfo`). If they match, call `checkServerType(serverInfo)` directly and skip the `AlertDialog.Builder` block entirely, but still set the "connection established" `serverStatusText` and open-lock icon so the UI reads consistently.
- **Why**: Anchoring the check to `baseUrl` (not the raw text in `hostUrlInput`) uses the normalized value the app already computed, and matches the value the URL-edit reset uses. Setting the status text unconditionally keeps the rotation frame visually identical to the pre-rotation frame.
- **Alternative considered**: Track acceptance only as a boolean. Rejected — a boolean cannot distinguish "the user accepted for host A" from "then edited to host B and we haven't checked yet." Storing the URL supports the "URL edit invalidates acceptance" requirement without extra state.

### Decision 3: Record acceptance in the **Continue** callback; do not record on **Cancel**

- **Choice**: In the positive-button callback (`LoginActivity.kt:365-367`), set `acceptedInsecureHttpUrl = serverInfo.baseUrl` before invoking `checkServerType(serverInfo)`. Leave the negative-button callback (`LoginActivity.kt:368-370`) unchanged.
- **Why**: This directly encodes the spec: acceptance is registered only when the user affirmatively continues. Cancel keeps the flow at "credentials hidden" and lets the next check re-show the warning.

### Decision 4: Reset acceptance in the existing URL-edit hook

- **Choice**: In the `doAfterTextChanged` block on `binding.hostUrlInput` (`LoginActivity.kt:340-350`), inside the same branch that already resets `showOrHideBasicAuthFields` / `loginButton` / `serverStatusText`, set `acceptedInsecureHttpUrl = null`.
- **Why**: This is already the single site that reacts to the user modifying the URL. Adding one line keeps the reset logic co-located and avoids introducing a second URL-change listener.
- **Note**: The block runs on any programmatic `setText` on `hostUrlInput`, including the one at `LoginActivity.kt:339` that writes the normalized `baseUrl` after a successful server check. That call feeds text equal to `baseUrl`, so the inner `baseUrl != binding.hostUrlInput.text.toString()` guard prevents a spurious reset; the acceptance clear lives inside that same guard.

### Decision 5: No changes to LiveData contracts, XML layouts, or SharedPreferences

- **Choice**: The observer keeps calling `event.peekContent()`. No new `Event` handling. No changes to `account_setup.xml` (the dialog is code-built). No new SharedPreferences keys — only a Bundle key on the Activity's saved-instance state.
- **Why**: Keeps the blast radius to a single Activity file. The change is one field, one save/restore pair, one guard, and one reset — no coupling to other screens or the domain layer.

## Risks / Trade-offs

- **Risk**: If a future refactor moves normalization of the URL out of `ServerInfo.baseUrl`, the equality check in Decision 2 could false-negative and re-show the dialog. → Mitigation: keep the check anchored to `baseUrl` (the same value used elsewhere for equality in `LoginActivity`); any future change to that field will already have to touch the same file.
- **Risk**: `onSaveInstanceState` runs only when the Activity is expecting recreation. If the process is killed while backgrounded and the user returns to the login screen from a task-manager-restored task, the acceptance is dropped. → Trade-off: acceptable, and consistent with the proposal's Non-Goal ("no persistence beyond the login session"). The dialog will show once more; correctness is preserved.
- **Risk**: A brand flow (`checkPasscodeEnforced`, MDM URL) that pre-fills `hostUrlInput` and immediately triggers `checkOcServer` could bypass any first-load dialog if the process was recreated mid-flow. → Mitigation: only the acceptance state is restored; the dialog logic still runs on the restored `UIResult.Success` and is only suppressed when the accepted URL matches. Fresh flows without a saved acceptance behave exactly as today.
- **Trade-off**: We deliberately do not centralize insecure-HTTP acceptance across the app. Other insecure-URL entry points (documents provider, account re-auth) are out of scope and continue their existing behavior.

## Migration Plan

Not applicable. Pure UI behavior change, no persisted state, no rollout gating needed. Rollback is a code revert of the single file.