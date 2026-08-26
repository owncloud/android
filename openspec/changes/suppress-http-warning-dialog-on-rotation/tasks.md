## 1. Implementation

- [x] 1.1 Add a nullable `acceptedInsecureHttpUrl: String?` field on `LoginActivity` and a `KEY_INSECURE_HTTP_URL_ACCEPTED` constant in the same file (`owncloudApp/src/main/java/com/owncloud/android/presentation/authentication/LoginActivity.kt`). Verify by grepping the file for both symbols and by confirming the field starts as `null` on a fresh Activity instance.
- [x] 1.2 In `onSaveInstanceState` (`LoginActivity.kt:901-904`), write `acceptedInsecureHttpUrl` under `KEY_INSECURE_HTTP_URL_ACCEPTED`; in `onCreate` (`LoginActivity.kt:137-144`), restore it alongside `authTokenType` when `savedInstanceState != null`. Verify by adding a `Timber.d` breadcrumb, rotating the device after accepting the dialog, and confirming the restored value appears in Logcat.
- [x] 1.3 In `getServerInfoIsSuccess` (`LoginActivity.kt:333-378`), before the `AlertDialog.Builder` block inside the `!isSecureConnection` branch, short-circuit to `checkServerType(serverInfo)` when `acceptedInsecureHttpUrl == serverInfo.baseUrl`. Keep the `serverStatusText` text/icon update outside the guard so the "connection established" state is drawn on both paths. Verify by triggering three rapid rotations after tapping **Continue** and observing no dialog on any redraw.
- [x] 1.4 In the positive-button callback inside the same `AlertDialog.Builder` block (`LoginActivity.kt:365-367`), set `acceptedInsecureHttpUrl = serverInfo.baseUrl` before calling `checkServerType(serverInfo)`. Leave the negative-button callback (`LoginActivity.kt:368-370`) unchanged. Verify by pressing **Cancel** first, rotating the device, and confirming the dialog reappears; then pressing **Continue** and confirming subsequent rotations no longer show it.
- [x] 1.5 In the `doAfterTextChanged` reset branch on `binding.hostUrlInput` (`LoginActivity.kt:340-350`), clear `acceptedInsecureHttpUrl = null` alongside the existing reset logic. Verify by accepting the warning for `http://server-a.example`, editing the field to `http://server-b.example`, re-checking, and observing the dialog reappears.
- [x] 1.6 Run `./gradlew detekt` and fix any style/formatting issues introduced by the change. Verify by a clean Detekt exit code.

## 2. Changelog

- [ ] 2.1 Add a calens entry at `changelog/unreleased/<PR-number>` (extensionless plain-text file, PR-number placeholder replaced once the PR is opened) with the content:

  ```
  Bugfix: Stop re-showing the insecure-HTTP warning dialog on rotation

  The insecure-HTTP warning shown on the login screen for non-TLS server URLs no longer re-appears after the user accepted it and rotated the device or triggered another configuration change. The dialog is shown again if the user edits the server URL or reopens the login screen.

  https://github.com/owncloud/android/issues/4386
  https://github.com/owncloud/android/pull/<PR-number>
  ```

  Do not mark this task done until the PR exists and its number replaces the `<PR-number>` placeholder. Verify by opening the file after the PR is created and confirming both URLs resolve.