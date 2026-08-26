## Why

Issue [#4386](https://github.com/owncloud/android/issues/4386): on the login screen, when the user enters an `http://` (non-TLS) server URL, an insecure-connection warning dialog is shown. If the user taps **Continue** and then rotates the device, the dialog reappears — and reappears on every subsequent rotation. The user is forced to re-accept the same warning after each configuration change, even though the acceptance is meaningful for the current login session.

## What Changes

- The insecure-HTTP warning dialog on the login screen is shown at most once per login session per accepted host. After the user taps **Continue**, the dialog does not reappear on device rotation, dark-mode change, or any other configuration change for the same server URL.
- If the user taps **Cancel**, the acceptance is not recorded (the dialog still surfaces the next time the user re-checks the same host, which matches today's behavior).
- If the user edits the server URL after accepting the warning, the acceptance is discarded and the dialog is shown again the next time an `http://` result is received for the new URL.
- Only the login-screen dialog is affected. The security semantics (still an insecure connection, still gated behind a warning) are unchanged.

## Capabilities

### New Capabilities
- `login-insecure-http-warning`: On the login screen, the user-facing behavior of the insecure-HTTP warning dialog that appears when a non-TLS server URL is entered — when it is shown, when it is suppressed, and how it interacts with configuration changes and URL edits.

### Modified Capabilities
<!-- None; this change introduces the first spec for this capability. -->

## Impact

- Affected module: `owncloudApp` (presentation layer only). Changes are limited to `LoginActivity` and its state-persistence surface (`onSaveInstanceState` / `onRestoreInstanceState`, or an equivalent retained flag).
- No changes to `owncloudDomain`, `owncloudData`, or `owncloudComLibrary`.
- No changes to `AuthenticationViewModel` public surface, LiveData contracts, or SharedPreferences keys.
- No dependency changes.
- Applies to both backends (ownCloud Infinite Scale / oCIS and ownCloud Classic) — the dialog fires on the shared login flow before backend type is determined.

## Non-Goals

- No change to the security policy of allowing HTTP connections: the dialog still gates the flow the first time it appears and the connection is still recognized as insecure.
- No change to how the dialog is rendered, worded, or styled.
- No persistence of the acceptance beyond the current login session — closing the login screen and returning still shows the warning again.
- No change to the certificate/SSL-error dialog path (`SslUntrustedCertDialog`), which is a separate flow.
- No change to the equivalent path on the file-transfer or account-details screens; only the login screen is in scope.