---
name: Release
about: List of checklist to accomplish for the ownCloud team to finish the release process
title: "[RELEASE]"
labels: Release
assignees: ''
---

<!--
Another release for the ownCloud Android client!
For Open releases, keep the Open release template and remove the Patch and Enterprise release ones
For Patch releases, keep the Patch release template and remove the Open and Enterprise release ones
For Enterprise releases, keep the Enterprise release template and remove the Open and Patch release ones
If you don't need some of the steps, cross them by removing the "[ ]" and surrounding the line by "~~ ~~", like "- ~~Non-applicable step~~"
-->

## Open release

### TASKS:

 - [ ] [COM] Ping in #android-dev-internal about the new release (@mmattel)
 - [ ] [GIT] Create branch `release/M.m.p` in owncloud/android from `master`
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DOC] Update [SBOM](https://infinite.owncloud.com/f/31e6d44f-f373-557c-9ab3-1748fc0c650d$4994cd9c-1c17-4254-829a-f5ef6e1ff7e3%215080be84-fbcc-4aca-956e-b278a7090418)
 - [ ] [DIS] Move Calens files from `unreleased` to a new folder like `M.m.p_YYYY-MM-DD` inside the `changelog` folder
 - [ ] [DEV] Check and reorder release notes in `ReleaseNotesViewModel.kt` to assure nothing important is missing there
 - [ ] [DEV] Code review
 - [ ] [DIS] Generate final bundle and APK from last commit in release branch
 - [ ] [COM] Prepare post in central.owncloud.org ([Category:News + Tag:android](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [DIS] Check for new screenshots in Play Store/GitHub/F-Droid and generate them
 - [ ] [QA] Design test plan
 - [ ] [QA] Regression test execution
 - [ ] [QA] QA approval
 - [ ] [DIS] Upload release APK and bundle to internal ownCloud instance
 - [ ] [COM] Ping in #android-dev-internal that we are close to sign the new tags (@mmattel)
 - [ ] [DIS] Upload and publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots in Play Store/GitHub/F-Droid
 - [ ] [GIT] Create and sign tag `vM.m.p` in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Move tag `latest` pointing the same commit as the release commit
 - [ ] [DIS] Publish a new [release](https://github.com/owncloud/android/releases) in owncloud/android
 - [ ] [DIS] Release published in Play Store
 - [ ] [COM] Publish post in central.owncloud.org ([Category:News + Tag:android](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [COM] Inform in #general that release is out
 - [ ] [GIT] Merge without rebasing `release/M.m.p` branch into `master`, in owncloud/android
 - [ ] [COM] Ping @TheOneRing to update release information in https://owncloud.com/mobile-apps/
 - [ ] [DOC] Update documentation with new stuff by creating [issue](https://github.com/owncloud/docs-client-android/issues)


### QA

Regression test:

Bugs & improvements:

- [ ] (1) ...

_____

## Patch release

### TASKS:

 - [ ] [GIT] Create branch `release/M.m.p` in owncloud/android from `latest`
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DOC] Update [SBOM](https://infinite.owncloud.com/f/31e6d44f-f373-557c-9ab3-1748fc0c650d$4994cd9c-1c17-4254-829a-f5ef6e1ff7e3%215080be84-fbcc-4aca-956e-b278a7090418)
 - [ ] [DIS] Update release notes in app and changelog in `unreleased` with the proper content for the release
 - [ ] [DIS] Move Calens files from `unreleased` to a new folder like `M.m.p_YYYY-MM-DD` inside the `changelog` folder
 - [ ] [DIS] Copy the `unreleased` folder in `master` branch into this branch, to avoid Calens conflicts problems
 - [ ] [DEV] Check and reorder release notes in `ReleaseNotesViewModel.kt` to assure nothing important is missing there
 - [ ] [DEV] Code review
 - [ ] [DIS] Generate final bundle and APK from last commit in the release branch
 - [ ] [DIS] Check for new screenshots in Play Store/GitHub/F-Droid and generate them
 - [ ] [QA] Design test plan
 - [ ] [QA] Test execution
 - [ ] [QA] Trigger BitRise builds for unit tests and UI tests, in case changelog conflicts avoid them in GitHub
 - [ ] [QA] QA approval
 - [ ] [DIS] Upload release APK and bundle to internal ownCloud instance
 - [ ] [DIS] Upload and publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots in Play Store/GitHub/F-Droid
 - [ ] [GIT] Create and sign tag `vM.m.p` in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Move tag `latest` pointing the same commit as the release commit
 - [ ] [DIS] Publish a new [release](https://github.com/owncloud/android/releases) in owncloud/android
 - [ ] [DIS] Release published in Play Store
 - [ ] [COM] Inform in #general that release is out
 - [ ] [GIT] Merge `master` into `release/M.m.p`, fixing all the conflicts that could happen, in owncloud/android
 - [ ] [GIT] Merge without rebasing `release/M.m.p` branch into `master`, in owncloud/android
 - [ ] [COM] Ping @TheOneRing to update release information in https://owncloud.com/mobile-apps/


### QA

QA checks:

- [ ] Smoke test
- [ ] Upgrade test

Bugs & improvements:

- [ ] (1) ...


_____

## Enterprise release

### TASKS:

- [ ] [GIT] Create branch `release/M.m.p_enterprise` in owncloud/android from `latest` (or the corresponding release tag)
- [ ] [DOC] Update [SBOM](https://infinite.owncloud.com/f/31e6d44f-f373-557c-9ab3-1748fc0c650d$4994cd9c-1c17-4254-829a-f5ef6e1ff7e3%215080be84-fbcc-4aca-956e-b278a7090418)
- [ ] [DIS] Update release notes in app and changelog in `M.m.p_YYYY-MM-DD` (already released version) with the proper content for the release
- [ ] [DIS] Copy the `unreleased` folder in `master` branch into this branch, to avoid Calens conflicts problems
- [ ] [DEV] Check and reorder release notes in `ReleaseNotesViewModel.kt` to assure nothing important is missing there
- [ ] [DEV] Code review
- [ ] [DIS] Generate final bundle and APK from last commit in the release branch
- [ ] [QA] Design test plan
- [ ] [QA] Test execution
- [ ] [QA] Trigger BitRise builds for unit tests and UI tests, in case changelog conflicts avoid them in GitHub
- [ ] [QA] QA approval
- [ ] [DIS] Upload release APK and bundle to internal ownCloud instance
- [ ] [GIT] Create and sign tag `vM.m.p_enterprise` in HEAD commit of release branch, in owncloud/android
- [ ] [DEV] Approve and merge changes in ownBrander
  - [ ] Feature 1 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Feature 2 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Update version number in ownBrander
- [ ] [COM] Ping @NannaBarz to block oB button
- [ ] [COM] Ping @NannaBarz to deploy oB
- [ ] [QA] Generate final APKs from signed commit in builder machine and perform some basic checks
    - [ ] Installation of APK/bundle generated by builder machine
    - [ ] Check Feature 1 oB
    - [ ] Check Feature 2 oB
    - [ ] App update from previous version (generated in advance)
- [ ] [COM] Notify result in internal chat
- [ ] [COM] Ping @NannaBarz to enable oB button
- [ ] [GIT] Merge `master` into `release/M.m.p_enterprise`, fixing all the conflicts that could happen, in owncloud/android
- [ ] [GIT] Merge without rebasing `release/M.m.p_enterprise` branch into `master`, in owncloud/android
