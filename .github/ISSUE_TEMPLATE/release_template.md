---
name: Release
about: List of checklist to accomplish for the ownCloud team to finish the release process
title: "[RELEASE]"
labels: Release
assignees: ''

---

### TASKS:

 - [ ] [GIT] Create branch release/Y.Y.Y in owncloud/android-library from master
 - [ ] [GIT] Create branch release/X.X.X in owncloud/android from master
 - [ ] [DEV] Update version number and name in build.gradle in owncloudApp module
 - [ ] [DIS] Create a folder for the new version like $majorVersion.$minorVersion.$patchVersion_YYYY-MM-DD
 - [ ] [DIS] Move all changelog files from the unreleased folder to the new version folder
 - [ ] [DIS] Update screenshots, if needed, in README.md
 - [ ] [DIS] Add ReleaseNotes replacing `emptyList` with `listOf` and adding inside `ReleaseNote()` with String resources
 - [ ] [QA] Design Test plan
 - [ ] [QA] Regression Test plan
 - [ ] [DIS] Generate test bundle from `release/X.X.X` branch in owncloud/android
 - [ ] [GIT] Create and sign tag 'oc-android-X.X.X' in HEAD commit of release branch, in owncloud/android
 - [ ] [GIT] Create and sign tag 'Y.Y.Y' in HEAD commit of release branch, in owncloud/android-library
 - [ ] [DIS] Generate final bundle from signed commit in owncloud/android
 - [ ] [GIT] Merge branch `release/Y.Y.Y` in owncloud/android-library, into master
 - [ ] [GIT] Merge branch `release/X.X.X` in owncloud/android, into master
 - [ ] [DIS] Upload & publish release bundle and changelog in Play Store
 - [ ] [DIS] Update screenshots and store listing, if needed, in Play Store
 - [ ] [GIT] Publish a new release in owncloud/android
 - [ ] [DIS] Create post in central.owncloud.org ([`Category:Android + Tag:news`](https://central.owncloud.org/tags/c/news/5/android))
 - [ ] [COM] Inform `#updates` and `#marketing` in internal chat
 - [ ] [DIS] Upload release APK and bundle to internal owncloud instance
 - [ ] [GIT] Merge master branch into stable, in owncloud/android-library
 - [ ] [GIT] Merge master branch into stable, in owncloud/android
 - [ ] [DOC] Update documentation with new stuff (notify rocketchat #documentation-internal in advance!)


_____

### BUGS & IMPROVEMENTS
