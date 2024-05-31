---
name: Release
about: List of checklist to accomplish for the ownCloud team to finish the release process
title: "[RELEASE]"
labels: Release
assignees: ''
---

<!--
Another release for the ownCloud Android client!
For Open releases, keep the Open Release template and remove the OEM and Patch Release ones
For Patch releases, keep the Patch Release template and remove the Open and OEM Release ones
For OEM releases, keep the OEM Release template and remove the Open and Patch Release ones
-->

## Open Release

### TASKS:

 - [ ] [DOC] Ping in #documentation-internal about the new release
 - [ ] [GIT] Create branch `release/M.m.p` in owncloud/android from master
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DEV] Update [SBOM](https://cloud.owncloud.com/f/6072870)
 - [ ] [DIS] Create a folder for the new version like `M.m.p_YYYY-MM-DD` inside the `changelog` folder
 - [ ] [DIS] Move all changelog files from the `unreleased` folder to the new version folder
 - [ ] [DEV] Check release notes in `ReleaseNotesViewModel.kt` to assure nothing important is missing there
 - [ ] [DIS] Prepare post in central.owncloud.org ([`Category:News + Tag:android`](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [DIS] Check for new screenshots in Play Store / GitHub repo and generate them
 - [ ] [DIS] Generate final bundle from last commit in owncloud/android
 - [ ] [QA] Design Test plan
 - [ ] [DEV] Code Review
 - [ ] [QA] Regression Test execution
 - [ ] [QA] QA Approval
 - [ ] [DIS] Upload release APK and bundle to internal owncloud instance
 - [ ] [DOC] Ping in #documentation-internal that we are close to sign the new tags
 - [ ] [DIS] Upload & publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots and check they are OK in Play Store and in `README.md` (if needed)
 - [ ] [GIT] Create and sign tag `vM.m.p` in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Move tag `latest` pointing the same commit as the release commit
 - [ ] [GIT] Publish a new [release](https://github.com/owncloud/android/releases) in owncloud/android
 - [ ] [DIS] Release published in Play Store
 - [ ] [DIS] Publish post in central.owncloud.org ([`Category:News + Tag:android`](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [COM] Inform `#updates` and `#marketing` in internal chat that release is out
 - [ ] [GIT] Merge without rebasing `release/M.m.p` branch into `master`, in owncloud/android
 - [ ] [DOC] Update documentation with new stuff by creating [issue](https://github.com/owncloud/docs-client-android/issues)


### QA

Regression test:

Bugs & improvements:

- [ ] (1) ...

_____

## Patch Release

### TASKS:

 - [ ] [GIT] Create branch `release/M.m.p` in owncloud/android from `latest`
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DEV] Update [SBOM](https://cloud.owncloud.com/f/6072870)
 - [ ] [DIS] Create a folder for the new version like `M.m.p_YYYY-MM-DD` inside the `changelog` folder with the proper fixes
 - [ ] [DIS] Copy the `unreleased` folder in `master` branch to `changelog` folder in this branch, to avoid Calens' conflicts problems
 - [ ] [DEV] Add release notes to `ReleaseNotesViewModel.kt` (in case patch release before OEM)
 - [ ] [DIS] Check for new screenshots in Play Store / GitHub repo and generate them
 - [ ] [DIS] Generate final bundle from last commit in owncloud/android
 - [ ] [DEV] Code Review
 - [ ] [QA] Test execution
 - [ ] [QA] Trigger BitRise builds for unit tests and UI tests, in case CHANGELOG conflicts avoid them in GitHub
 - [ ] [QA] QA Approval
 - [ ] [DIS] Upload release APK and bundle to internal owncloud instance
 - [ ] [DIS] Upload & publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots and check they are OK in Play Store and in `README.md` (if needed)
 - [ ] [GIT] Create and sign tag `vM.m.p` in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Move tag `latest` pointing the same commit as the release commit
 - [ ] [GIT] Publish a new [release](https://github.com/owncloud/android/releases) in owncloud/android
 - [ ] [DIS] Release published in Play Store
 - [ ] [COM] Inform `#updates` in internal chat that release is out
 - [ ] [GIT] Fix conflicts if they happen. GitHub option will help and will merge `master` into release branch, getting it ready to fast forward
 - [ ] [GIT] Merge without rebasing `release/M.m.p` branch into `master`, in owncloud/android



### QA

QA checks:

- [ ] Smoke test
- [ ] Upgrade test

Bugs & improvements:

- [ ] (1) ...


_____

## OEM Release

### TASKS:

- [ ] [GIT] Create a new branch `release/M.m.p_oem` (optional)
- [ ] [DIS] Update release notes in app with the proper content for oem release
- [ ] [GIT] Create and sign tag `vM.m.p_oem` in HEAD commit of `release/M.m.p_oem` branch
- [ ] [DEV] Approve and merge changes in ownBrander
  - [ ] Feature 1 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Feature 2 oB https://github.com/owncloud/ownbrander/pull/
  - [ ] Update version number in ownBrander
- [ ] [OPS] Block oB button
- [ ] [OPS] Deploy oB
- [ ] [QA] Generate final APKs files from signed commit in builder machine and perform some basic checks:
    - [ ] Installation of apk/aab generated by builder machine
    - [ ] Check Feature 1 oB
    - [ ] Check Feature 2 oB
    - [ ] App update from previous version (generated in advance)
- [ ] [QA] Notify result in #ownbrander
- [ ] [OPS] Enable button
- [ ] [GIT] Fix conflicts in branch if they happen. GitHub option will help and will merge `master` into release branch, getting it ready to fast forward
- [ ] [GIT] Merge without rebasing `release/M.m.p_oem` branch into `master`, in owncloud/android
