# AI Agent Guidelines for Android

This file provides context for AI coding agents (Claude Code, GitHub Copilot, Cursor, etc.) working in this repository.

## Repository Overview
- **Product family:** Mobile (Android)
- **Primary language(s):** Kotlin
- **Build system:** Gradle
- **Test framework:** JUnit (Android Unit Tests, Instrumented Tests)
- **CI system:** GitHub Actions

## Architecture & Key Paths
- `owncloudApp/` - Main Android application module
- `owncloudDomain/` - Domain layer (business logic, use cases)
- `owncloudData/` - Data layer (repositories, data sources)
- `owncloudComLibrary/` - Common library module
- `owncloudTestUtil/` - Test utilities
- `build.gradle` - Root Gradle build file
- `gradle/` - Gradle wrapper
- `config/` - Build configuration
- `fastlane/` - Fastlane deployment configuration
- `doc/` - Developer documentation
- `SETUP.md` - Development environment setup
- `CONTRIBUTING.md` - Contribution guidelines
- `CHANGELOG.md` - Release history

## Development Conventions
- **Branching:** master
- **Commit messages:** DCO sign-off required (`git commit -s`)
- **Code style:** Detekt (Kotlin linter), EditorConfig
- **PR process:** Open a PR against master. All CI checks must pass.

## Build & Test Commands
```bash
# Build
./gradlew assembleDebug

# Test (unit)
./gradlew testDebugUnitTest

# Test (instrumented)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew detekt
```

## Important Constraints
- All code contributions must be compatible with the **GPL-2.0** license
- Do not introduce new **copyleft-licensed dependencies** (GPL, AGPL, LGPL, MPL) without explicit discussion in an issue first. This is especially important for repos migrating to Apache 2.0.
- Do not introduce new dependencies without discussion in an issue first
- Minimum SDK version is 24 (Android 7.0), target SDK is 35
- The app uses a clean architecture pattern with domain/data/presentation layers


## OSPO Policy Constraints

### GitHub Actions
- **Only** use actions owned by `owncloud`, created by GitHub (`actions/*`), verified on the GitHub Marketplace, or verified by the ownCloud Maintainers.
- Pin all actions to their full commit SHA (not tags): `uses: actions/checkout@<SHA> # vX.Y.Z`
- Never introduce actions from unverified third parties.

### Dependency Management
- Dependabot is configured for automated dependency updates.
- Review and merge Dependabot PRs as part of regular maintenance.
- Do not introduce new dependencies without discussion in an issue first.

### Git Workflow
- **Rebase policy**: Always rebase; never create merge commits. Use `git pull --rebase` and `git rebase` before pushing.
- **Signed commits**: All commits **must** be PGP/GPG signed (`git commit -S -s`).
- **DCO sign-off**: Every commit needs a `Signed-off-by` line (`git commit -s`).
- **Conventional Commits & Squash Merge**: Use the [Conventional Commits](https://www.conventionalcommits.org/) format where the repository enforces it. Many repos use squash merge, where the PR title becomes the commit message on the default branch — apply Conventional Commits format to PR titles as well. A reusable GitHub Actions workflow enforces this.

## Context for AI Agents
- Match existing code style
- Do not refactor unrelated code in the same PR
- Write tests for new functionality
- Keep PRs focused and atomic
- Follow the existing multi-module architecture (app/domain/data separation)
- Use Kotlin idioms consistent with the existing codebase
