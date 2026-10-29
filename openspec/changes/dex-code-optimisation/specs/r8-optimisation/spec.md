## Purpose

Defines the build requirements for R8 code shrinking and optimization in release builds of the ownCloud Android app, ensuring APKs pass Google Play's DEX optimization check.

## ADDED Requirements

### Requirement: Release builds apply R8 code shrinking

The `owncloudApp` release build type SHALL have `minifyEnabled true` set, causing the AGP/R8 toolchain to shrink and optimize the DEX code.

#### Scenario: Release APK is produced with R8 enabled

- **WHEN** `./gradlew assembleOriginalRelease` is executed
- **THEN** the build completes without errors and produces a valid, installable APK at `owncloudApp/build/outputs/apk/original/release/`

#### Scenario: Debug builds remain unminified

- **WHEN** `./gradlew assembleOriginalDebug` is executed
- **THEN** `minifyEnabled` is false for the debug build type and no shrinking is applied

### Requirement: Keep rules preserve reflection-loaded and framework-instantiated classes

The ProGuard/R8 rules file SHALL include keep rules that prevent shrinking or renaming of: all `CoroutineWorker` subclasses in `com.owncloud.android.workers`, Moshi `@JsonClass`-annotated response model classes in `owncloudComLibrary`, and any other classes instantiated by name via reflection or the Android framework.

#### Scenario: WorkManager workers are instantiated correctly at runtime

- **WHEN** a background sync or upload WorkRequest is enqueued and executed in a release build
- **THEN** WorkManager successfully instantiates the worker class by its registered name with no `ClassNotFoundException`

#### Scenario: Moshi JSON deserialization succeeds in release builds

- **WHEN** the app deserializes a JSON response from the ownCloud server in a release build
- **THEN** all annotated fields are correctly mapped and no deserialization errors occur due to renamed fields

#### Scenario: App registry providers survive an app update in release builds

- **WHEN** the app fetches and caches app registry data (app providers) in a release build and the app is subsequently updated to a new release build
- **THEN** the cached app providers are deserialized correctly from Room and displayed in the "open in web" / office app menu without silent data loss

#### Scenario: Settings sub-screens open correctly in release builds

- **WHEN** the user taps any item in the Settings screen in a release build
- **THEN** the corresponding settings sub-screen opens without a `ClassNotFoundException`

### Requirement: Library modules are not affected

The `owncloudComLibrary`, `owncloudData`, and `owncloudDomain` library modules SHALL keep `minifyEnabled false`; only `owncloudApp` is changed.

#### Scenario: Library module builds are unchanged

- **WHEN** any library module is built independently
- **THEN** its build configuration is identical to before this change (no minification applied)
