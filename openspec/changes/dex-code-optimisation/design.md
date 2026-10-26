## Context

See proposal.md — Why for motivation. The `owncloudApp/build.gradle` release build type currently has no `minifyEnabled` setting (defaults to `false`). No `proguard-rules.pro` exists in `owncloudApp/`. A legacy `proguard.cfg` exists at the repo root but is not referenced by any build script. AGP 9.x uses R8 by default; no explicit R8 toolchain switch is needed.

## Goals / Non-Goals

**Goals:**
- Enable R8 code shrinking for `owncloudApp` release builds only.
- Author keep rules sufficient to prevent runtime breakage from missing classes.

**Non-Goals:**
- Enabling minification in library modules or debug builds.
- Adding obfuscation suppression (`-dontobfuscate`) — obfuscation stays on.
- Integrating a crash-reporting SDK or automating mapping file upload.

## Decisions

### D1: Use `getDefaultProguardFile('proguard-android-optimize.txt')` as the base rules file

**Decision:** Reference `proguard-android-optimize.txt` (not `proguard-android.txt`) as the AGP-provided base rules file.

**Rationale:** `proguard-android-optimize.txt` enables code-level optimizations (constant propagation, dead code removal across methods) that `proguard-android.txt` does not. This maximises APK size reduction and aligns with Google's current recommendation for new projects.

**Alternative considered:** `proguard-android.txt` — less aggressive, larger output, not needed given we are starting from scratch with R8.

### D2: Use a single `owncloudApp/proguard-rules.pro` file for app-specific keep rules

**Decision:** All custom keep rules live in one file at `owncloudApp/proguard-rules.pro`, referenced alongside the AGP default.

**Rationale:** Library consumer rules (Room, Kotlin Coroutines, OkHttp, Moshi, Koin) are already bundled with each library's AAR and applied automatically by R8. Only app-level classes that R8 cannot infer are instantiated by name (WorkManager workers, classes loaded via reflection) need explicit keep rules. A single file is easy to audit and extend.

**Alternative considered:** Splitting rules per concern into multiple `.pro` files — unnecessary complexity for the volume of custom rules needed here.

### D3: Keep rules strategy by class category

| Category | Rule strategy | Rationale |
|---|---|---|
| Custom `View` subclasses inflated from layout XML (`SortOptionsView`, `NumberKeyboard`, `SquareFrameLayout`, `ViewPagerWorkAround`) | No explicit rule needed — covered automatically by AAPT | AGP's AAPT step scans layout XML files and emits keep rules into `aapt_rules.txt` for every custom view referenced by class name. R8 merges these automatically; duplicating them in `proguard-rules.pro` is redundant. |
| `CoroutineWorker` subclasses in `com.owncloud.android.workers` | `-keep class com.owncloud.android.workers.** { public <init>(...); }` | WorkManager instantiates workers by class name from the `WorkRequest`; renaming breaks lookup |
| Moshi `@JsonClass`-annotated models in `owncloudComLibrary` | `-keep @com.squareup.moshi.JsonClass class * { *; }` | KSP generates adapters at compile time, but the annotated model fields must not be renamed or JSON field mapping breaks |
| Room entities and DAOs | Already covered by Room's bundled consumer rules — verify after first build by inspecting `configuration.txt` | Room ships its own `-keep` rules in its AAR |
| Koin wiring | No keep rules needed | Koin resolves dependencies via reified type parameters, not reflection on class names |
| `PreferenceFragmentCompat` subclasses in `settings.xml` | `-keep class * extends androidx.preference.PreferenceFragmentCompat { public <init>(...); }` | `androidx.preference` loads them by class name at runtime; a wildcard rule mirrors the workers pattern and avoids silent gaps when screens are added or renamed. `LargePreferenceCategory` (extends `Preference`, not `PreferenceFragmentCompat`) kept with an explicit rule. |
| Domain model classes serialized to Room via Moshi reflection (`AppRegistryProvider` in `owncloudDomain`) | `-keep class com.owncloud.android.domain.appregistry.model.AppRegistryProvider { *; }` | `AppRegistryProvider` has no `@JsonClass` annotation and is serialized by `KotlinJsonAdapterFactory` using reflected field names as JSON keys. Without this rule R8 renames the fields freely; the JSON format stored in Room becomes unstable across releases and app providers silently disappear after an update. Adding `@JsonClass` is not an option as it would introduce a Moshi dependency into the domain layer. |
| All other classes | Default R8 behavior (shrink + obfuscate) | No known issues; the `configuration.txt` dump after the first build is the safety net |

### D4: Verify merged rules after the first successful release build

**Decision:** Inspect `owncloudApp/build/outputs/mapping/originalRelease/configuration.txt` after the first successful `assembleOriginalRelease` to confirm all consumer rules were applied and spot any unexpected gaps before opening the PR.

**Rationale:** R8 merges rules cumulatively (AGP default + `proguard-rules.pro` + library consumer rules). The merged dump is the ground truth; reviewing it catches over-keeping and under-keeping early.

## Risks / Trade-offs

- **[Risk] Keep rules are incomplete** → Mitigation: Run the release APK on a device/emulator and exercise login, file listing, upload, download, and background sync before opening the PR. Missing keep rules typically surface as `ClassNotFoundException` or Moshi deserialization errors at runtime, not at build time.
- **[Risk] R8 removes code that is used only via reflection (e.g. custom View classes inflated from XML, `PreferenceFragmentCompat` subclasses in preference XML)** → Mitigation: Review the R8 usage report (`usage.txt` in the mapping output directory) and add `@Keep` annotations or explicit rules for any incorrectly removed class. Confirmed during smoke testing: Settings sub-screens crashed until keep rules were added for all six settings fragments and `LargePreferenceCategory`.
- **[Risk] Detekt flags new annotations or changed constructs** → Mitigation: Run `./gradlew detekt` locally after every change to the keep rules file and before pushing.

## Migration Plan

1. Add `minifyEnabled true` and `proguardFiles` to the `release` build type in `owncloudApp/build.gradle`, and immediately create a minimal stub `owncloudApp/proguard-rules.pro` (header comment only) so the build configuration is valid before any keep rules are written.
2. Populate `owncloudApp/proguard-rules.pro` with keep rules per D3.
3. Run `./gradlew assembleOriginalRelease` and resolve any build errors.
4. Inspect `configuration.txt` (D4) and iterate on `proguard-rules.pro` as needed.
5. Install the release APK on a device/emulator and smoke-test (login, file listing, upload, download, background sync).
6. Run `./gradlew testDebugUnitTest testMdmDebugUnitTest` to confirm unit tests pass.
7. Run `./gradlew detekt` to confirm zero issues.

**Rollback:** Revert `owncloudApp/build.gradle` to remove the `minifyEnabled` and `proguardFiles` lines. Delete `owncloudApp/proguard-rules.pro`. No database migrations or server-side changes are involved.
