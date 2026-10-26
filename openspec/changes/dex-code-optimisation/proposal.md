## Why

The release APK published on Google Play has DEX code optimization disabled because `minifyEnabled` is not set in `owncloudApp/build.gradle`. Google Play Console flags this in Android Vitals and requires it to be fixed by February 2027. Enabling R8 also reduces APK size.

## What Changes

- Enable `minifyEnabled true` in the `release` build type of `owncloudApp/build.gradle`.
- Add `proguardFiles` referencing `getDefaultProguardFile('proguard-android-optimize.txt')` and a new `owncloudApp/proguard-rules.pro`.
- Create `owncloudApp/proguard-rules.pro` with keep rules for: `CoroutineWorker` subclasses in `com.owncloud.android.workers`, Moshi `@JsonClass`-annotated response models in `owncloudComLibrary`, domain model classes serialized to Room via Moshi reflection (`AppRegistryProvider`), custom `View` and `Preference` subclasses inflated from XML by the Android framework, and `PreferenceFragmentCompat` subclasses loaded by name via `app:fragment` in preference XML.
- Debug builds remain unaffected (`minifyEnabled false`).
- Library modules (`owncloudData`, `owncloudDomain`, `owncloudComLibrary`) are not changed.

## Capabilities

### New Capabilities

- `r8-optimisation`: Release build configuration requirements — minification enabled, keep rules covering Workers, Moshi models, and reflection-loaded classes.

### Modified Capabilities

None — no existing spec-level behavior changes. The app's observable user-facing functionality is unchanged.

## Impact

- **`owncloudApp/build.gradle`**: `release` build type gains `minifyEnabled true` and `proguardFiles`.
- **`owncloudApp/proguard-rules.pro`** (new file): keep rules for Workers, Moshi models, domain models serialized to Room via reflection (`AppRegistryProvider`), and reflection-loaded classes.
- No new runtime dependencies introduced.
