# AI Agent Guidelines for Android

This file provides context for AI coding agents (Claude Code, GitHub Copilot, Cursor, etc.) working in this repository.

## Repository Overview
- **Product family:** Mobile (Android)
- **Primary language(s):** Kotlin
- **Build system:** Gradle
- **Test framework:** JUnit (Android Unit Tests, Instrumented Tests)
- **CI system:** GitHub Actions

## Architecture & Key Paths

Four modules in a strict dependency chain:

```
owncloudComLibrary → owncloudData → owncloudDomain ← owncloudApp
```

- `owncloudApp/` — presentation layer: Activities, Fragments, ViewModels, Koin wiring
- `owncloudDomain/` — business layer: UseCases, domain Models, Repository interfaces, Exceptions
- `owncloudData/` — data layer: Repository implementations, Room DB, Remote/Local DataSources
- `owncloudComLibrary/` — networking layer: OkHttp, DAV4Android (WebDAV), OAuth/OIDC, Moshi
- `owncloudTestUtil/` — shared test factories, mocks, and extensions (test-only dependency)
- `build.gradle` — Root Gradle build file
- `gradle/` — Gradle wrapper and version catalog (`gradle/libs.versions.toml`)
- `config/` — Build configuration
- `fastlane/` — Fastlane deployment configuration
- `doc/` — Developer documentation
- `SETUP.md` — Development environment setup
- `CONTRIBUTING.md` — Contribution guidelines
- `CHANGELOG.md` — Release history

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

## Development Conventions

- **Branching (OneFlow):** `feature/feature_name`, `fix/fix_name`, `improvement/improvement_name`, `technical/technical_description`, `chore/chore_description`
- **Commit messages:** Conventional Commits format enforced by CI (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `chore:`)
  - `feat:` introduces a new capability visible to the user or other modules (new screen, new API, new setting)
  - `fix:` corrects a defect — something that was broken and is now working as intended
  - `refactor:` restructuring existing code (rename, extract, move, simplify) with no change to observable behavior
  - `test:` adds or updates tests only — no production code changes
  - `build:` changes to dependencies (adding, removing, or upgrading libraries in `libs.versions.toml` or any `build.gradle`)
  - `chore:` maintenance that has no user-facing effect (changelog entries, build config, tooling) — do not use for dependency changes
- **Signed commits:** All commits **must** be GPG-signed with DCO sign-off: `git commit -s -S -m "type: description"`
- **Rebase policy:** Always rebase; never create merge commits. Use `git pull --rebase` before pushing.
- **Code style:** Detekt (zero-tolerance, `maxIssues: 0`), ktlint, EditorConfig (max line length: 150)
- **PR process:** Rebase on the target branch before opening a PR. All CI checks must pass.

## Changelog

Every PR must include a file in `changelog/unreleased/` named after the PR number (e.g., `4936`):

```
Enhancement: Short title under 80 chars

Description in present perfect passive tense.

https://github.com/owncloud/android/issues/<issue>  ← optional, omit if there is no linked issue
https://github.com/owncloud/android/pull/<pr>
```

Types: `Bugfix`, `Change`, `Enhancement`, `Security`. Calens reads these files to auto-generate `CHANGELOG.md` after merge.

## Important Constraints

- **No translation PRs** — submit translations to Transifex only.
- **Detekt is zero-tolerance** (`maxIssues: 0`). Run `./gradlew detekt` locally before pushing.
- **License migration in progress** (GPL-2.0 → Apache 2.0): do not introduce new copyleft dependencies without discussion.
- **Security issues** go to security.owncloud.com, not GitHub issues.
- Do not introduce new dependencies without discussion in an issue first.

## OSPO Policy Constraints

### GitHub Actions
- **Only** use actions owned by `owncloud`, created by GitHub (`actions/*`), verified on the GitHub Marketplace, or verified by the ownCloud Maintainers.
- Pin all actions to their full commit SHA (not tags): `uses: actions/checkout@<SHA> # vX.Y.Z`
- Never introduce actions from unverified third parties.

### Dependency Management
- Dependabot is configured for automated dependency updates.
- Review and merge Dependabot PRs as part of regular maintenance.
- Do not introduce new dependencies without discussion in an issue first.

## Context for AI Agents
- Match existing code style
- Do not refactor unrelated code in the same PR
- Write tests for new functionality
- Keep PRs focused and atomic
- Follow the existing multi-module architecture (app/domain/data separation)
- Use Kotlin idioms consistent with the existing codebase
