## 1. Build Configuration

- [x] 1.1 Add `minifyEnabled true` to the `release` build type in `owncloudApp/build.gradle`
- [x] 1.2 Add `proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'` to the `release` build type in `owncloudApp/build.gradle`
- [x] 1.3 Verify that the `debug` build type in `owncloudApp/build.gradle` has no `minifyEnabled` change (remains false by default)
- [x] 1.4 Create `owncloudApp/proguard-rules.pro` as a minimal stub (header comment only) so the `proguardFiles` reference added in task 1.2 is immediately satisfied and `assembleOriginalRelease` does not fail with "Supplied proguard configuration does not exist". Keep rules are populated in Group 2.

## 2. ProGuard Rules

- [x] 2.1 Create `owncloudApp/proguard-rules.pro` with keep rules for all `CoroutineWorker` subclasses in `com.owncloud.android.workers` (9 workers: `OldLogsCollectorWorker`, `AutomaticUploadsWorker`, `AvailableOfflinePeriodicWorker`, `DownloadFileWorker`, `RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker`, `AccountDiscoveryWorker`, `UploadFileFromFileSystemWorker`, `RemoveSourceFileWorker`, `UploadFileFromContentUriWorker`)
- [x] 2.2 Add keep rules to `owncloudApp/proguard-rules.pro` for all Moshi `@JsonClass`-annotated response model classes in `owncloudComLibrary` (covering: `CommonOcsResponse`, `ShareeResponse`, `ShareItem`, `TokenResponse`, `ClientRegistrationResponse`, `OIDCDiscoveryResponse`, `WebFingerResponse`, `AppRegistryResponse`, `SpacesResponse`, `RoleResponse`, `CapabilityResponse`, `UserInfoResponse`, `MembersResponse`, and related)
- [x] 2.3 Add class-level keep rules for any other classes instantiated by name via the Android framework: custom `View` subclasses inflated from layout XML (`SortOptionsView`, `NumberKeyboard`, `SquareFrameLayout`, `ViewPagerWorkAround`); `PreferenceFragmentCompat` subclasses loaded by name via `app:fragment` in `settings.xml` (`SettingsSecurityFragment`, `SettingsLogsFragment`, `SettingsPictureUploadsFragment`, `SettingsVideoUploadsFragment`, `SettingsAdvancedFragment`, `SettingsMoreFragment`); and `LargePreferenceCategory` referenced by name in preference XML.

## 3. First Build and Rule Verification

- [ ] 3.1 Run `./gradlew assembleOriginalRelease` and resolve any build errors
- [x] 3.2 Inspect `owncloudApp/build/outputs/mapping/originalRelease/configuration.txt` to verify that Room consumer rules and Coroutines rules were applied automatically, and that all custom keep rules appear in the merged output
- [x] 3.3 Inspect `owncloudApp/build/outputs/mapping/originalRelease/usage.txt` to check for any incorrectly removed classes and add additional keep rules if needed
- [x] 3.4 Confirm mapping file is present at `owncloudApp/build/outputs/mapping/originalRelease/mapping.txt`

## 4. Verification

- [ ] 4.1 Run `./gradlew testDebugUnitTest testMdmDebugUnitTest --continue` and confirm all unit tests pass
- [ ] 4.2 Run `./gradlew detekt` and confirm zero issues
- [x] 4.3 Install the release APK on a device or emulator and perform the smoke test: login, file listing, upload, download
- [x] 4.4 Trigger a background sync (automatic upload) and confirm it completes without a `ClassNotFoundException` from WorkManager

## 5. Changelog

- [x] 5.1 Create `changelog/unreleased/5032` with the following content:

```
Enhancement: Enable R8 code shrinking for release builds

R8 code shrinking has been enabled for release builds. The APK is now smaller and passes Google Play's DEX optimisation check.

https://github.com/owncloud/android/issues/5029
https://github.com/owncloud/android/pull/XXXX
```

---

## QA

### Test Plan

| Test name | Steps to execute | Expected result | Result | Comments |
|---|---|---|---|---|
| Release build compiles | Run `./gradlew assembleOriginalRelease` | Build succeeds, APK produced | | |
| Mapping file generated | Check `owncloudApp/build/outputs/mapping/originalRelease/mapping.txt` after release build | File exists and is non-empty | | |
| Debug build unaffected | Run `./gradlew assembleOriginalDebug` | Build succeeds, no minification applied | | |
| Unit tests pass | Run `./gradlew testDebugUnitTest testMdmDebugUnitTest` | All tests green | | |
| Detekt zero issues | Run `./gradlew detekt` | Zero issues reported | | |
| Login works in release APK | Install release APK, open app, log in to an ownCloud server | Login completes, files listed | | |
| File listing works | Navigate through folders in release APK | Folders and files display correctly | | |
| File upload works | Upload a file from the release APK | File appears on server | | |
| File download works | Download a file in the release APK | File saved locally and opens correctly | | |
| Background sync works | Trigger automatic upload via release APK | Upload worker completes with no `ClassNotFoundException` | | |
| Moshi deserialization works | Perform any server request returning JSON (e.g. capabilities, spaces) in release APK | Response parsed correctly, no deserialization errors | | |
| Settings navigation works | Tap each item in the Settings screen in a release APK | Each sub-screen opens without a crash (`ClassNotFoundException`) | | |

### Gherkin Scenario Proposal (E2E suite)

```gherkin
Feature: Release build integrity with R8 optimisation

  Scenario: User can log in and list files with a release build
    Given the ownCloud Android app is installed from a release APK with R8 optimisation enabled
    And a valid ownCloud server is accessible
    When the user enters their credentials and taps the login button
    Then the app navigates to the file list screen
    And files and folders from the server are displayed correctly

  Scenario: Background worker completes without reflection errors
    Given the ownCloud Android app is running as a release build
    And automatic uploads are enabled for a local folder
    When a new file is added to the watched folder
    Then the AutomaticUploadsWorker is enqueued and executed by WorkManager
    And the file is uploaded to the ownCloud server without a ClassNotFoundException
```
