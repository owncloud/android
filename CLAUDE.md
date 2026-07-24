# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease          # requires signing env vars

# Unit tests (all flavors)
./gradlew testDebugUnitTest testMdmDebugUnitTest --continue

# Single module unit tests
./gradlew :owncloudApp:testOriginalDebugUnitTest
./gradlew :owncloudData:testDebugUnitTest

# Instrumented tests (requires running emulator/device)
./gradlew :owncloudData:connectedAndroidTest

# Lint & static analysis
./gradlew detekt                   # maxIssues: 0 — any issue fails CI
./gradlew lintDebug
./gradlew ktlintCheck
./gradlew ktlintFormat             # auto-fix formatting
```

## Architecture

Four modules in a strict dependency chain:

```
owncloudComLibrary → owncloudData → owncloudDomain ← owncloudApp
```

- **owncloudComLibrary** — networking layer: OkHttp, DAV4Android (WebDAV), OAuth/OIDC, Moshi
- **owncloudDomain** — business layer: UseCases, domain Models, Repository interfaces, Exceptions
- **owncloudData** — data layer: Repository implementations, Room DB, Remote/Local DataSources
- **owncloudApp** — presentation layer: Activities, Fragments, ViewModels, Koin wiring
- **owncloudTestUtil** — shared test factories, mocks, and extensions (test-only dependency)

**Pattern:** MVVM + Clean Architecture. Data flows strictly in one direction:

```
Fragment → ViewModel → UseCase → Repository → DataSource → ownCloud server (WebDAV/Graph API)
```

UseCases extend `BaseUseCase<R>` or `BaseUseCaseWithResult<P, R>`. ViewModels call them via coroutines and expose `Flow` (preferred) or `LiveData` to the UI.

**DI:** Koin with module-per-layer structure under `owncloudApp/.../dependecyinjection/`:
`CommonModule`, `RemoteDataSourceModule`, `LocalDataSourceModule`, `RepositoryModule`, `UseCaseModule`, `ViewModelModule`.

## Product Flavors

Dimension: `management`

| Flavor | Purpose |
|--------|---------|
| `original` | Standard ownCloud app |
| `mdm` | Mobile Device Management — custom branding/policies |
| `qa` | QA variant for automated testing |

Build types: `debug` / `release`. Combined targets follow the pattern `assembleOriginalDebug`, `testMdmDebugUnitTest`, etc.

## SDK & Toolchain

- Min SDK 28 (Android 9), Target/Compile SDK 36
- Kotlin 2.3.10, JDK 17, AGP 9.2.1
- Version catalog: `gradle/libs.versions.toml`

## Git & Commit Conventions

All commits must be **GPG-signed** and include a **DCO sign-off**:

```bash
git commit -s -S -m "type: description"
```

**Conventional Commits** format is enforced by CI:
- `feat:` introduces a new capability visible to the user or other modules (new screen, new API, new setting)
- `fix:` corrects a defect — something that was broken and is now working as intended
- `refactor:` restructuring existing code (rename, extract, move, simplify) with no change to observable behavior
- `test:` adds or updates tests only — no production code changes
- `build:` changes to dependencies (adding, removing, or upgrading libraries in `libs.versions.toml` or any `build.gradle`)
- `chore:` maintenance that has no user-facing effect (changelog entries, build config, tooling) — do not use for dependency changes

Branch naming follows OneFlow: `feature/feature_name`, `fix/fix_name`, `improvement/improvement_name`, `technical/technical_description`.

Rebase on the target branch before opening a PR. No merge commits.

## Changelog

Every PR must include a file in `changelog/unreleased/` named after the PR number (e.g., `4936`):

```
Enhancement: Short title under 80 chars

Description in present perfect passive tense.

https://github.com/owncloud/android/issues/<issue>  ← optional, omit if there is no linked issue
https://github.com/owncloud/android/pull/<pr>
```

Types: `Bugfix`, `Change`, `Enhancement`, `Security`. Calens reads these files to auto-generate `CHANGELOG.md` after merge.

## Key Constraints

- **No translation PRs** — submit translations to Transifex only.
- **Detekt is zero-tolerance** (`maxIssues: 0`). Run `./gradlew detekt` locally before pushing.
- **Max line length: 150** (enforced by `.editorconfig`).
- **GitHub Actions** must use SHA-pinned refs and come from the owncloud org, `actions/*`, or verified Marketplace publishers.
- **License migration in progress** (GPL-2.0 → Apache 2.0): do not introduce new copyleft dependencies without discussion.
- **Security issues** go to security.owncloud.com, not GitHub issues.