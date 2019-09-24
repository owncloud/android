---
name: Release
about: Create a bug report to help us improve
title: "[RELEASE]"
labels: Release
assignees: ''

---

### TASKS:

 - [ ] [GIT] Create branch release_Y.Y.Y in owncloud/android-library from master
 - [ ] [GIT] Create branch release_X.X.X in owncloud/android from master
 - [ ] [DEV] Update version number and name in AndroidManifest.xml in android module
 - [ ] [DIS] Create changelog file (< 500 chars) and add to CHANGELOG.md in owncloud/android
 - [ ] [QA] Design Test plan
 - [ ] [QA] Regression Test plan
 - [ ] [DIS] Generate test APK file from `release_X.X.X` branch in owncloud/android
 - [ ] [GIT] Create and sign tag 'oc-android-X.X.X' in HEAD commit of stable master branch, in owncloud/android
 - [ ] [GIT] Create and sign tag 'Y.Y.Y' in HEAD commit of stable master branch, in owncloud/android-library
 - [ ] [DIS] Generate final APKs from signed commit in owncloud/android
 - [ ] [mail] inform john@owncloud.com and emil@owncloud.com about new release.
 - [ ] [GIT] Merge branch `release_Y.Y.Y` in owncloud/android-library, into master
 - [ ] [GIT] Merge branch `release_X.X.X` in owncloud/android, into master
 - [ ] [DIS] Upload & publish release APK and changelog in Play Store
 - [ ] [DIS] Update screenshots and store listing, if needed, in Play Store
 - [ ] [GIT] merge master branch into stable, in owncloud/android-library
 - [ ] [GIT] merge master branch into stable, in owncloud/android
 - [ ] [DOC] Update owncloud.org/download version numbers (notify rocketchat #marketing)


_____

### BUGS & IMPROVEMENTS
