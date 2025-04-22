# Table of Contents

* [Changelog for unreleased](#changelog-for-owncloud-android-client-unreleased-unreleased)
* [Changelog for 4.5.1](#changelog-for-owncloud-android-client-451-2025-04-03)
* [Changelog for 4.5.0](#changelog-for-owncloud-android-client-450-2025-03-24)
* [Changelog for 4.4.1](#changelog-for-owncloud-android-client-441-2024-10-30)
* [Changelog for 4.4.0](#changelog-for-owncloud-android-client-440-2024-09-30)
* [Changelog for 4.3.1](#changelog-for-owncloud-android-client-431-2024-07-22)
* [Changelog for 4.3.0](#changelog-for-owncloud-android-client-430-2024-07-01)
* [Changelog for 4.2.2](#changelog-for-owncloud-android-client-422-2024-05-30)
* [Changelog for 4.2.1](#changelog-for-owncloud-android-client-421-2024-02-22)
* [Changelog for 4.2.0](#changelog-for-owncloud-android-client-420-2024-02-12)
* [Changelog for 4.1.1](#changelog-for-owncloud-android-client-411-2023-10-18)
* [Changelog for 4.1.0](#changelog-for-owncloud-android-client-410-2023-08-23)
* [Changelog for 4.0.0](#changelog-for-owncloud-android-client-400-2023-05-29)
* [Changelog for 3.0.4](#changelog-for-owncloud-android-client-304-2023-03-07)
* [Changelog for 3.0.3](#changelog-for-owncloud-android-client-303-2023-02-13)
* [Changelog for 3.0.2](#changelog-for-owncloud-android-client-302-2023-01-26)
* [Changelog for 3.0.1](#changelog-for-owncloud-android-client-301-2022-12-21)
* [Changelog for 3.0.0](#changelog-for-owncloud-android-client-300-2022-12-12)
* [Changelog for 2.21.2](#changelog-for-owncloud-android-client-2212-2022-09-07)
* [Changelog for 2.21.1](#changelog-for-owncloud-android-client-2211-2022-06-15)
* [Changelog for 2.21.0](#changelog-for-owncloud-android-client-2210-2022-06-07)
* [Changelog for 2.20.0](#changelog-for-owncloud-android-client-2200-2022-02-16)
* [Changelog for 2.19.0](#changelog-for-owncloud-android-client-2190-2021-11-15)
* [Changelog for 2.18.3](#changelog-for-owncloud-android-client-2183-2021-10-27)
* [Changelog for 2.18.1](#changelog-for-owncloud-android-client-2181-2021-07-20)
* [Changelog for 2.18.0](#changelog-for-owncloud-android-client-2180-2021-05-24)
* [Changelog for 2.17 versions and below](#changelog-for-217-versions-and-below)
# Changelog for ownCloud Android Client [unreleased] (UNRELEASED)

The following sections list the changes in ownCloud Android Client unreleased relevant to
ownCloud admins and users.

[unreleased]: https://github.com/owncloud/android/compare/v4.5.1...master

## Summary

* Bugfix - Content in Spaces not shown from third-party apps: [#4522](https://github.com/owncloud/android/issues/4522)
* Bugfix - Add bottom margin for used quota in account dialog: [#4566](https://github.com/owncloud/android/issues/4566)
* Change - Bump target SDK to 35: [#4529](https://github.com/owncloud/android/issues/4529)
* Change - Replace dav4android location: [#4536](https://github.com/owncloud/android/issues/4536)
* Change - Modify biometrics fail source string: [#4572](https://github.com/owncloud/android/issues/4572)
* Enhancement - QA variant: [#3791](https://github.com/owncloud/android/issues/3791)
* Enhancement - Accessibility reports in 4.5.1: [#4568](https://github.com/owncloud/android/issues/4568)

## Details

* Bugfix - Content in Spaces not shown from third-party apps: [#4522](https://github.com/owncloud/android/issues/4522)

   The root of the spaces has been synchronized before displaying the file list
   when a file is shared from a third-party app.

   https://github.com/owncloud/android/issues/4522
   https://github.com/owncloud/android/pull/4574

* Bugfix - Add bottom margin for used quota in account dialog: [#4566](https://github.com/owncloud/android/issues/4566)

   Added bottom margin to the container holding used quota view when multi account
   is disabled

   https://github.com/owncloud/android/issues/4566
   https://github.com/owncloud/android/pull/4567

* Change - Bump target SDK to 35: [#4529](https://github.com/owncloud/android/issues/4529)

   Target SDK has been upgraded to 35 in order to fulfill Android platform
   requirements.

   https://github.com/owncloud/android/issues/4529
   https://github.com/owncloud/android/pull/4556

* Change - Replace dav4android location: [#4536](https://github.com/owncloud/android/issues/4536)

   Dav4android location has been moved from GitLab to GitHub.

   https://github.com/owncloud/android/issues/4536
   https://github.com/owncloud/android/pull/4558

* Change - Modify biometrics fail source string: [#4572](https://github.com/owncloud/android/issues/4572)

   The string that appears when biometric unlocking is not available has been
   changed in order to make it clearer.

   https://github.com/owncloud/android/issues/4572
   https://github.com/owncloud/android/pull/4578

* Enhancement - QA variant: [#3791](https://github.com/owncloud/android/issues/3791)

   A new flavor for QA has been created in order to make automatic tests easier.

   https://github.com/owncloud/android/issues/3791
   https://github.com/owncloud/android/pull/4569

* Enhancement - Accessibility reports in 4.5.1: [#4568](https://github.com/owncloud/android/issues/4568)

   Some content descriptions that were missing have been added to provide a better
   accessibility experience.

   https://github.com/owncloud/android/issues/4568
   https://github.com/owncloud/android/pull/4573

# Changelog for ownCloud Android Client [4.5.1] (2025-04-03)

The following sections list the changes in ownCloud Android Client 4.5.1 relevant to
ownCloud admins and users.

[4.5.1]: https://github.com/owncloud/android/compare/v4.5.0...v4.5.1

## Summary

* Bugfix - Confusing behaviour when creating new files using apps provider: [#4560](https://github.com/owncloud/android/issues/4560)
* Bugfix - App crashes at start when biometrics fail: [#7134](https://github.com/owncloud/enterprise/issues/7134)

## Details

* Bugfix - Confusing behaviour when creating new files using apps provider: [#4560](https://github.com/owncloud/android/issues/4560)

   The error that appeared when creating a new file using the apps provider has
   been fixed. Now, the custom tab is opened correctly with the file content.

   https://github.com/owncloud/android/issues/4560
   https://github.com/owncloud/android/pull/4562

* Bugfix - App crashes at start when biometrics fail: [#7134](https://github.com/owncloud/enterprise/issues/7134)

   The crash that happened when biometrics failed due to a system error has been
   handled. In this case, an error is shown and pattern or passcode unlock are used
   instead of biometrics.

   https://github.com/owncloud/enterprise/issues/7134
   https://github.com/owncloud/android/pull/4564

# Changelog for ownCloud Android Client [4.5.0] (2025-03-24)

The following sections list the changes in ownCloud Android Client 4.5.0 relevant to
ownCloud admins and users.

[4.5.0]: https://github.com/owncloud/android/compare/v4.4.1...v4.5.0

## Summary

* Bugfix - Crash from Google Play Store: [#4333](https://github.com/owncloud/android/issues/4333)
* Bugfix - Navigation in automatic uploads folder picker: [#4340](https://github.com/owncloud/android/issues/4340)
* Bugfix - Downloading non-previewable files in details view leads to empty list: [#4428](https://github.com/owncloud/android/issues/4428)
* Bugfix - Ensure folder size updates automatically after file replacement: [#4505](https://github.com/owncloud/android/issues/4505)
* Change - Replace auto-uploads with automatic uploads: [#4252](https://github.com/owncloud/android/issues/4252)
* Change - Removed survey and chat from feedback: [#4540](https://github.com/owncloud/android/issues/4540)
* Enhancement - Unit tests for repository classes - Part 2: [#4233](https://github.com/owncloud/android/issues/4233)
* Enhancement - Unit tests for repository classes - Part 3: [#4234](https://github.com/owncloud/android/issues/4234)
* Enhancement - Unit tests for repository classes - Part 4: [#4235](https://github.com/owncloud/android/issues/4235)
* Enhancement - Add status message when (un)setting av. offline from preview: [#4382](https://github.com/owncloud/android/issues/4382)
* Enhancement - Quota improvements from GraphAPI: [#4411](https://github.com/owncloud/android/issues/4411)
* Enhancement - Upgraded AGP version to 8.7.2: [#4478](https://github.com/owncloud/android/issues/4478)
* Enhancement - Added text labels for BottomNavigationView: [#4484](https://github.com/owncloud/android/issues/4484)
* Enhancement - OCIS Light Users: [#4490](https://github.com/owncloud/android/issues/4490)
* Enhancement - Enforce OIDC auth flow via branding: [#4500](https://github.com/owncloud/android/issues/4500)
* Enhancement - Detekt: static code analyzer: [#4506](https://github.com/owncloud/android/issues/4506)
* Enhancement - Multi-Personal (1st round): [#4514](https://github.com/owncloud/android/issues/4514)
* Enhancement - Technical improvements for user quota: [#4521](https://github.com/owncloud/android/issues/4521)

## Details

* Bugfix - Crash from Google Play Store: [#4333](https://github.com/owncloud/android/issues/4333)

   The androidx-appcompat version has been upgraded from 1.5.1 to 1.6.1 in order to
   fix one crash reported by Play Console which is related to the
   FileDataStorageManager constructor

   https://github.com/owncloud/android/issues/4333
   https://github.com/owncloud/android/pull/4542

* Bugfix - Navigation in automatic uploads folder picker: [#4340](https://github.com/owncloud/android/issues/4340)

   The button in the toolbar for going up when choosing an upload path has been
   added when needed, since there were some cases in which it didn't appear.

   https://github.com/owncloud/android/issues/4340
   https://github.com/owncloud/android/pull/4535

* Bugfix - Downloading non-previewable files in details view leads to empty list: [#4428](https://github.com/owncloud/android/issues/4428)

   The error that led to an empty file list after downloading a file in details
   view, due to the bottom sheet "Open with", has been fixed.

   https://github.com/owncloud/android/issues/4428
   https://github.com/owncloud/android/pull/4548

* Bugfix - Ensure folder size updates automatically after file replacement: [#4505](https://github.com/owncloud/android/issues/4505)

   The folder size has been updated automatically after replacing a file during a
   move operation, eliminating the need for a manual refresh.

   https://github.com/owncloud/android/issues/4505
   https://github.com/owncloud/android/pull/4553

* Change - Replace auto-uploads with automatic uploads: [#4252](https://github.com/owncloud/android/issues/4252)

   Wording change in the feature name, in order to make it clearer in translations
   and documentation

   https://github.com/owncloud/android/issues/4252
   https://github.com/owncloud/android/pull/4492

* Change - Removed survey and chat from feedback: [#4540](https://github.com/owncloud/android/issues/4540)

   Survey and chat have been removed from the feedback dialog due to they are not
   maintained anymore or they have low traffic.

   https://github.com/owncloud/android/issues/4540
   https://github.com/owncloud/android/pull/4549

* Enhancement - Unit tests for repository classes - Part 2: [#4233](https://github.com/owncloud/android/issues/4233)

   Unit tests for OCFileRepository class have been completed.

   https://github.com/owncloud/android/issues/4233
   https://github.com/owncloud/android/pull/4389

* Enhancement - Unit tests for repository classes - Part 3: [#4234](https://github.com/owncloud/android/issues/4234)

   Unit tests for OCFolderBackupRepository, OCOAuthRepository,
   OCServerInfoRepository, OCShareeRepository, OCShareRepository classes have been
   completed.

   https://github.com/owncloud/android/issues/4234
   https://github.com/owncloud/android/pull/4523

* Enhancement - Unit tests for repository classes - Part 4: [#4235](https://github.com/owncloud/android/issues/4235)

   Unit tests for OCSpacesRepository, OCTransferRepository, OCUserRepository and
   OCWebFingerRepository classes have been completed.

   https://github.com/owncloud/android/issues/4235
   https://github.com/owncloud/android/pull/4537

* Enhancement - Add status message when (un)setting av. offline from preview: [#4382](https://github.com/owncloud/android/issues/4382)

   A message has been added in all previews when the (un)setting av. offline
   buttons are clicked. The options menu has been updated in all previews depending
   on the file status.

   https://github.com/owncloud/android/issues/4382
   https://github.com/owncloud/android/pull/4482

* Enhancement - Quota improvements from GraphAPI: [#4411](https://github.com/owncloud/android/issues/4411)

   The quota in the drawer has been updated depending on its status and also when a
   file is removed, copied, moved and after a refresh operation. In addition, the
   quota value for each account has been added in the manage accounts dialog.

   https://github.com/owncloud/android/issues/4411
   https://github.com/owncloud/android/pull/4496

* Enhancement - Upgraded AGP version to 8.7.2: [#4478](https://github.com/owncloud/android/issues/4478)

   The Android Gradle Plugin version has been upgraded to 8.7.2, together with
   Gradle version (updated to 8.9) and JDK version (updated to JBR 17).

   https://github.com/owncloud/android/issues/4478
   https://github.com/owncloud/android/pull/4507

* Enhancement - Added text labels for BottomNavigationView: [#4484](https://github.com/owncloud/android/issues/4484)

   Text labels have been added below the icons, and the active indicator feature is
   implemented using the default itemActiveIndicatorStyle for better navigation
   experience.

   https://github.com/owncloud/android/issues/4484
   https://github.com/owncloud/android/pull/4498

* Enhancement - OCIS Light Users: [#4490](https://github.com/owncloud/android/issues/4490)

   OCIS light users (users without personal space) are now supported in the app

   https://github.com/owncloud/android/issues/4490
   https://github.com/owncloud/android/pull/4518

* Enhancement - Enforce OIDC auth flow via branding: [#4500](https://github.com/owncloud/android/issues/4500)

   A new branded parameter `enforce_oidc` has been added to enforce the app to
   follow the OIDC auth flow, and `clientId` and `clientSecret` are sent in token
   requests when required by server. Moreover, the app now supports branded
   redirect URIs with path due to the new branded parameter
   `oauth2_redirect_uri_path` (legacy `oauth2_redirect_uri_path` is now
   `oauth2_redirect_uri_host`).

   https://github.com/owncloud/android/issues/4500
   https://github.com/owncloud/android/pull/4516

* Enhancement - Detekt: static code analyzer: [#4506](https://github.com/owncloud/android/issues/4506)

   The Kotlin static code analyzer Detekt has been introduced with the agreed
   rules, and the left code smells have been fixed throughout the whole code.

   https://github.com/owncloud/android/issues/4506
   https://github.com/owncloud/android/pull/4487

* Enhancement - Multi-Personal (1st round): [#4514](https://github.com/owncloud/android/issues/4514)

   Support for multi-personal accounts has been added. This first approach displays
   all personal spaces in the Spaces tab, not showing project spaces. In addition,
   the Personal tab shows an empty view since there is not a single personal space.

   https://github.com/owncloud/android/issues/4514
   https://github.com/owncloud/android/pull/4527/files

* Enhancement - Technical improvements for user quota: [#4521](https://github.com/owncloud/android/issues/4521)

   A new use case has been added to fetch the user quota as a flow. Also, all
   unnecessary calls from DrawerActivity have been removed.

   https://github.com/owncloud/android/issues/4521
   https://github.com/owncloud/android/pull/4525

# Changelog for ownCloud Android Client [4.4.1] (2024-10-30)

The following sections list the changes in ownCloud Android Client 4.4.1 relevant to
ownCloud admins and users.

[4.4.1]: https://github.com/owncloud/android/compare/v4.4.0...v4.4.1

## Summary

* Bugfix - File size becomes 0 after a local update: [#4495](https://github.com/owncloud/android/issues/4495)

## Details

* Bugfix - File size becomes 0 after a local update: [#4495](https://github.com/owncloud/android/issues/4495)

   The local copy of a file is not removed after a local update anymore. Therefore,
   the file size has been fixed.

   https://github.com/owncloud/android/issues/4495
   https://github.com/owncloud/android/pull/4502

# Changelog for ownCloud Android Client [4.4.0] (2024-09-30)

The following sections list the changes in ownCloud Android Client 4.4.0 relevant to
ownCloud admins and users.

[4.4.0]: https://github.com/owncloud/android/compare/v4.3.1...v4.4.0

## Summary

* Bugfix - Rely on `resharing` capability: [#4397](https://github.com/owncloud/android/issues/4397)
* Bugfix - Shares in non-root are updated correctly: [#4432](https://github.com/owncloud/android/issues/4432)
* Bugfix - List filtering not working after rotating device: [#4441](https://github.com/owncloud/android/issues/4441)
* Bugfix - The color of some elements is set up correctly: [#4442](https://github.com/owncloud/android/issues/4442)
* Bugfix - Audio player does not work: [#4474](https://github.com/owncloud/android/issues/4474)
* Bugfix - Buttons visibility in name conflicts dialog: [#4480](https://github.com/owncloud/android/pull/4480)
* Enhancement - Improved "Remove from original folder" option in auto-upload: [#4357](https://github.com/owncloud/android/issues/4357)
* Enhancement - Improved accessibility of information and relationships: [#4362](https://github.com/owncloud/android/issues/4362)
* Enhancement - Changed the color of some elements to improve accessibility: [#4364](https://github.com/owncloud/android/issues/4364)
* Enhancement - Improved SearchView accessibility: [#4365](https://github.com/owncloud/android/issues/4365)
* Enhancement - Roles added to some elements to improve accessibility: [#4373](https://github.com/owncloud/android/issues/4373)
* Enhancement - Hardware keyboard support: [#4438](https://github.com/owncloud/android/pull/4438)
* Enhancement - Hardware keyboard support for passcode view: [#4447](https://github.com/owncloud/android/issues/4447)
* Enhancement - TalkBack announces the view label correctly: [#4458](https://github.com/owncloud/android/issues/4458)

## Details

* Bugfix - Rely on `resharing` capability: [#4397](https://github.com/owncloud/android/issues/4397)

   The request to create a new share has been fixed so that it only includes the
   share permission by default when the resharing capability is true, and the "can
   share" switch in the edition view of private shares is now only shown when
   resharing is true.

   https://github.com/owncloud/android/issues/4397
   https://github.com/owncloud/android/pull/4472

* Bugfix - Shares in non-root are updated correctly: [#4432](https://github.com/owncloud/android/issues/4432)

   The items of the "Share" view are updated instantly when create/edit a link or
   share with users or groups in a non-root file.

   https://github.com/owncloud/android/issues/4432
   https://github.com/owncloud/android/pull/4435

* Bugfix - List filtering not working after rotating device: [#4441](https://github.com/owncloud/android/issues/4441)

   Configuration changes have been handled when rotating the device so that list
   filtering works.

   https://github.com/owncloud/android/issues/4441
   https://github.com/owncloud/android/pull/4467

* Bugfix - The color of some elements is set up correctly: [#4442](https://github.com/owncloud/android/issues/4442)

   The colors of the Manage Accounts header and status bar have been changed to be
   consistent with the branding colors.

   https://github.com/owncloud/android/issues/4442
   https://github.com/owncloud/android/pull/4463

* Bugfix - Audio player does not work: [#4474](https://github.com/owncloud/android/issues/4474)

   Audio player in Android 14+ devices wasn't working, so some proper permissions
   have been added in Manifest so that media can be played correctly in the
   foreground and background in all versions.

   https://github.com/owncloud/android/issues/4474
   https://github.com/owncloud/android/pull/4479

* Bugfix - Buttons visibility in name conflicts dialog: [#4480](https://github.com/owncloud/android/pull/4480)

   In some languages, labels for the buttons in the name conflicts dialog were too
   long and their visibility was very poor. These buttons have been placed in
   vertical instead of horizontal to avoid this problem.

   https://github.com/owncloud/android/pull/4480

* Enhancement - Improved "Remove from original folder" option in auto-upload: [#4357](https://github.com/owncloud/android/issues/4357)

   The file will be deleted locally after it has been uploaded to the server,
   avoiding the loss of the file if an error happens during the upload.

   https://github.com/owncloud/android/issues/4357
   https://github.com/owncloud/android/pull/4437

* Enhancement - Improved accessibility of information and relationships: [#4362](https://github.com/owncloud/android/issues/4362)

   Headings have been added to the following views: Share, Edit/Create Share Link,
   Standard Toolbar and Manage Accounts. The filename input field and the two
   switches are now linked to their labels. The 'contentDescription' attributes of
   the buttons in the Edit/Create Share Link view have also been updated.

   https://github.com/owncloud/android/issues/4362
   https://github.com/owncloud/android/issues/4363
   https://github.com/owncloud/android/issues/4371
   https://github.com/owncloud/android/pull/4448

* Enhancement - Changed the color of some elements to improve accessibility: [#4364](https://github.com/owncloud/android/issues/4364)

   The color of some UI elements has been changed to meet minimum color contrast
   requirements.

   https://github.com/owncloud/android/issues/4364
   https://github.com/owncloud/android/pull/4429

* Enhancement - Improved SearchView accessibility: [#4365](https://github.com/owncloud/android/issues/4365)

   The text hint and cross button color of the SearchView has been changed to meet
   the color contrast requirements. In addition, the SearchView includes a new
   resource with rounded edges, using the same background color (brandable) as the
   containing toolbar.

   https://github.com/owncloud/android/issues/4365
   https://github.com/owncloud/android/pull/4433

* Enhancement - Roles added to some elements to improve accessibility: [#4373](https://github.com/owncloud/android/issues/4373)

   Roles have been added to specific elements within the following views: Toolbar,
   Spaces, Drawer Menu, Manage accounts and Floating Action Button. Improved the
   navigation system within the passcode view.

   https://github.com/owncloud/android/issues/4373
   https://github.com/owncloud/android/pull/4454
   https://github.com/owncloud/android/pull/4466

* Enhancement - Hardware keyboard support: [#4438](https://github.com/owncloud/android/pull/4438)

   Navigation via hardware keyboard has been improved so that now focus order has a
   logical path, every element is reachable and there are no traps. These
   improvements have been applied in main file list, spaces list, drawer menu,
   share view and image preview.

   https://github.com/owncloud/android/issues/4366
   https://github.com/owncloud/android/issues/4367
   https://github.com/owncloud/android/issues/4368
   https://github.com/owncloud/android/pull/4438

* Enhancement - Hardware keyboard support for passcode view: [#4447](https://github.com/owncloud/android/issues/4447)

   Navigation via hardware keyboard has been added to the passcode view.

   https://github.com/owncloud/android/issues/4447
   https://github.com/owncloud/android/pull/4455

* Enhancement - TalkBack announces the view label correctly: [#4458](https://github.com/owncloud/android/issues/4458)

   TalkBack no longer announces "ownCloud" every time the screen changes. Now, it
   correctly dictates the name of the current view.

   https://github.com/owncloud/android/issues/4458
   https://github.com/owncloud/android/pull/4470

# Changelog for ownCloud Android Client [4.3.1] (2024-07-22)

The following sections list the changes in ownCloud Android Client 4.3.1 relevant to
ownCloud admins and users.

[4.3.1]: https://github.com/owncloud/android/compare/v4.3.0...v4.3.1

## Summary

* Change - Bump target SDK to 34: [#4434](https://github.com/owncloud/android/issues/4434)

## Details

* Change - Bump target SDK to 34: [#4434](https://github.com/owncloud/android/issues/4434)

   Target SDK was upgraded to 34 in order to fulfill Android platform requirements.

   https://github.com/owncloud/android/issues/4434
   https://github.com/owncloud/android/pull/4440

# Changelog for ownCloud Android Client [4.3.0] (2024-07-01)

The following sections list the changes in ownCloud Android Client 4.3.0 relevant to
ownCloud admins and users.

[4.3.0]: https://github.com/owncloud/android/compare/v4.2.2...v4.3.0

## Summary

* Bugfix - Removed unnecessary requests when the app is installed from scratch: [#4213](https://github.com/owncloud/android/issues/4213)
* Bugfix - "Clear data" button enabled in the app settings in device settings: [#4309](https://github.com/owncloud/android/issues/4309)
* Bugfix - Video streaming in spaces: [#4328](https://github.com/owncloud/android/issues/4328)
* Bugfix - Retried successful uploads are cleaned up from the temporary folder: [#4335](https://github.com/owncloud/android/issues/4335)
* Bugfix - Resolve incorrect truncation of long display names in Manage Accounts: [#4351](https://github.com/owncloud/android/issues/4351)
* Bugfix - Av. offline files are not removed when "Local only" option is clicked: [#4353](https://github.com/owncloud/android/issues/4353)
* Bugfix - Unwanted DELETE operations when synchronization in single file fails: [#6638](https://github.com/owncloud/enterprise/issues/6638)
* Change - Upgrade minimum SDK version to Android 7.0 (v24): [#4230](https://github.com/owncloud/android/issues/4230)
* Change - Automatic discovery of the account in login: [#4301](https://github.com/owncloud/android/issues/4301)
* Change - Add new prefixes in commit messages of 3rd party contributors: [#4346](https://github.com/owncloud/android/pull/4346)
* Change - Kotlinize PreviewTextFragment: [#4356](https://github.com/owncloud/android/issues/4356)
* Enhancement - Add search functionality to spaces list: [#3865](https://github.com/owncloud/android/issues/3865)
* Enhancement - Get personal space quota from GraphAPI: [#3874](https://github.com/owncloud/android/issues/3874)
* Enhancement - Correct "Local only" option in remove dialog: [#3936](https://github.com/owncloud/android/issues/3936)
* Enhancement - Show app provider icon from endpoint: [#4105](https://github.com/owncloud/android/issues/4105)
* Enhancement - Improvements in Manage Accounts view: [#4148](https://github.com/owncloud/android/issues/4148)
* Enhancement - New setting for manual removal of local storage: [#4174](https://github.com/owncloud/android/issues/4174)
* Enhancement - New setting for automatic removal of local files: [#4175](https://github.com/owncloud/android/issues/4175)
* Enhancement - Avoid unnecessary requests when an av. offline folder is refreshed: [#4197](https://github.com/owncloud/android/issues/4197)
* Enhancement - Unit tests for repository classes - Part 1: [#4232](https://github.com/owncloud/android/issues/4232)
* Enhancement - Add a warning in http connections: [#4284](https://github.com/owncloud/android/issues/4284)
* Enhancement - Make dialog more Android-alike: [#4303](https://github.com/owncloud/android/issues/4303)
* Enhancement - Password generator for public links in oCIS: [#4308](https://github.com/owncloud/android/issues/4308)
* Enhancement - New UI for "Manage accounts" view: [#4312](https://github.com/owncloud/android/issues/4312)
* Enhancement - Improvements in remove dialog: [#4342](https://github.com/owncloud/android/issues/4342)
* Enhancement - Content description in UI elements to improve accessibility: [#4360](https://github.com/owncloud/android/issues/4360)
* Enhancement - Added contentDescription attribute in the previewed image: [#4360](https://github.com/owncloud/android/issues/4360)
* Enhancement - Support for URL shortcut files: [#4413](https://github.com/owncloud/android/issues/4413)
* Enhancement - Changes in the Feedback section: [#6594](https://github.com/owncloud/enterprise/issues/6594)

## Details

* Bugfix - Removed unnecessary requests when the app is installed from scratch: [#4213](https://github.com/owncloud/android/issues/4213)

   Some requests to the server that were not necessary when installing the app from
   scratch have been removed.

   https://github.com/owncloud/android/issues/4213
   https://github.com/owncloud/android/pull/4385

* Bugfix - "Clear data" button enabled in the app settings in device settings: [#4309](https://github.com/owncloud/android/issues/4309)

   The "Clear data" button has been enabled to delete the application data from the
   app settings in the device settings. Shared preferences, temporary files,
   accounts and the local database will be cleared when the button is pressed.

   https://github.com/owncloud/android/issues/4309
   https://github.com/owncloud/android/pull/4350

* Bugfix - Video streaming in spaces: [#4328](https://github.com/owncloud/android/issues/4328)

   The URI formed to perform video streaming in spaces has been adapted to oCIS
   accounts so that it takes into account the space where the file is located.

   https://github.com/owncloud/android/issues/4328
   https://github.com/owncloud/android/pull/4394

* Bugfix - Retried successful uploads are cleaned up from the temporary folder: [#4335](https://github.com/owncloud/android/issues/4335)

   Temporary files related to a failed upload are deleted after retrying it and
   being successfully completed.

   https://github.com/owncloud/android/issues/4335
   https://github.com/owncloud/android/pull/4341

* Bugfix - Resolve incorrect truncation of long display names in Manage Accounts: [#4351](https://github.com/owncloud/android/issues/4351)

   Resolved the bug where long display names were truncated incorrectly in the
   Manage Accounts view. Now, display names are properly truncated in the middle
   with ellipsis (...) to maintain readability.

   https://github.com/owncloud/android/issues/4351
   https://github.com/owncloud/android/pull/4380

* Bugfix - Av. offline files are not removed when "Local only" option is clicked: [#4353](https://github.com/owncloud/android/issues/4353)

   "Local only" option in remove dialog will be displayed when the selected folder
   contains at least one downloaded file, ignoring those available offline. If the
   "Local only" option is displayed and clicked, available offline files will not
   be deleted.

   https://github.com/owncloud/android/issues/4353
   https://github.com/owncloud/android/pull/4399

* Bugfix - Unwanted DELETE operations when synchronization in single file fails: [#6638](https://github.com/owncloud/enterprise/issues/6638)

   A new exception is now thrown and handled when the account of the network client
   is null, avoiding DELETE requests to the server when synchronization (PROPFIND)
   on a single file responds with 404. Also, when PROPFINDs respond with 404, the
   delete operation has been changed to be just local and not remote too.

   https://github.com/owncloud/enterprise/issues/6638
   https://github.com/owncloud/android/pull/4408

* Change - Upgrade minimum SDK version to Android 7.0 (v24): [#4230](https://github.com/owncloud/android/issues/4230)

   The minimum Android version will be Android 7.0 Nougat (API 24). The application
   will no longer support previous versions.

   https://github.com/owncloud/android/issues/4230
   https://github.com/owncloud/android/pull/4299

* Change - Automatic discovery of the account in login: [#4301](https://github.com/owncloud/android/issues/4301)

   Automatic account discovery is done at login. Removed the refresh account button
   in the Manage Accounts view.

   https://github.com/owncloud/android/issues/4301
   https://github.com/owncloud/android/pull/4325

* Change - Add new prefixes in commit messages of 3rd party contributors: [#4346](https://github.com/owncloud/android/pull/4346)

   Dependaboy and Calens' commit messages with prefixes that fits 'Conventional
   Commits'

   https://github.com/owncloud/android/pull/4346

* Change - Kotlinize PreviewTextFragment: [#4356](https://github.com/owncloud/android/issues/4356)

   PreviewTextFragment class has been moved from Java to Kotlin.

   https://github.com/owncloud/android/issues/4356
   https://github.com/owncloud/android/pull/4376

* Enhancement - Add search functionality to spaces list: [#3865](https://github.com/owncloud/android/issues/3865)

   Search functionality was added in spaces list when you are trying to filter
   them.

   https://github.com/owncloud/android/issues/3865
   https://github.com/owncloud/android/pull/4393

* Enhancement - Get personal space quota from GraphAPI: [#3874](https://github.com/owncloud/android/issues/3874)

   Personal space quota in an oCIS account has been added from GraphAPI instead of
   propfind.

   https://github.com/owncloud/android/issues/3874
   https://github.com/owncloud/android/pull/4401

* Enhancement - Correct "Local only" option in remove dialog: [#3936](https://github.com/owncloud/android/issues/3936)

   "Local only" option in remove dialog will only be shown if checking selected
   files and folders recursively, at least one file is available locally.

   https://github.com/owncloud/android/issues/3936
   https://github.com/owncloud/android/pull/4289

* Enhancement - Show app provider icon from endpoint: [#4105](https://github.com/owncloud/android/issues/4105)

   App provider icon fetched from the server has been added to the "Open in (web)"
   option on the bottom sheet that appears when clicking the 3-dots button of a
   file.

   https://github.com/owncloud/android/issues/4105
   https://github.com/owncloud/android/pull/4391

* Enhancement - Improvements in Manage Accounts view: [#4148](https://github.com/owncloud/android/issues/4148)

   Removed the key icon and avoid overlap account name with icons in Manage
   Accounts. Redirect to login when snackbar appears in authentication failure.

   https://github.com/owncloud/android/issues/4148
   https://github.com/owncloud/android/pull/4330

* Enhancement - New setting for manual removal of local storage: [#4174](https://github.com/owncloud/android/issues/4174)

   A new icon has been added in Manage Accounts view to delete manually local
   files.

   https://github.com/owncloud/android/issues/4174
   https://github.com/owncloud/android/pull/4334

* Enhancement - New setting for automatic removal of local files: [#4175](https://github.com/owncloud/android/issues/4175)

   A new setting has been created to delete automatically downloaded files, when
   the time since their last usage exceeds the selected time in the setting.

   https://github.com/owncloud/android/issues/4175
   https://github.com/owncloud/android/pull/4320

* Enhancement - Avoid unnecessary requests when an av. offline folder is refreshed: [#4197](https://github.com/owncloud/android/issues/4197)

   The available offline folders will only be refreshed when their eTag from the
   server and the corresponding one of the local database are different, avoiding
   sending unnecessary request.

   https://github.com/owncloud/android/issues/4197
   https://github.com/owncloud/android/pull/4354

* Enhancement - Unit tests for repository classes - Part 1: [#4232](https://github.com/owncloud/android/issues/4232)

   Unit tests for OCAppRegistryRepository, OCAuthenticationRepository and
   OCCapabilityRepository classes have been completed.

   https://github.com/owncloud/android/issues/4232
   https://github.com/owncloud/android/pull/4281

* Enhancement - Add a warning in http connections: [#4284](https://github.com/owncloud/android/issues/4284)

   Warning dialog has been added in the login screen when you are trying to connect
   to a http server.

   https://github.com/owncloud/android/issues/4284
   https://github.com/owncloud/android/pull/4345

* Enhancement - Make dialog more Android-alike: [#4303](https://github.com/owncloud/android/issues/4303)

   Name conflicts dialog appearance was changed to look Android-alike and more
   similar to other dialogs in the app.

   https://github.com/owncloud/android/issues/4303
   https://github.com/owncloud/android/pull/4336

* Enhancement - Password generator for public links in oCIS: [#4308](https://github.com/owncloud/android/issues/4308)

   A new password generator has been added to the public links creation view in
   oCIS accounts, which creates passwords that fulfill all the policies coming from
   server in a cryptographically secure way.

   https://github.com/owncloud/android/issues/4308
   https://github.com/owncloud/android/pull/4349

* Enhancement - New UI for "Manage accounts" view: [#4312](https://github.com/owncloud/android/issues/4312)

   A new dialog has been added to substitute the previous view for "Manage
   accounts". In addition, all the accounts management related stuff has been
   removed from the drawer menu in order not to show repetitive actions and make
   this menu simpler.

   https://github.com/owncloud/android/issues/4312
   https://github.com/owncloud/android/pull/4410

* Enhancement - Improvements in remove dialog: [#4342](https://github.com/owncloud/android/issues/4342)

   A new remove dialog has been created by adding the thumbnail of the file to be
   deleted. Also, when removing files in multiple selection, the number of elements
   that are going to be removed is displayed in the dialog.

   https://github.com/owncloud/android/issues/4342
   https://github.com/owncloud/android/issues/4377
   https://github.com/owncloud/android/pull/4348
   https://github.com/owncloud/android/pull/4404

* Enhancement - Content description in UI elements to improve accessibility: [#4360](https://github.com/owncloud/android/issues/4360)

   A description of the meaning or action associated with some UI elements has been
   included as alternative text to make the application more accessible. Views
   improved: toolbar, file list, spaces list, share, drawer menu, manage accounts
   and image preview.

   https://github.com/owncloud/android/issues/4360
   https://github.com/owncloud/android/pull/4387

* Enhancement - Added contentDescription attribute in the previewed image: [#4360](https://github.com/owncloud/android/issues/4360)

   A contentDescription attribute has been added to previewed image to make the
   application more accessible.

   https://github.com/owncloud/android/issues/4360
   https://github.com/owncloud/android/pull/4388

* Enhancement - Support for URL shortcut files: [#4413](https://github.com/owncloud/android/issues/4413)

   A new option has been added in the FAB to create a shortcut file with a .url
   extension. When the file is clicked, the URL will open in the browser.

   https://github.com/owncloud/android/issues/4413
   https://github.com/owncloud/android/pull/4420

* Enhancement - Changes in the Feedback section: [#6594](https://github.com/owncloud/enterprise/issues/6594)

   Based on a brandable parameter, a new dialog has been added to handle feedback.
   Within the dialog, links to the survey, GitHub and the open forum Central will
   be displayed.

   https://github.com/owncloud/enterprise/issues/6594
   https://github.com/owncloud/android/pull/4423

# Changelog for ownCloud Android Client [4.2.2] (2024-05-30)

The following sections list the changes in ownCloud Android Client 4.2.2 relevant to
ownCloud admins and users.

[4.2.2]: https://github.com/owncloud/android/compare/v4.2.1...v4.2.2

## Summary

* Bugfix - Downloads not working when `Content-Length` is not received: [#4352](https://github.com/owncloud/android/issues/4352)

## Details

* Bugfix - Downloads not working when `Content-Length` is not received: [#4352](https://github.com/owncloud/android/issues/4352)

   The case when Content-Length header is not received in the response of a GET for
   a download has been handled, and now the progress bar in images preview and
   details view is indeterminate for those cases.

   https://github.com/owncloud/android/issues/4352
   https://github.com/owncloud/android/pull/4415

# Changelog for ownCloud Android Client [4.2.1] (2024-02-22)

The following sections list the changes in ownCloud Android Client 4.2.1 relevant to
ownCloud admins and users.

[4.2.1]: https://github.com/owncloud/android/compare/v4.2.0...v4.2.1

## Summary

* Bugfix - Some crashes in 4.2.0: [#4318](https://github.com/owncloud/android/issues/4318)

## Details

* Bugfix - Some crashes in 4.2.0: [#4318](https://github.com/owncloud/android/issues/4318)

   Several crashes reported by Play Console in version 4.2.0 have been fixed.

   https://github.com/owncloud/android/issues/4318
   https://github.com/owncloud/android/pull/4323

# Changelog for ownCloud Android Client [4.2.0] (2024-02-12)

The following sections list the changes in ownCloud Android Client 4.2.0 relevant to
ownCloud admins and users.

[4.2.0]: https://github.com/owncloud/android/compare/v4.1.1...v4.2.0

## Summary

* Security - Improve biometric authentication security: [#4180](https://github.com/owncloud/android/issues/4180)
* Bugfix - Fixed AlertDialog title theme in Samsung Devices: [#3192](https://github.com/owncloud/android/issues/3192)
* Bugfix - Some Null Pointer Exceptions in MainFileListViewModel: [#4065](https://github.com/owncloud/android/issues/4065)
* Bugfix - Bugs related to Details view: [#4188](https://github.com/owncloud/android/issues/4188)
* Bugfix - Some Null Pointer Exceptions fixed from Google Play: [#4207](https://github.com/owncloud/android/issues/4207)
* Bugfix - Conflict in copy with files without extension: [#4222](https://github.com/owncloud/android/issues/4222)
* Bugfix - Add "scope" parameter to /token endpoint HTTP requests: [#4260](https://github.com/owncloud/android/pull/4260)
* Bugfix - Fix in the handling of the base URL: [#4279](https://github.com/owncloud/android/issues/4279)
* Bugfix - Handle Http 423 (resource locked): [#4282](https://github.com/owncloud/android/issues/4282)
* Bugfix - Copy folder into descendant in different spaces: [#4293](https://github.com/owncloud/android/issues/4293)
* Change - Android library as a module instead of submodule: [#3962](https://github.com/owncloud/android/issues/3962)
* Change - Migration to Media3 from Exoplayer: [#4157](https://github.com/owncloud/android/issues/4157)
* Enhancement - Koin DSL: [#3966](https://github.com/owncloud/android/pull/3966)
* Enhancement - Unit tests for datasources classes - Part 1 & Fixes: [#4063](https://github.com/owncloud/android/issues/4063)
* Enhancement - Unit tests for datasources classes - Part 3: [#4072](https://github.com/owncloud/android/issues/4072)
* Enhancement - "Apply to all" when many name conflicts arise: [#4078](https://github.com/owncloud/android/issues/4078)
* Enhancement - "Share to" in oCIS accounts allows upload to any space: [#4088](https://github.com/owncloud/android/issues/4088)
* Enhancement - Auto-refresh when a file is uploaded: [#4103](https://github.com/owncloud/android/issues/4103)
* Enhancement - Auto upload in oCIS accounts allows upload to any space: [#4117](https://github.com/owncloud/android/issues/4117)
* Enhancement - Thumbnail improvements in grid view: [#4145](https://github.com/owncloud/android/issues/4145)
* Enhancement - Logging changes: [#4151](https://github.com/owncloud/android/issues/4151)
* Enhancement - Download log files on Android10+ devices: [#4155](https://github.com/owncloud/android/issues/4155)
* Enhancement - Log file sharing allowed within ownCloud Android app: [#4156](https://github.com/owncloud/android/issues/4156)
* Enhancement - New field "last usage" in database: [#4173](https://github.com/owncloud/android/issues/4173)
* Enhancement - Use invoke operator to execute usecases: [#4179](https://github.com/owncloud/android/pull/4179)
* Enhancement - Deep link open app correctly: [#4181](https://github.com/owncloud/android/issues/4181)
* Enhancement - Select user and navigate to file when opening via deep link: [#4194](https://github.com/owncloud/android/issues/4194)
* Enhancement - New branding/MDM parameter to show sensitive auth info in logs: [#4249](https://github.com/owncloud/android/issues/4249)
* Enhancement - Fix in the type handling of the content-type: [#4258](https://github.com/owncloud/android/issues/4258)
* Enhancement - Prevent that two media files are playing at the same time: [#4263](https://github.com/owncloud/android/pull/4263)
* Enhancement - Added icon for .docxf files: [#4267](https://github.com/owncloud/android/issues/4267)
* Enhancement - Manage password policy in live mode: [#4269](https://github.com/owncloud/android/issues/4269)
* Enhancement - New branding/MDM parameter to send `login_hint` and `user` params: [#4288](https://github.com/owncloud/android/issues/4288)

## Details

* Security - Improve biometric authentication security: [#4180](https://github.com/owncloud/android/issues/4180)

   Biometric authentication has been improved by checking the result received when
   performing a successful authentication.

   https://github.com/owncloud/android/issues/4180
   https://github.com/owncloud/android/pull/4283

* Bugfix - Fixed AlertDialog title theme in Samsung Devices: [#3192](https://github.com/owncloud/android/issues/3192)

   Use of device default theme was removed.

   https://github.com/owncloud/android/issues/3192
   https://github.com/owncloud/android/pull/4277

* Bugfix - Some Null Pointer Exceptions in MainFileListViewModel: [#4065](https://github.com/owncloud/android/issues/4065)

   The MainFileListViewModel has prevented the fileById variable from crashing when
   a null value is found.

   https://github.com/owncloud/android/issues/4065
   https://github.com/owncloud/android/pull/4241

* Bugfix - Bugs related to Details view: [#4188](https://github.com/owncloud/android/issues/4188)

   When coming to Details view from video or image previews, now the top bar is
   shown correctly and navigation has the correct stack, so the back button has the
   expected flow.

   https://github.com/owncloud/android/issues/4188
   https://github.com/owncloud/android/pull/4265

* Bugfix - Some Null Pointer Exceptions fixed from Google Play: [#4207](https://github.com/owncloud/android/issues/4207)

   FileDisplayActivity and ReceiverExternalFilesActivity have prevented some
   functions from crashing when a null value is found.

   https://github.com/owncloud/android/issues/4207
   https://github.com/owncloud/android/pull/4238

* Bugfix - Conflict in copy with files without extension: [#4222](https://github.com/owncloud/android/issues/4222)

   The check of files names that start in the same way has been removed from the
   copy network operation, so that the copy use case takes care of that and works
   properly with files without extension.

   https://github.com/owncloud/android/issues/4222
   https://github.com/owncloud/android/pull/4294

* Bugfix - Add "scope" parameter to /token endpoint HTTP requests: [#4260](https://github.com/owncloud/android/pull/4260)

   The "scope" parameter is now always sent in the body of HTTP requests to the
   /token endpoint, which is optional in v1 but required in v2.

   https://github.com/owncloud/android/pull/4260

* Bugfix - Fix in the handling of the base URL: [#4279](https://github.com/owncloud/android/issues/4279)

   Base URL has been formatted in GetRemoteAppRegistryOperation when server
   instance is installed in subfolder, so that the endpoint is formed correctly.

   https://github.com/owncloud/android/issues/4279
   https://github.com/owncloud/android/pull/4287

* Bugfix - Handle Http 423 (resource locked): [#4282](https://github.com/owncloud/android/issues/4282)

   App can gracefully show if the file is locked when done certain operations on
   it.

   https://github.com/owncloud/android/issues/4282
   https://github.com/owncloud/android/pull/4285

* Bugfix - Copy folder into descendant in different spaces: [#4293](https://github.com/owncloud/android/issues/4293)

   Copying a folder into another folder with the same name in a different space now
   works correctly.

   https://github.com/owncloud/android/issues/4293
   https://github.com/owncloud/android/pull/4295

* Change - Android library as a module instead of submodule: [#3962](https://github.com/owncloud/android/issues/3962)

   Android library, containing all networking stuff, is now the 5th module in the
   app instead of submodule.

   https://github.com/owncloud/android/issues/3962
   https://github.com/owncloud/android/pull/4183

* Change - Migration to Media3 from Exoplayer: [#4157](https://github.com/owncloud/android/issues/4157)

   Media3 is the new home for Exoplayer, which has become a part of this library.
   Media3 provides a more advanced and optimized media playback experience for
   users, with improvements in performance and compatibility.

   https://github.com/owncloud/android/issues/4157
   https://github.com/owncloud/android/pull/4177

* Enhancement - Koin DSL: [#3966](https://github.com/owncloud/android/pull/3966)

   Koin DSL makes easier the dependency definition avoiding verbosity by allowing
   you to target a class constructor directly

   https://github.com/owncloud/android/pull/3966

* Enhancement - Unit tests for datasources classes - Part 1 & Fixes: [#4063](https://github.com/owncloud/android/issues/4063)

   Unit tests for OCLocalAppRegistryDataSource, OCRemoteAppRegistryDataSource,
   OCLocalAuthenticationDataSource, OCRemoteAuthenticationDataSource,
   OCLocalCapabilitiesDataSource and OCRemoteCapabilitiesDataSource classes have
   been done and completed, and several fixes have been applied to all existent
   unit test classes for datasources.

   https://github.com/owncloud/android/issues/4063
   https://github.com/owncloud/android/pull/4209

* Enhancement - Unit tests for datasources classes - Part 3: [#4072](https://github.com/owncloud/android/issues/4072)

   Unit tests of the OCFolderBackupLocalDataSource, OCRemoteOAuthDataSource,
   OCRemoteShareeDataSource, OCLocalShareDataSource, OCRemoteShareDataSource,
   OCLocalSpacesDataSource, OCRemoteSpacesDataSource, OCLocalTransferDataSource,
   OCLocalUserDataSource, OCRemoteUserDataSource, OCRemoteWebFingerDatasource
   classes have been done and completed.

   https://github.com/owncloud/android/issues/4072
   https://github.com/owncloud/android/pull/4143

* Enhancement - "Apply to all" when many name conflicts arise: [#4078](https://github.com/owncloud/android/issues/4078)

   A new dialog has been created where a checkbox has been added to be able to
   select all the folders or files that have conflicts.

   https://github.com/owncloud/android/issues/4078
   https://github.com/owncloud/android/pull/4138

* Enhancement - "Share to" in oCIS accounts allows upload to any space: [#4088](https://github.com/owncloud/android/issues/4088)

   With this improvement, shared stuff from other apps can be uploaded to any space
   and not only the personal one in oCIS accounts.

   https://github.com/owncloud/android/issues/4088
   https://github.com/owncloud/android/pull/4160

* Enhancement - Auto-refresh when a file is uploaded: [#4103](https://github.com/owncloud/android/issues/4103)

   The file list will be now refreshed automatically when an upload whose
   destination folder is the one we are in is completed successfully.

   https://github.com/owncloud/android/issues/4103
   https://github.com/owncloud/android/pull/4199

* Enhancement - Auto upload in oCIS accounts allows upload to any space: [#4117](https://github.com/owncloud/android/issues/4117)

   Auto uploads of images and videos can now be uploaded to any space and not only
   the personal one in oCIS accounts.

   https://github.com/owncloud/android/issues/4117
   https://github.com/owncloud/android/pull/4214

* Enhancement - Thumbnail improvements in grid view: [#4145](https://github.com/owncloud/android/issues/4145)

   Grid view was improved by adding the file name to images when the thumbnail is
   null.

   https://github.com/owncloud/android/issues/4145
   https://github.com/owncloud/android/pull/4237

* Enhancement - Logging changes: [#4151](https://github.com/owncloud/android/issues/4151)

   - Updating version of com.github.AppDevNext.Logcat:LogcatCoreLib lib. - Adding
   the hour, minutes and seconds to the log file. - Printing http logs in one line.
   - Printing http logs with 1000000 bytes as max size. - Printing http logs in a
   Json format.

   https://github.com/owncloud/android/issues/4151
   https://github.com/owncloud/android/pull/4204

* Enhancement - Download log files on Android10+ devices: [#4155](https://github.com/owncloud/android/issues/4155)

   A new icon to download a log file to the Downloads folder of the device has been
   added to the log list screen on Android10+ devices.

   https://github.com/owncloud/android/issues/4155
   https://github.com/owncloud/android/pull/4205

* Enhancement - Log file sharing allowed within ownCloud Android app: [#4156](https://github.com/owncloud/android/issues/4156)

   Sharing log files to the ownCloud app itself is now possible from the logs
   screen.

   https://github.com/owncloud/android/issues/4156
   https://github.com/owncloud/android/pull/4215

* Enhancement - New field "last usage" in database: [#4173](https://github.com/owncloud/android/issues/4173)

   To know the last usage of a file, a new field has been created in the database
   to handle this specific information.

   https://github.com/owncloud/android/issues/4173
   https://github.com/owncloud/android/pull/4187

* Enhancement - Use invoke operator to execute usecases: [#4179](https://github.com/owncloud/android/pull/4179)

   Removes all the "execute" verbosity for use cases by using the "invoke" operator
   instead.

   https://github.com/owncloud/android/pull/4179

* Enhancement - Deep link open app correctly: [#4181](https://github.com/owncloud/android/issues/4181)

   Opening the app with the deep link correctly and managing if user logged or not.

   https://github.com/owncloud/android/issues/4181
   https://github.com/owncloud/android/pull/4191

* Enhancement - Select user and navigate to file when opening via deep link: [#4194](https://github.com/owncloud/android/issues/4194)

   Select the correct user owner of the deep link file, managing possible errors
   and navigating to the correct file.

   https://github.com/owncloud/android/issues/4194
   https://github.com/owncloud/android/pull/4212

* Enhancement - New branding/MDM parameter to show sensitive auth info in logs: [#4249](https://github.com/owncloud/android/issues/4249)

   A new branding and MDM parameter has been created to decide if the sensitive
   information put in the authorization header in HTTP requests is shown or not in
   the logs.

   https://github.com/owncloud/android/issues/4249
   https://github.com/owncloud/android/pull/4257

* Enhancement - Fix in the type handling of the content-type: [#4258](https://github.com/owncloud/android/issues/4258)

   The content-type `application/jrd+json` has been added to the loggable types
   list, so that body in some requests and responses can be correctly logged.

   https://github.com/owncloud/android/issues/4258
   https://github.com/owncloud/android/pull/4266

* Enhancement - Prevent that two media files are playing at the same time: [#4263](https://github.com/owncloud/android/pull/4263)

   The player handles the audio focus shifts, pausing one player if another starts.

   https://github.com/owncloud/android/pull/4263

* Enhancement - Added icon for .docxf files: [#4267](https://github.com/owncloud/android/issues/4267)

   An icon has been added for files that have a .docxf extension.

   https://github.com/owncloud/android/issues/4267
   https://github.com/owncloud/android/pull/4297

* Enhancement - Manage password policy in live mode: [#4269](https://github.com/owncloud/android/issues/4269)

   Password policy for public links is handled in live mode with new items in the
   dialog.

   https://github.com/owncloud/android/issues/4269
   https://github.com/owncloud/android/pull/4276

* Enhancement - New branding/MDM parameter to send `login_hint` and `user` params: [#4288](https://github.com/owncloud/android/issues/4288)

   A new branding and MDM parameter has been created to decide if `login_hint` and
   `user` are sent as parameters in the login request, so that a value is shown in
   the Username text field.

   https://github.com/owncloud/android/issues/4288
   https://github.com/owncloud/android/pull/4291

# Changelog for ownCloud Android Client [4.1.1] (2023-10-18)

The following sections list the changes in ownCloud Android Client 4.1.1 relevant to
ownCloud admins and users.

[4.1.1]: https://github.com/owncloud/android/compare/v4.1.0...v4.1.1

## Summary

* Bugfix - Some Null Pointer Exceptions avoided: [#4158](https://github.com/owncloud/android/issues/4158)
* Bugfix - Thumbnails correctly shown for every user: [#4189](https://github.com/owncloud/android/pull/4189)

## Details

* Bugfix - Some Null Pointer Exceptions avoided: [#4158](https://github.com/owncloud/android/issues/4158)

   In the detail screen, in the main file list ViewModel and in the OCFile
   repository the app has been prevented from crashing when a null is found.

   https://github.com/owncloud/android/issues/4158
   https://github.com/owncloud/android/pull/4170

* Bugfix - Thumbnails correctly shown for every user: [#4189](https://github.com/owncloud/android/pull/4189)

   Due to an error in the request, users that included the '@' character in their
   usernames couldn't see the thumbnails of the image files. Now, every user can
   see them correctly.

   https://github.com/owncloud/android/pull/4189

# Changelog for ownCloud Android Client [4.1.0] (2023-08-23)

The following sections list the changes in ownCloud Android Client 4.1.0 relevant to
ownCloud admins and users.

[4.1.0]: https://github.com/owncloud/android/compare/v4.0.0...v4.1.0

## Summary

* Bugfix - Spaces' thumbnails not loaded the first time: [#3959](https://github.com/owncloud/android/issues/3959)
* Bugfix - Bad error message when copying/moving with server down: [#4044](https://github.com/owncloud/android/issues/4044)
* Bugfix - Unnecessary or wrong call: [#4074](https://github.com/owncloud/android/issues/4074)
* Bugfix - Menu option unset av. offline shown when shouldn't: [#4077](https://github.com/owncloud/android/issues/4077)
* Bugfix - List of accounts empty after removing all accounts and adding new ones: [#4114](https://github.com/owncloud/android/issues/4114)
* Bugfix - Crash when the token is expired: [#4116](https://github.com/owncloud/android/issues/4116)
* Change - Upgrade min SDK to Android 6 (API 23): [#3245](https://github.com/owncloud/android/issues/3245)
* Change - Move file menu options filter to use case: [#4009](https://github.com/owncloud/android/issues/4009)
* Change - Gradle Version Catalog: [#4035](https://github.com/owncloud/android/pull/4035)
* Change - Remove "ignore" from the debug flavour Android manifest: [#4064](https://github.com/owncloud/android/pull/4064)
* Change - Not opening browser automatically in login: [#4067](https://github.com/owncloud/android/issues/4067)
* Change - Added new unit tests for providers: [#4073](https://github.com/owncloud/android/issues/4073)
* Change - New detail screen file design: [#4098](https://github.com/owncloud/android/pull/4098)
* Enhancement - Show "More" button for every file list item: [#2885](https://github.com/owncloud/android/issues/2885)
* Enhancement - Added "Open in web" options to main file list: [#3860](https://github.com/owncloud/android/issues/3860)
* Enhancement - Copy/move conflict solved by users: [#3935](https://github.com/owncloud/android/issues/3935)
* Enhancement - Improve grid mode: [#4027](https://github.com/owncloud/android/issues/4027)
* Enhancement - Improve UX of creation dialog: [#4031](https://github.com/owncloud/android/issues/4031)
* Enhancement - File name conflict starting by (1): [#4040](https://github.com/owncloud/android/pull/4040)
* Enhancement - Force security if not protected: [#4061](https://github.com/owncloud/android/issues/4061)
* Enhancement - Prevent http traffic with branding options: [#4066](https://github.com/owncloud/android/issues/4066)
* Enhancement - Unit tests for datasources classes - Part 2: [#4071](https://github.com/owncloud/android/issues/4071)
* Enhancement - Respect app_providers_appsUrl value from capabilities: [#4075](https://github.com/owncloud/android/issues/4075)
* Enhancement - Apply (1) to uploads' name conflicts: [#4079](https://github.com/owncloud/android/issues/4079)
* Enhancement - Support "per app" language change on Android 13+: [#4082](https://github.com/owncloud/android/issues/4082)
* Enhancement - Align Sharing icons with other platforms: [#4101](https://github.com/owncloud/android/issues/4101)

## Details

* Bugfix - Spaces' thumbnails not loaded the first time: [#3959](https://github.com/owncloud/android/issues/3959)

   Changing our own lazy image loading with coil library in spaces and file list.

   https://github.com/owncloud/android/issues/3959
   https://github.com/owncloud/android/pull/4084

* Bugfix - Bad error message when copying/moving with server down: [#4044](https://github.com/owncloud/android/issues/4044)

   Right now, when we are trying to copy a file to another folder and the server is
   downwe receive a correct message. Before the issue the message shown code from
   the application.

   https://github.com/owncloud/android/issues/4044
   https://github.com/owncloud/android/pull/4127

* Bugfix - Unnecessary or wrong call: [#4074](https://github.com/owncloud/android/issues/4074)

   Removed added path when checking path existence.

   https://github.com/owncloud/android/issues/4074
   https://github.com/owncloud/android/pull/4131
   https://github.com/owncloud/android-library/pull/578

* Bugfix - Menu option unset av. offline shown when shouldn't: [#4077](https://github.com/owncloud/android/issues/4077)

   Unset available offline menu option is not shown in files inside an available
   offline folder anymore, because content inside an available offline folder
   cannot be changed its status, only if the folder changes it.

   https://github.com/owncloud/android/issues/4077
   https://github.com/owncloud/android/pull/4093

* Bugfix - List of accounts empty after removing all accounts and adding new ones: [#4114](https://github.com/owncloud/android/issues/4114)

   Now, the account list is shown when User opens the app and was added a new
   account.

   https://github.com/owncloud/android/issues/4114
   https://github.com/owncloud/android/pull/4122

* Bugfix - Crash when the token is expired: [#4116](https://github.com/owncloud/android/issues/4116)

   Now when the token expires and we switch from grid to list mode on the main
   screen the app doesn't crash.

   https://github.com/owncloud/android/issues/4116
   https://github.com/owncloud/android/pull/4132

* Change - Upgrade min SDK to Android 6 (API 23): [#3245](https://github.com/owncloud/android/issues/3245)

   The minimum SDK has been updated to API 23, which means that the minimum version
   of Android we'll support from now on is Android 6 Marshmallow.

   https://github.com/owncloud/android/issues/3245
   https://github.com/owncloud/android/pull/4036
   https://github.com/owncloud/android-library/pull/566

* Change - Move file menu options filter to use case: [#4009](https://github.com/owncloud/android/issues/4009)

   The old class where the menu options for a file or group or files were filtered
   has been replaced by a new use case which fits in the architecture of the app.

   https://github.com/owncloud/android/issues/4009
   https://github.com/owncloud/android/pull/4039

* Change - Gradle Version Catalog: [#4035](https://github.com/owncloud/android/pull/4035)

   Introduces the Gradle Version Catalog to manage the dependencies in a scalable
   way. Now, all the dependencies are declared inside toml file.

   https://github.com/owncloud/android/pull/4035

* Change - Remove "ignore" from the debug flavour Android manifest: [#4064](https://github.com/owncloud/android/pull/4064)

   A `tools:ignore` property from the Android manifest specific for the debug
   flavour was removed as it is not needed anymore.

   https://github.com/owncloud/android/pull/4064

* Change - Not opening browser automatically in login: [#4067](https://github.com/owncloud/android/issues/4067)

   When there is a fixed bearer auth server URL via a branded parameter, the login
   screen won't redirect automatically to the browser so that some problems in the
   authentication flow are solved.

   https://github.com/owncloud/android/issues/4067
   https://github.com/owncloud/android/pull/4106

* Change - Added new unit tests for providers: [#4073](https://github.com/owncloud/android/issues/4073)

   Implementation of tests for the functions within ScopedStorageProvider and
   OCSharedPreferencesProvider.

   https://github.com/owncloud/android/issues/4073
   https://github.com/owncloud/android/pull/4091

* Change - New detail screen file design: [#4098](https://github.com/owncloud/android/pull/4098)

   The detail view ha been improved. It added new properties like last sync, status
   icon on thumbnail, path and creation date

   https://github.com/owncloud/android/issues/4092
   https://github.com/owncloud/android/pull/4098

* Enhancement - Show "More" button for every file list item: [#2885](https://github.com/owncloud/android/issues/2885)

   A 3-dot button has been added to every file, where the options that we have in
   the 3-dot menu in multiselection for that single file have been added for a
   quicker access to them. Also, some options have been reordered.

   https://github.com/owncloud/android/issues/2885
   https://github.com/owncloud/android/pull/4076

* Enhancement - Added "Open in web" options to main file list: [#3860](https://github.com/owncloud/android/issues/3860)

   "Open in web" dynamic options (depending on the providers available) are now
   shown in the main file list as well, when selecting one single file which has
   providers to open it in web.

   https://github.com/owncloud/android/issues/3860
   https://github.com/owncloud/android/pull/4058

* Enhancement - Copy/move conflict solved by users: [#3935](https://github.com/owncloud/android/issues/3935)

   A pop-up is displayed in case there is a name conflict with the files been moved
   or copied. The pop-up has the options to Skip, Replace and Keep both, to be
   consistent with the web client.

   https://github.com/owncloud/android/issues/3935
   https://github.com/owncloud/android/pull/4062

* Enhancement - Improve grid mode: [#4027](https://github.com/owncloud/android/issues/4027)

   Grid mode has been improved to show bigger thumbnails in images files.

   https://github.com/owncloud/android/issues/4027
   https://github.com/owncloud/android/pull/4089

* Enhancement - Improve UX of creation dialog: [#4031](https://github.com/owncloud/android/issues/4031)

   Creation dialog now shows an error message and disables the confirmation button
   when forbidden characters are typed

   https://github.com/owncloud/android/issues/4031
   https://github.com/owncloud/android/pull/4097

* Enhancement - File name conflict starting by (1): [#4040](https://github.com/owncloud/android/pull/4040)

   File conflicts now are named with suffix starting in (1) instead of (2).

   https://github.com/owncloud/android/issues/3946
   https://github.com/owncloud/android/pull/4040

* Enhancement - Force security if not protected: [#4061](https://github.com/owncloud/android/issues/4061)

   A new branding parameter was created to enforce security protection in the app
   if device protection is not enabled.

   https://github.com/owncloud/android/issues/4061
   https://github.com/owncloud/android/pull/4087

* Enhancement - Prevent http traffic with branding options: [#4066](https://github.com/owncloud/android/issues/4066)

   Adding branding option for prevent http traffic.

   https://github.com/owncloud/android/issues/4066
   https://github.com/owncloud/android/pull/4110

* Enhancement - Unit tests for datasources classes - Part 2: [#4071](https://github.com/owncloud/android/issues/4071)

   Unit tests of the OCLocalFileDataSource and OCRemoteFileDataSource classes have
   been done.

   https://github.com/owncloud/android/issues/4071
   https://github.com/owncloud/android/pull/4123

* Enhancement - Respect app_providers_appsUrl value from capabilities: [#4075](https://github.com/owncloud/android/issues/4075)

   Now, the app receives the app_providers_appsUrl from the local database. Before
   of this issue, the value was hardcoded.

   https://github.com/owncloud/android/issues/4075
   https://github.com/owncloud/android/pull/4113

* Enhancement - Apply (1) to uploads' name conflicts: [#4079](https://github.com/owncloud/android/issues/4079)

   When new files were uploaded manually to pC, shared from a 3rd party app or text
   shared with oC name conflict happens, (2) was added to the file name instead of
   (1).

   Right now if we upload a file with a repeated name, the new file name will end
   with (1).

   https://github.com/owncloud/android/issues/4079
   https://github.com/owncloud/android/pull/4129

* Enhancement - Support "per app" language change on Android 13+: [#4082](https://github.com/owncloud/android/issues/4082)

   The locales_config.xml file has been created for the application to detect the
   language that the user wishes to choose.

   https://github.com/owncloud/android/issues/4082
   https://github.com/owncloud/android/pull/4099

* Enhancement - Align Sharing icons with other platforms: [#4101](https://github.com/owncloud/android/issues/4101)

   The share icon has been changed on the screens where it appears to be
   synchronized with other platforms.

   https://github.com/owncloud/android/issues/4101
   https://github.com/owncloud/android/pull/4112

# Changelog for ownCloud Android Client [4.0.0] (2023-05-29)

The following sections list the changes in ownCloud Android Client 4.0.0 relevant to
ownCloud admins and users.

[4.0.0]: https://github.com/owncloud/android/compare/v3.0.4...v4.0.0

## Summary

* Security - Make ShareActivity not-exported: [#4038](https://github.com/owncloud/android/pull/4038)
* Bugfix - Error message for protocol exception: [#3948](https://github.com/owncloud/android/issues/3948)
* Bugfix - Incorrect list of files in av. offline when browsing from details: [#3986](https://github.com/owncloud/android/issues/3986)
* Change - Bump target SDK to 33: [#3617](https://github.com/owncloud/android/issues/3617)
* Change - Use ViewBinding in FolderPickerActivity: [#3796](https://github.com/owncloud/android/issues/3796)
* Change - Use ViewBinding in WhatsNewActivity: [#3796](https://github.com/owncloud/android/issues/3796)
* Enhancement - Support for Markdown files: [#3716](https://github.com/owncloud/android/issues/3716)
* Enhancement - Support for spaces: [#3851](https://github.com/owncloud/android/pull/3851)
* Enhancement - Update label on Camera Uploads: [#3930](https://github.com/owncloud/android/pull/3930)
* Enhancement - Authenticated WebFinger: [#3943](https://github.com/owncloud/android/issues/3943)
* Enhancement - Link in drawer menu: [#3949](https://github.com/owncloud/android/pull/3949)
* Enhancement - Send language header in all requests: [#3980](https://github.com/owncloud/android/issues/3980)
* Enhancement - Open in specific web provider: [#3994](https://github.com/owncloud/android/issues/3994)
* Enhancement - Create file via web: [#3995](https://github.com/owncloud/android/issues/3995)
* Enhancement - Updated WebFinger flow: [#3998](https://github.com/owncloud/android/issues/3998)
* Enhancement - Monochrome icon for the app: [#4001](https://github.com/owncloud/android/pull/4001)
* Enhancement - Add prompt parameter to OIDC flow: [#4011](https://github.com/owncloud/android/pull/4011)
* Enhancement - New setting "Access document provider": [#4032](https://github.com/owncloud/android/pull/4032)

## Details

* Security - Make ShareActivity not-exported: [#4038](https://github.com/owncloud/android/pull/4038)

   ShareActivity was made not-exported in the manifest since this property is only
   needed for those activities that need to be launched from other external apps,
   which is not the case.

   https://github.com/owncloud/android/pull/4038

* Bugfix - Error message for protocol exception: [#3948](https://github.com/owncloud/android/issues/3948)

   Previously, when the network connection is lost while uploading a file, "Unknown
   error" was shown. Now, we show a more specific error.

   https://github.com/owncloud/android/issues/3948
   https://github.com/owncloud/android/pull/4013
   https://github.com/owncloud/android-library/pull/558

* Bugfix - Incorrect list of files in av. offline when browsing from details: [#3986](https://github.com/owncloud/android/issues/3986)

   When opening the details view of a file accessed from the available offline
   shortcut, browsing back led to a incorrect list of files. Now, browsing back
   leads to the list of available offline files again.

   https://github.com/owncloud/android/issues/3986
   https://github.com/owncloud/android/pull/4026

* Change - Bump target SDK to 33: [#3617](https://github.com/owncloud/android/issues/3617)

   Target SDK was upgraded to 33 to keep the app updated with the latest android
   changes. A new setting was introduced to manage notifications in an easier way.

   https://github.com/owncloud/android/issues/3617
   https://github.com/owncloud/android/pull/3972
   https://developer.android.com/about/versions/13/behavior-changes-13

* Change - Use ViewBinding in FolderPickerActivity: [#3796](https://github.com/owncloud/android/issues/3796)

   The use of findViewById method was replaced by using ViewBinding in the
   FolderPickerActivity.

   https://github.com/owncloud/android/issues/3796
   https://github.com/owncloud/android/pull/4014

* Change - Use ViewBinding in WhatsNewActivity: [#3796](https://github.com/owncloud/android/issues/3796)

   The use of findViewById method was replaced by using ViewBinding in the
   WhatsNewActivity.

   https://github.com/owncloud/android/issues/3796
   https://github.com/owncloud/android/pull/4021

* Enhancement - Support for Markdown files: [#3716](https://github.com/owncloud/android/issues/3716)

   Markdown files preview will now be rendered to show its content in a prettier
   way.

   https://github.com/owncloud/android/issues/3716
   https://github.com/owncloud/android/pull/4017

* Enhancement - Support for spaces: [#3851](https://github.com/owncloud/android/pull/3851)

   Spaces are now supported in oCIS accounts. A new tab has been added, which
   allows to list and browse through all the available spaces for the current
   account. The supported operations for files in spaces are: download, upload,
   remove, rename, create folder, copy and move. The documents provider has been
   adapted as well to be able to browse through spaces and perform the operations
   already mentioned.

   https://github.com/owncloud/android/pull/3851

* Enhancement - Update label on Camera Uploads: [#3930](https://github.com/owncloud/android/pull/3930)

   Update label on camera uploads to avoid confusions with the behavior of original
   files. Now, it is clear that original files will be removed.

   https://github.com/owncloud/android/pull/3930

* Enhancement - Authenticated WebFinger: [#3943](https://github.com/owncloud/android/issues/3943)

   Authenticated WebFinger was introduced into the authentication flow. Now,
   WebFinger is used to retrieve the OpenID Connect issuer and the available
   ownCloud instances. For the moment, multiple oC instances are not supported,
   only the first available instance is used.

   https://github.com/owncloud/android/issues/3943
   https://github.com/owncloud/android/pull/3945
   https://doc.owncloud.com/ocis/next/deployment/services/s-list/webfinger.html

* Enhancement - Link in drawer menu: [#3949](https://github.com/owncloud/android/pull/3949)

   Customers will be able now to set a personalized label and link that will appear
   in the drawer menu, together with the drawer logo as an icon.

   https://github.com/owncloud/android/issues/3907
   https://github.com/owncloud/android/pull/3949

* Enhancement - Send language header in all requests: [#3980](https://github.com/owncloud/android/issues/3980)

   Added Accept-Language header to all requests so the android App can receive
   translated content.

   https://github.com/owncloud/android/issues/3980
   https://github.com/owncloud/android/pull/3982
   https://github.com/owncloud/android-library/pull/551

* Enhancement - Open in specific web provider: [#3994](https://github.com/owncloud/android/issues/3994)

   We've added the specific web app providers instead of opening the file with the
   default web provider.

   The user can open their files with any of the available specific web app
   providers from the server. Previously, file was opened with the default one.

   https://github.com/owncloud/android/issues/3994
   https://github.com/owncloud/android/pull/3990
   https://owncloud.dev/services/app-registry/apps/#app-registry

* Enhancement - Create file via web: [#3995](https://github.com/owncloud/android/issues/3995)

   A new option has been added in the FAB to create new files, for those servers
   which support this option and have available app providers that allow the
   creation of new files.

   https://github.com/owncloud/android/issues/3995
   https://github.com/owncloud/android/pull/4023
   https://github.com/owncloud/android-library/pull/562

* Enhancement - Updated WebFinger flow: [#3998](https://github.com/owncloud/android/issues/3998)

   WebFinger call won't follow redirections. WebFinger will be requested first and
   will skip status.php in case it's successful, and in case the lookup server is
   not directly accessible, we will continue the authentication flow with the
   regular status.php.

   https://github.com/owncloud/android/issues/3998
   https://github.com/owncloud/android/pull/4000
   https://github.com/owncloud/android-library/pull/555

* Enhancement - Monochrome icon for the app: [#4001](https://github.com/owncloud/android/pull/4001)

   From Android 13, if the user has enabled themed app icons in their device
   settings, the app will be shown with a monochrome icon.

   https://github.com/owncloud/android/pull/4001

* Enhancement - Add prompt parameter to OIDC flow: [#4011](https://github.com/owncloud/android/pull/4011)

   Added prompt parameter to the authorization request in case OIDC is supported.
   By default, select_account will be sent. It can be changed via branding or MDM.

   https://github.com/owncloud/android/issues/3862
   https://github.com/owncloud/android/issues/3984
   https://github.com/owncloud/android/pull/4011

* Enhancement - New setting "Access document provider": [#4032](https://github.com/owncloud/android/pull/4032)

   A new setting has been added in the "More" settings section with a suggested app
   to access the document provider.

   https://github.com/owncloud/android/issues/4028
   https://github.com/owncloud/android/pull/4032

# Changelog for ownCloud Android Client [3.0.4] (2023-03-07)

The following sections list the changes in ownCloud Android Client 3.0.4 relevant to
ownCloud admins and users.

[3.0.4]: https://github.com/owncloud/android/compare/v3.0.3...v3.0.4

## Summary

* Security - Fix for security issues with database: [#3952](https://github.com/owncloud/android/pull/3952)
* Enhancement - HTTP logs show more info: [#547](https://github.com/owncloud/android-library/pull/547)

## Details

* Security - Fix for security issues with database: [#3952](https://github.com/owncloud/android/pull/3952)

   Some fixes have been added so that now no part of the app's database can be
   accessed from other apps.

   https://github.com/owncloud/android/pull/3952

* Enhancement - HTTP logs show more info: [#547](https://github.com/owncloud/android-library/pull/547)

   When enabling HTTP logs, now the URL for each log will be shown as well to make
   debugging easier.

   https://github.com/owncloud/android-library/pull/547

# Changelog for ownCloud Android Client [3.0.3] (2023-02-13)

The following sections list the changes in ownCloud Android Client 3.0.3 relevant to
ownCloud admins and users.

[3.0.3]: https://github.com/owncloud/android/compare/v3.0.2...v3.0.3

## Summary

* Bugfix - Error messages too long in folders operation: [#3852](https://github.com/owncloud/android/pull/3852)
* Bugfix - Fix problems after authentication: [#3889](https://github.com/owncloud/android/pull/3889)
* Bugfix - Toolbar in file details view: [#3899](https://github.com/owncloud/android/pull/3899)

## Details

* Bugfix - Error messages too long in folders operation: [#3852](https://github.com/owncloud/android/pull/3852)

   Error messages when trying to perform a non-allowed action for copying and
   moving folders have been shortened so that they are shown completely in the
   snackbar.

   https://github.com/owncloud/android/issues/3820
   https://github.com/owncloud/android/pull/3852

* Bugfix - Fix problems after authentication: [#3889](https://github.com/owncloud/android/pull/3889)

   Client for session are now fetched on demand to avoid reinitialize DI, making
   the process smoother

   https://github.com/owncloud/android/pull/3889

* Bugfix - Toolbar in file details view: [#3899](https://github.com/owncloud/android/pull/3899)

   When returning from the share screen to details screen, the toolbar didn't show
   the correct options and title. Now it does.

   https://github.com/owncloud/android/issues/3866
   https://github.com/owncloud/android/pull/3899

# Changelog for ownCloud Android Client [3.0.2] (2023-01-26)

The following sections list the changes in ownCloud Android Client 3.0.2 relevant to
ownCloud admins and users.

[3.0.2]: https://github.com/owncloud/android/compare/v3.0.1...v3.0.2

## Summary

* Bugfix - Fix reauthentication prompt: [#534](https://github.com/owncloud/android-library/pull/534)
* Enhancement - Branded scope for OpenID Connect: [#3869](https://github.com/owncloud/android/pull/3869)

## Details

* Bugfix - Fix reauthentication prompt: [#534](https://github.com/owncloud/android-library/pull/534)

   Potential fix to oauth error after logging in for first time that makes user to
   reauthenticate

   https://github.com/owncloud/android-library/pull/534

* Enhancement - Branded scope for OpenID Connect: [#3869](https://github.com/owncloud/android/pull/3869)

   OpenID Connect scope is now brandable via setup.xml file or MDM

   https://github.com/owncloud/android/pull/3869

# Changelog for ownCloud Android Client [3.0.1] (2022-12-21)

The following sections list the changes in ownCloud Android Client 3.0.1 relevant to
ownCloud admins and users.

[3.0.1]: https://github.com/owncloud/android/compare/v3.0.0...v3.0.1

## Summary

* Bugfix - Fix crash when upgrading from 2.18: [#3837](https://github.com/owncloud/android/pull/3837)
* Bugfix - Fix crash when opening uploads section: [#3841](https://github.com/owncloud/android/pull/3841)

## Details

* Bugfix - Fix crash when upgrading from 2.18: [#3837](https://github.com/owncloud/android/pull/3837)

   Upgrading from 2.18 or older versions made the app crash due to camera uploads
   data migration. This problem has been solved and now the app upgrades correctly.

   https://github.com/owncloud/android/pull/3837

* Bugfix - Fix crash when opening uploads section: [#3841](https://github.com/owncloud/android/pull/3841)

   When upgrading from an old version with uploads with "forget" behaviour, app
   crashed when opening the uploads tab. Now, this has been fixed so that it works
   correctly.

   https://github.com/owncloud/android/pull/3841

# Changelog for ownCloud Android Client [3.0.0] (2022-12-12)

The following sections list the changes in ownCloud Android Client 3.0.0 relevant to
ownCloud admins and users.

[3.0.0]: https://github.com/owncloud/android/compare/v2.21.2...v3.0.0

## Summary

* Bugfix - Fix for thumbnails: [#3719](https://github.com/owncloud/android/pull/3719)
* Enhancement - Sync engine rewritten: [#2934](https://github.com/owncloud/android/pull/2934)
* Enhancement - Faster browser authentication: [#3632](https://github.com/owncloud/android/pull/3632)
* Enhancement - Several transfers running simultaneously: [#3710](https://github.com/owncloud/android/pull/3710)
* Enhancement - Empty views improved: [#3728](https://github.com/owncloud/android/pull/3728)
* Enhancement - Automatic conflicts propagation: [#3766](https://github.com/owncloud/android/pull/3766)

## Details

* Bugfix - Fix for thumbnails: [#3719](https://github.com/owncloud/android/pull/3719)

   Some thumbnails were not shown in the file list. Now, they are all shown
   correctly.

   https://github.com/owncloud/android/issues/2818
   https://github.com/owncloud/android/pull/3719

* Enhancement - Sync engine rewritten: [#2934](https://github.com/owncloud/android/pull/2934)

   The whole synchronization engine has been refactored to a new architecture to
   make it better structured and more efficient.

   https://github.com/owncloud/android/issues/2818
   https://github.com/owncloud/android/pull/2934

* Enhancement - Faster browser authentication: [#3632](https://github.com/owncloud/android/pull/3632)

   Login flow has been improved by saving a click when the server is OAuth2/OIDC
   and it is valid. Also, when authenticating again in a OAuth2/OIDC account
   already saved in the app, the username is already shown in the browser.

   https://github.com/owncloud/android/issues/3759
   https://github.com/owncloud/android/pull/3632

* Enhancement - Several transfers running simultaneously: [#3710](https://github.com/owncloud/android/pull/3710)

   With the sync engine refactor, now several downloads and uploads can run at the
   same time, improving efficiency.

   https://github.com/owncloud/android/issues/3426
   https://github.com/owncloud/android/pull/3710

* Enhancement - Empty views improved: [#3728](https://github.com/owncloud/android/pull/3728)

   When the list of items is empty, we now show a more attractive view. This
   applies to file list, available offline list, shared by link list, uploads list,
   logs list and external share list.

   https://github.com/owncloud/android/issues/3026
   https://github.com/owncloud/android/pull/3728

* Enhancement - Automatic conflicts propagation: [#3766](https://github.com/owncloud/android/pull/3766)

   Conflicts are now propagated automatically to parent folders, and cleaned when
   solved or removed. Before, it was needed to navigate to the file location for
   the conflict to propagate. Also, move, copy and remove actions work properly
   with conflicts.

   https://github.com/owncloud/android/issues/3005
   https://github.com/owncloud/android/pull/3766

# Changelog for ownCloud Android Client [2.21.2] (2022-09-07)

The following sections list the changes in ownCloud Android Client 2.21.2 relevant to
ownCloud admins and users.

[2.21.2]: https://github.com/owncloud/android/compare/v2.21.1...v2.21.2

## Summary

* Enhancement - Open in web: [#3672](https://github.com/owncloud/android/issues/3672)
* Enhancement - Shares from propfind: [#3711](https://github.com/owncloud/android/issues/3711)
* Enhancement - Private link capability: [#3732](https://github.com/owncloud/android/issues/3732)

## Details

* Enhancement - Open in web: [#3672](https://github.com/owncloud/android/issues/3672)

   OCIS feature, to open files with mime types supported by the server in the web
   browser using collaborative or specific tools

   https://github.com/owncloud/android/issues/3672
   https://github.com/owncloud/android/pull/3737

* Enhancement - Shares from propfind: [#3711](https://github.com/owncloud/android/issues/3711)

   Added a new property to the propfind, so that, we can get if the files in a
   folder are shared directly with just one request. Previously, a propfind and
   another additional request were needed to the shares api to retrieve the shares
   of the folder.

   https://github.com/owncloud/android/issues/3711
   https://github.com/owncloud/android-library/pull/496

* Enhancement - Private link capability: [#3732](https://github.com/owncloud/android/issues/3732)

   Private link capability is now respected. Option is shown/hidden depending on
   its value

   https://github.com/owncloud/android/issues/3732
   https://github.com/owncloud/android/pull/3738
   https://github.com/owncloud/android-library/pull/505

# Changelog for ownCloud Android Client [2.21.1] (2022-06-15)

The following sections list the changes in ownCloud Android Client 2.21.1 relevant to
ownCloud admins and users.

[2.21.1]: https://github.com/owncloud/android/compare/v2.21.0...v2.21.1

## Summary

* Bugfix - Fix crash when opening from details screen: [#3696](https://github.com/owncloud/android/pull/3696)

## Details

* Bugfix - Fix crash when opening from details screen: [#3696](https://github.com/owncloud/android/pull/3696)

   Fixed a crash when opening a non downloaded file from the details view.

   https://github.com/owncloud/android/pull/3696

# Changelog for ownCloud Android Client [2.21.0] (2022-06-07)

The following sections list the changes in ownCloud Android Client 2.21.0 relevant to
ownCloud admins and users.

[2.21.0]: https://github.com/owncloud/android/compare/v2.20.0...v2.21.0

## Summary

* Bugfix - Prevented signed in user in the list of users to be shared: [#1419](https://github.com/owncloud/android/issues/1419)
* Bugfix - Corrupt picture error controlled: [#3441](https://github.com/owncloud/android/issues/3441)
* Bugfix - Security flags for recording screen: [#3468](https://github.com/owncloud/android/issues/3468)
* Bugfix - Crash when changing orientation in Details view: [#3571](https://github.com/owncloud/android/issues/3571)
* Bugfix - Lock displays shown again: [#3591](https://github.com/owncloud/android/issues/3591)
* Enhancement - Support for SVG files added: [#1033](https://github.com/owncloud/android/issues/1033)
* Enhancement - Full name is shown in shares: [#1106](https://github.com/owncloud/android/issues/1106)
* Enhancement - Improved copy/move dialog: [#1414](https://github.com/owncloud/android/issues/1414)
* Enhancement - Share a folder from within the folder: [#1441](https://github.com/owncloud/android/issues/1441)
* Enhancement - New option to show or not hidden files: [#2578](https://github.com/owncloud/android/issues/2578)
* Enhancement - What´s new option: [#3352](https://github.com/owncloud/android/issues/3352)
* Enhancement - First steps in Android Enterprise integration: [#3415](https://github.com/owncloud/android/issues/3415)
* Enhancement - Provide app feedback to MDM admins: [#3420](https://github.com/owncloud/android/issues/3420)
* Enhancement - Lock delay enforced: [#3440](https://github.com/owncloud/android/issues/3440)
* Enhancement - Release Notes: [#3442](https://github.com/owncloud/android/issues/3442)
* Enhancement - Send for file multiselect: [#3491](https://github.com/owncloud/android/issues/3491)
* Enhancement - Improvements for the UI in the passcode screen: [#3516](https://github.com/owncloud/android/issues/3516)
* Enhancement - Extended security enforced: [#3543](https://github.com/owncloud/android/issues/3543)
* Enhancement - Improvements for the UI in the pattern screen: [#3580](https://github.com/owncloud/android/issues/3580)
* Enhancement - Prevent taking screenshots: [#3596](https://github.com/owncloud/android/issues/3596)
* Enhancement - Option to allow screenshots or not in Android Enterprise: [#3625](https://github.com/owncloud/android/issues/3625)
* Enhancement - Thumbnail click action in file detail: [#3653](https://github.com/owncloud/android/pull/3653)

## Details

* Bugfix - Prevented signed in user in the list of users to be shared: [#1419](https://github.com/owncloud/android/issues/1419)

   Previously, user list for sharing contains signed in user, now this user is
   omitted to avoid errors.

   https://github.com/owncloud/android/issues/1419
   https://github.com/owncloud/android/pull/3643

* Bugfix - Corrupt picture error controlled: [#3441](https://github.com/owncloud/android/issues/3441)

   Previously, If a file is not correct or is damaged, it is downloaded but not
   previewed. An infinite spinner on a black window is shown instead. Now, an error
   appears warning to the user.

   https://github.com/owncloud/android/issues/3441
   https://github.com/owncloud/android/pull/3644

* Bugfix - Security flags for recording screen: [#3468](https://github.com/owncloud/android/issues/3468)

   Previously, if passcode or pattern were enabled, no screen from the app could be
   viewed from a recording screen app. Now, only the login, passcode and pattern
   screens are protected against recording.

   https://github.com/owncloud/android/issues/3468
   https://github.com/owncloud/android/pull/3560

* Bugfix - Crash when changing orientation in Details view: [#3571](https://github.com/owncloud/android/issues/3571)

   Previously, the app crashes when changing orientation in Details view after
   installing Now, app shows correctly the details after installing.

   https://github.com/owncloud/android/issues/3571
   https://github.com/owncloud/android/pull/3589

* Bugfix - Lock displays shown again: [#3591](https://github.com/owncloud/android/issues/3591)

   Previously, if you clicked on passcode or pattern lock to remove it, and then
   you clicked on cancel, the lock display was shown again to put the passcode or
   pattern. Now, if you cancel it, you come back to settings screen.

   https://github.com/owncloud/android/issues/3591
   https://github.com/owncloud/android/pull/3592

* Enhancement - Support for SVG files added: [#1033](https://github.com/owncloud/android/issues/1033)

   SVG files are supported and can be downloaded and viewed.

   https://github.com/owncloud/android/issues/1033
   https://github.com/owncloud/android/pull/3639

* Enhancement - Full name is shown in shares: [#1106](https://github.com/owncloud/android/issues/1106)

   Full name is shown when using public share instead of username.

   https://github.com/owncloud/android/issues/1106
   https://github.com/owncloud/android/pull/3636

* Enhancement - Improved copy/move dialog: [#1414](https://github.com/owncloud/android/issues/1414)

   Previously,they appeared exactly the same and there was no way of knowing which
   was which. Now they are differentiated by the text on the action button.

   https://github.com/owncloud/android/issues/1414
   https://github.com/owncloud/android/pull/3640

* Enhancement - Share a folder from within the folder: [#1441](https://github.com/owncloud/android/issues/1441)

   You can share a folder clicking in the share icon inside the folder.

   https://github.com/owncloud/android/issues/1441
   https://github.com/owncloud/android/pull/3659

* Enhancement - New option to show or not hidden files: [#2578](https://github.com/owncloud/android/issues/2578)

   Enable it to show hidden files and folders

   https://github.com/owncloud/android/issues/2578
   https://github.com/owncloud/android/pull/3624

* Enhancement - What´s new option: [#3352](https://github.com/owncloud/android/issues/3352)

   New option to check what was included in the latest version.

   https://github.com/owncloud/android/issues/3352
   https://github.com/owncloud/android/pull/3616

* Enhancement - First steps in Android Enterprise integration: [#3415](https://github.com/owncloud/android/issues/3415)

   Two parameters (server url and server url input visibility) can be now managed
   via MDM. These were the first parameters used to test integration with Android
   Enterprise and Android Management API.

   https://github.com/owncloud/android/issues/3415
   https://github.com/owncloud/android/pull/3419

* Enhancement - Provide app feedback to MDM admins: [#3420](https://github.com/owncloud/android/issues/3420)

   Now, when a MDM configuration is applied for the first time or changed by an IT
   administrator, the app sends feedback that will be shown in the EMM console.

   https://github.com/owncloud/android/issues/3420
   https://github.com/owncloud/android/pull/3480

* Enhancement - Lock delay enforced: [#3440](https://github.com/owncloud/android/issues/3440)

   A new local setup's option has been added for the application to lock after the
   selected interval

   https://github.com/owncloud/android/issues/3440
   https://github.com/owncloud/android/pull/3547

* Enhancement - Release Notes: [#3442](https://github.com/owncloud/android/issues/3442)

   New release notes to show news in updates.

   https://github.com/owncloud/android/issues/3442
   https://github.com/owncloud/android/pull/3594

* Enhancement - Send for file multiselect: [#3491](https://github.com/owncloud/android/issues/3491)

   Send multiple files at once if they are downloaded.

   https://github.com/owncloud/android/issues/3491
   https://github.com/owncloud/android/pull/3638

* Enhancement - Improvements for the UI in the passcode screen: [#3516](https://github.com/owncloud/android/issues/3516)

   Redesign of the passcode screen to have the numeric keyboard in the screen
   instead of using the Android one.

   https://github.com/owncloud/android/issues/3516
   https://github.com/owncloud/android/pull/3582

* Enhancement - Extended security enforced: [#3543](https://github.com/owncloud/android/issues/3543)

   New extended branding options have been added to make app lock via passcode or
   pattern compulsory.

   https://github.com/owncloud/android/issues/3543
   https://github.com/owncloud/android/pull/3544

* Enhancement - Improvements for the UI in the pattern screen: [#3580](https://github.com/owncloud/android/issues/3580)

   Redesign of the pattern screen. Cancel button deleted and new back arrow in the
   toolbar.

   https://github.com/owncloud/android/issues/3580
   https://github.com/owncloud/android/pull/3587

* Enhancement - Prevent taking screenshots: [#3596](https://github.com/owncloud/android/issues/3596)

   New option to prevent taking screenshots.

   https://github.com/owncloud/android/issues/3596
   https://github.com/owncloud/android/pull/3615

* Enhancement - Option to allow screenshots or not in Android Enterprise: [#3625](https://github.com/owncloud/android/issues/3625)

   New parameter to manage screenshots can be configured via MDM.

   https://github.com/owncloud/android/issues/3625
   https://github.com/owncloud/android/pull/3627

* Enhancement - Thumbnail click action in file detail: [#3653](https://github.com/owncloud/android/pull/3653)

   When a user clicks on a file's detail view thumbnail, the file is automatically
   downloaded and previewed.

   https://github.com/owncloud/android/pull/3653

# Changelog for ownCloud Android Client [2.20.0] (2022-02-16)

The following sections list the changes in ownCloud Android Client 2.20.0 relevant to
ownCloud admins and users.

[2.20.0]: https://github.com/owncloud/android/compare/v2.19.0...v2.20.0

## Summary

* Bugfix - Small glitch when side menu is full of accounts: [#3437](https://github.com/owncloud/android/pull/3437)
* Bugfix - Small bug when privacy policy disabled: [#3542](https://github.com/owncloud/android/pull/3542)
* Enhancement - Permission dialog removal: [#2524](https://github.com/owncloud/android/pull/2524)
* Enhancement - Brute force protection: [#3320](https://github.com/owncloud/android/issues/3320)
* Enhancement - Lock delay for app: [#3344](https://github.com/owncloud/android/issues/3344)
* Enhancement - Allow access from document provider preference: [#3379](https://github.com/owncloud/android/issues/3379)
* Enhancement - Security enforced: [#3434](https://github.com/owncloud/android/pull/3434)
* Enhancement - Respect capability for Avatar support: [#3438](https://github.com/owncloud/android/pull/3438)
* Enhancement - "Open with" action now allows editing: [#3475](https://github.com/owncloud/android/issues/3475)
* Enhancement - Enable logs by default in debug mode: [#3526](https://github.com/owncloud/android/issues/3526)
* Enhancement - Suggest the user to enable enhanced security: [#3539](https://github.com/owncloud/android/pull/3539)

## Details

* Bugfix - Small glitch when side menu is full of accounts: [#3437](https://github.com/owncloud/android/pull/3437)

   Previously, when users set up a large number of accounts, the side menu
   overlapped the available space quota. Now, everything is contained within a
   scroll to avoid this.

   https://github.com/owncloud/android/issues/3060
   https://github.com/owncloud/android/pull/3437

* Bugfix - Small bug when privacy policy disabled: [#3542](https://github.com/owncloud/android/pull/3542)

   Previously, when privacy policy setup was disabled, the side menu showed the
   privacy policy menu item. Now, option is hidden when privacy policy is disabled.

   https://github.com/owncloud/android/issues/3521
   https://github.com/owncloud/android/pull/3542

* Enhancement - Permission dialog removal: [#2524](https://github.com/owncloud/android/pull/2524)

   The old permission request dialog has been removed. It was not needed after
   migrating the storage to scoped storage, read and write permissions are
   guaranteed in our scoped storage.

   https://github.com/owncloud/android/pull/2524

* Enhancement - Brute force protection: [#3320](https://github.com/owncloud/android/issues/3320)

   Previously, when setting passcode lock, an unlimited number of attempts to
   unlock the app could be done in a row. Now, from the third incorrect attempt,
   there will be an exponential growing waiting time until next unlock attempt.

   https://github.com/owncloud/android/issues/3320
   https://github.com/owncloud/android/pull/3463

* Enhancement - Lock delay for app: [#3344](https://github.com/owncloud/android/issues/3344)

   A new preference has been added to choose the interval in which the app will be
   unlocked after having unlocked it once, making it more comfortable for those who
   access the app frequently and have a security lock set.

   https://github.com/owncloud/android/issues/3344
   https://github.com/owncloud/android/pull/3375

* Enhancement - Allow access from document provider preference: [#3379](https://github.com/owncloud/android/issues/3379)

   Previously, files of ownCloud accounts couldn't be accessed via documents
   provider when there was a lock set in the app. Now, a new preference has been
   added to allow/disallow the access, so users have more control over their files.

   https://github.com/owncloud/android/issues/3379
   https://github.com/owncloud/android/issues/3520
   https://github.com/owncloud/android/pull/3384
   https://github.com/owncloud/android/pull/3538

* Enhancement - Security enforced: [#3434](https://github.com/owncloud/android/pull/3434)

   A new branding/MDM option has been added to make app lock via passcode or
   pattern compulsory, whichever the user chooses.

   https://github.com/owncloud/android/issues/3400
   https://github.com/owncloud/android/pull/3434

* Enhancement - Respect capability for Avatar support: [#3438](https://github.com/owncloud/android/pull/3438)

   Previously, the user's avatar was shown by default. Now, it is shown or not
   depending on a new capability.

   https://github.com/owncloud/android/issues/3285
   https://github.com/owncloud/android/pull/3438

* Enhancement - "Open with" action now allows editing: [#3475](https://github.com/owncloud/android/issues/3475)

   Previously, when a document file was opened and edited with an external app,
   changes weren't saved because it didn't synchronized with the server. Now, when
   you edit a document and navigate or refresh in the ownCloud app, it synchronizes
   automatically, keeping consistence of your files.

   https://github.com/owncloud/android/issues/3475
   https://github.com/owncloud/android/pull/3499

* Enhancement - Enable logs by default in debug mode: [#3526](https://github.com/owncloud/android/issues/3526)

   Now, when the app is built in DEBUG mode, the logs are enabled by default.

   https://github.com/owncloud/android/issues/3526
   https://github.com/owncloud/android/pull/3527

* Enhancement - Suggest the user to enable enhanced security: [#3539](https://github.com/owncloud/android/pull/3539)

   When a user sets the passcode or pattern lock on the security screen, the
   application suggests the user whether to enable or not a biometric lock to
   unlock the application.

   https://github.com/owncloud/android/pull/3539

# Changelog for ownCloud Android Client [2.19.0] (2021-11-15)

The following sections list the changes in ownCloud Android Client 2.19.0 relevant to
ownCloud admins and users.

[2.19.0]: https://github.com/owncloud/android/compare/v2.18.3...v2.19.0

## Summary

* Bugfix - Crash in FileDataStorageManager: [#2896](https://github.com/owncloud/android/issues/2896)
* Bugfix - Account removed is not removed from the drawer: [#3340](https://github.com/owncloud/android/issues/3340)
* Bugfix - Passcode input misbehaving: [#3342](https://github.com/owncloud/android/issues/3342)
* Bugfix - Lack of back button in Logs view: [#3357](https://github.com/owncloud/android/issues/3357)
* Bugfix - ANR after removing account with too many downloaded files: [#3362](https://github.com/owncloud/android/issues/3362)
* Bugfix - Camera Upload manual retry: [#3418](https://github.com/owncloud/android/pull/3418)
* Bugfix - Device rotation moves to root in folder picker: [#3431](https://github.com/owncloud/android/pull/3431)
* Bugfix - Logging does not stop when the user deactivates it: [#3436](https://github.com/owncloud/android/pull/3436)
* Enhancement - Instant upload only when charging: [#465](https://github.com/owncloud/android/issues/465)
* Enhancement - Scoped Storage: [#2877](https://github.com/owncloud/android/issues/2877)
* Enhancement - Delete old logs every week: [#3328](https://github.com/owncloud/android/issues/3328)
* Enhancement - New Logging Screen 2.0: [#3333](https://github.com/owncloud/android/issues/3333)
* Enhancement - Delete old user directories in order to free memory: [#3336](https://github.com/owncloud/android/pull/3336)

## Details

* Bugfix - Crash in FileDataStorageManager: [#2896](https://github.com/owncloud/android/issues/2896)

   A possible null value with the account that caused certain crashes on Android 10
   devices has been controlled.

   https://github.com/owncloud/android/issues/2896
   https://github.com/owncloud/android/pull/3383

* Bugfix - Account removed is not removed from the drawer: [#3340](https://github.com/owncloud/android/issues/3340)

   When an account was deleted from the device settings, in the accounts section,
   it was not removed from the Navigation Drawer. Now, when deleting an account
   from there, the Navigation Drawer is refreshed and the removed account is no
   more shown.

   https://github.com/owncloud/android/issues/3340
   https://github.com/owncloud/android/pull/3381

* Bugfix - Passcode input misbehaving: [#3342](https://github.com/owncloud/android/issues/3342)

   Passcode text fields have been made not selectable once a number is written on
   them, so that we avoid bugs with the digits of the passcode and the way of
   entering them.

   https://github.com/owncloud/android/issues/3342
   https://github.com/owncloud/android/pull/3365

* Bugfix - Lack of back button in Logs view: [#3357](https://github.com/owncloud/android/issues/3357)

   A new back arrow button has been added in the toolbar in Logs screen, so that
   now it's possible to return to the settings screen without the use of physical
   buttons of the device.

   https://github.com/owncloud/android/issues/3357
   https://github.com/owncloud/android/pull/3363

* Bugfix - ANR after removing account with too many downloaded files: [#3362](https://github.com/owncloud/android/issues/3362)

   Previously, when a user account was deleted, the application could freeze when
   trying to delete a large number of files. Now, the application has been fixed so
   that it doesn't freeze anymore by doing this.

   https://github.com/owncloud/android/issues/3362
   https://github.com/owncloud/android/pull/3380

* Bugfix - Camera Upload manual retry: [#3418](https://github.com/owncloud/android/pull/3418)

   Previously, when users selected to retry a single camera upload, an error
   message appeared. Now, the retry of a single upload is enqueued again as
   expected.

   https://github.com/owncloud/android/issues/3417
   https://github.com/owncloud/android/pull/3418

* Bugfix - Device rotation moves to root in folder picker: [#3431](https://github.com/owncloud/android/pull/3431)

   Previously, when users rotate the device trying to share photos with oC
   selecting a non-root folder, folder picker shows the root folder Now, folder
   picker shows the folder that the user browsed.

   https://github.com/owncloud/android/issues/3163
   https://github.com/owncloud/android/pull/3431

* Bugfix - Logging does not stop when the user deactivates it: [#3436](https://github.com/owncloud/android/pull/3436)

   Previously, when users disabled the logging option in the settings, the
   application would not stop logging and the size of the log files would increase.
   Now, the option to disable it works perfectly and no logs are collected if
   disabled.

   https://github.com/owncloud/android/issues/3325
   https://github.com/owncloud/android/pull/3436

* Enhancement - Instant upload only when charging: [#465](https://github.com/owncloud/android/issues/465)

   A new option has been added in the auto upload pictures/videos screen, so that
   now it's possible to upload pictures or videos only when charging.

   https://github.com/owncloud/android/issues/465
   https://github.com/owncloud/android/issues/3315
   https://github.com/owncloud/android/pull/3385

* Enhancement - Scoped Storage: [#2877](https://github.com/owncloud/android/issues/2877)

   The way to store files in the device has changed completely. Previously, the
   files were stored in the shared storage. That means that apps that had access to
   the shared storage, could read, write or do whatever they wanted with the
   ownCloud files.

   Now, ownCloud files are stored in the Scoped Storage, so they are safer. Other
   apps can access ownCloud files using the Documents Provider, which is the native
   way to do it, and that means that the ownCloud app has full control of its
   files.

   Furthermore, if the app is removed, the files downloaded to ownCloud are removed
   too. So, files are not lost or forgotten in the device after uninstalling the
   app.

   https://github.com/owncloud/android/issues/2877
   https://github.com/owncloud/android/pull/3269

* Enhancement - Delete old logs every week: [#3328](https://github.com/owncloud/android/issues/3328)

   Previously, logs were stored but never deleted. It used a lot of storage when
   logs were enabled for some time. Now, the logs are removed periodically every
   week.

   https://github.com/owncloud/android/issues/3328
   https://github.com/owncloud/android/pull/3337

* Enhancement - New Logging Screen 2.0: [#3333](https://github.com/owncloud/android/issues/3333)

   A new option has been added to the logging screen, so that now it's possible to
   share/delete log files or open them.

   https://github.com/owncloud/android/issues/3333
   https://github.com/owncloud/android/pull/3408

* Enhancement - Delete old user directories in order to free memory: [#3336](https://github.com/owncloud/android/pull/3336)

   Previously, when users deleted an account the synchronized files of this account
   stayed on the SD-Card. So if the user didn't want them anymore he had to delete
   them manually. Now, the app automatically removes the files associated with an
   account.

   https://github.com/owncloud/android/issues/125
   https://github.com/owncloud/android/pull/3336

# Changelog for ownCloud Android Client [2.18.3] (2021-10-27)

The following sections list the changes in ownCloud Android Client 2.18.3 relevant to
ownCloud admins and users.

[2.18.3]: https://github.com/owncloud/android/compare/v2.18.1...v2.18.3

## Summary

* Enhancement - Privacy policy button more accessible: [#3423](https://github.com/owncloud/android/pull/3423)

## Details

* Enhancement - Privacy policy button more accessible: [#3423](https://github.com/owncloud/android/pull/3423)

   The privacy policy button has been removed from "More" settings section, and it
   has been added to general settings screen as well as to the drawer menu, so that
   it is easier and more accessible for users.

   https://github.com/owncloud/android/issues/3422
   https://github.com/owncloud/android/pull/3423

# Changelog for ownCloud Android Client [2.18.1] (2021-07-20)

The following sections list the changes in ownCloud Android Client 2.18.1 relevant to
ownCloud admins and users.

[2.18.1]: https://github.com/owncloud/android/compare/v2.18.0...v2.18.1

## Summary

* Security - Add PKCE support: [#3310](https://github.com/owncloud/android/pull/3310)
* Enhancement - Replace picker to select camera folder with native one: [#2899](https://github.com/owncloud/android/issues/2899)
* Enhancement - Hide "More" section if all options are disabled: [#3271](https://github.com/owncloud/android/issues/3271)
* Enhancement - Note icon in music player to be branded: [#3272](https://github.com/owncloud/android/issues/3272)

## Details

* Security - Add PKCE support: [#3310](https://github.com/owncloud/android/pull/3310)

   PKCE (Proof Key for Code Exchange) support defined in RFC-7636 was added to
   prevent authorization code interception attacks.

   https://github.com/owncloud/android/pull/3310

* Enhancement - Replace picker to select camera folder with native one: [#2899](https://github.com/owncloud/android/issues/2899)

   The custom picker to select the camera folder was replaced with the native one.
   Now, it is ready for scoped storage and some problems to select a folder in the
   SD Card were fixed. Also, a new field to show the last synchronization timestamp
   was added.

   https://github.com/owncloud/android/issues/2899
   https://github.com/owncloud/android/pull/3293

* Enhancement - Hide "More" section if all options are disabled: [#3271](https://github.com/owncloud/android/issues/3271)

   A blank view was shown when all options in "More" subsection were disabled. Now,
   the subsection is only shown if at least one option is enabled.

   https://github.com/owncloud/android/issues/3271
   https://github.com/owncloud/android/pull/3296

* Enhancement - Note icon in music player to be branded: [#3272](https://github.com/owncloud/android/issues/3272)

   The note icon in the music player will have the same color as the toolbar, so
   branded apps can have the icon tinted using their custom theme.

   https://github.com/owncloud/android/issues/3272
   https://github.com/owncloud/android/pull/3297

# Changelog for ownCloud Android Client [2.18.0] (2021-05-24)

The following sections list the changes in ownCloud Android Client 2.18.0 relevant to
ownCloud admins and users.



## Summary

* Bugfix - Snackbar in passcode view is not displayed: [#2722](https://github.com/owncloud/android/issues/2722)
* Bugfix - Fixed problem when a file is edited externally: [#2752](https://github.com/owncloud/android/issues/2752)
* Bugfix - Fix navbar is visible in file preview screen after rotation: [#3184](https://github.com/owncloud/android/pull/3184)
* Bugfix - Fix a bug when some fields where not retrieved from OIDC Discovery: [#3202](https://github.com/owncloud/android/pull/3202)
* Bugfix - Fix permissions were displayed in share creation view after rotation: [#3204](https://github.com/owncloud/android/issues/3204)
* Change - Error handling for pattern lock: [#3215](https://github.com/owncloud/android/issues/3215)
* Change - Hide biometrical if device does not support it: [#3217](https://github.com/owncloud/android/issues/3217)
* Enhancement - Settings accessible even when no account is attached: [#2638](https://github.com/owncloud/android/issues/2638)
* Enhancement - Support for apk files: [#2691](https://github.com/owncloud/android/issues/2691)
* Enhancement - Move to AndroidX Preference and new structure for settings: [#2867](https://github.com/owncloud/android/issues/2867)
* Enhancement - Replace blank view in music player with cover art: [#3121](https://github.com/owncloud/android/issues/3121)
* Enhancement - Align previews actions: [#3155](https://github.com/owncloud/android/issues/3155)
* Enhancement - Fixed account for camera uploads: [#3166](https://github.com/owncloud/android/issues/3166)

## Details

* Bugfix - Snackbar in passcode view is not displayed: [#2722](https://github.com/owncloud/android/issues/2722)

   Snackbar telling about an error in a failed enter or reenter of the passcode
   wasn't visible. Now, the message is shown in a text just below the passcode
   input.

   https://github.com/owncloud/android/issues/2722
   https://github.com/owncloud/android/pull/3210

* Bugfix - Fixed problem when a file is edited externally: [#2752](https://github.com/owncloud/android/issues/2752)

   If an external editor modifies a file, the new size will not match when it is
   assembled in server side. Fixed by removing the if-match header from the proper
   place

   https://github.com/owncloud/android/issues/2752
   https://github.com/owncloud/android/pull/3220

* Bugfix - Fix navbar is visible in file preview screen after rotation: [#3184](https://github.com/owncloud/android/pull/3184)

   Glitch was fixed where the navigation bar became visible in a file preview
   screen when rotating the device.

   https://github.com/owncloud/android/issues/3139
   https://github.com/owncloud/android/pull/3184

* Bugfix - Fix a bug when some fields where not retrieved from OIDC Discovery: [#3202](https://github.com/owncloud/android/pull/3202)

   Problem when requesting the OIDC discovery was fixed. Some fields were handled
   as mandatory, but they are recommended according to the docs. It prevented from
   a proper login. Now it is possible to login as expected when some fields are not
   retrieved.

   https://github.com/owncloud/android/pull/3202
   https://github.com/owncloud/android-library/pull/392

* Bugfix - Fix permissions were displayed in share creation view after rotation: [#3204](https://github.com/owncloud/android/issues/3204)

   Permissions view was shown when creating a share for a file after rotation.
   Capabilities were taken into account just once. Now, the permissions view is
   shown only when capabilities match.

   https://github.com/owncloud/android/issues/3204
   https://github.com/owncloud/android/pull/3234

* Change - Error handling for pattern lock: [#3215](https://github.com/owncloud/android/issues/3215)

   Error messages when an incorrect pattern was entered were shown in a snackbar.
   Now, they are displayed in a text below the pattern input, just like in the
   passcode screen.

   https://github.com/owncloud/android/issues/3215
   https://github.com/owncloud/android/pull/3221

* Change - Hide biometrical if device does not support it: [#3217](https://github.com/owncloud/android/issues/3217)

   Biometric lock preference in "Security" settings subsection was shown even when
   the device didn't support biometrics (if it was Android 6.0 or later versions).
   Now, the preference is only shown if the device has the suitable hardware for
   it.

   https://github.com/owncloud/android/issues/3217
   https://github.com/owncloud/android/pull/3230

* Enhancement - Settings accessible even when no account is attached: [#2638](https://github.com/owncloud/android/issues/2638)

   Now, settings can be accessed via a button in the login screen, removing the
   necessity to have an attached account. However, auto picture and video uploads
   won't be available until an account is registered in the app.

   https://github.com/owncloud/android/issues/2638
   https://github.com/owncloud/android/pull/3218

* Enhancement - Support for apk files: [#2691](https://github.com/owncloud/android/issues/2691)

   Apk files could be installed from the app after being downloaded. Installation
   process will be triggered by the system.

   https://github.com/owncloud/android/issues/2691
   https://github.com/owncloud/android/pull/3156
   https://github.com/owncloud/android/pull/3162

* Enhancement - Move to AndroidX Preference and new structure for settings: [#2867](https://github.com/owncloud/android/issues/2867)

   Settings have been updated to use the current Android's recommendation, AndroidX
   framework. In addition, they have been reorganized into subsections for a better
   understanding and navigation structure. Also, new features have been added: now,
   source path and behaviour in auto uploads can be chosen differently for pictures
   and videos.

   https://github.com/owncloud/android/issues/2867
   https://github.com/owncloud/android/pull/3143

* Enhancement - Replace blank view in music player with cover art: [#3121](https://github.com/owncloud/android/issues/3121)

   Blank view in the music preview player with styled up cover art was replaced.
   For music files that does not have cover art embodied, it is displayed a
   placeholder.

   https://github.com/owncloud/android/issues/3121
   https://github.com/owncloud/android/pull/3182

* Enhancement - Align previews actions: [#3155](https://github.com/owncloud/android/issues/3155)

   Behaviour was aligned through every preview fragment. Images, videos, audios and
   texts show the same actions now.

   https://github.com/owncloud/android/issues/3155
   https://github.com/owncloud/android/pull/3177

* Enhancement - Fixed account for camera uploads: [#3166](https://github.com/owncloud/android/issues/3166)

   Camera uploads will be uploaded to a fixed account independently of the current
   account. Removing the account attached to camera uploads will disable this
   feature. User will be warned when removing an account that has camera uploads
   attached.

   https://github.com/owncloud/android/issues/3166
   https://github.com/owncloud/android/pull/3226

# Changelog for 2.17 versions and below

## 2.17 (March 2021)
- Toolbar redesign
- Show thumbnails for every supported file type
- Fix 301 redirections
- Fix a crash related to pictures preview
- Fix two bugs when sharing files with ownCloud
- Improvements in OAuth2, including
  + Fix a crash when migrating from OAuth2 to OIDC
  + Fix a crash when disabling OAuth2
  + Fix a bug where token was not refreshed properly
  + Log authentication requests
  + Support OIDC Dynamic Client Registration

## 2.17 beta v1 (March 2021)
- Toolbar redesign
- Show thumbnails for every supported file type
- Fix 301 redirections
- Fix a crash related to pictures preview
- Fix a bug when sharing files with ownCloud
- Improvements in OAuth2, including
  + Fix a crash when migrating from OAuth2 to OIDC
  + Fix a crash when disabling OAuth2
  + Fix a bug where token was not refreshed properly
  + Log authentication requests
  + Support OIDC Dynamic Client Registration

## 2.16.0 (January 2021)
- Native Android ShareSheet
- Option to log HTTP requests and responses
- Move sort menu from toolbar to files view
- Update background images
- Search when sharing with ownCloud
- Bug fixes, including:
  + Fix a crash while accessing a WebDAV folder
  + Fix some crashes when rotating the device
  + Fix a glitch where image was not refreshed properly
  + Fix some issues when using OCIS

## 2.15.3 (October 2020)
- Bug fixes, including:
  + Fix a crash related to downloads notifications
  + Potential fix for ANR when retrying camera uploads
  + Removal of legacy header http.protocol.single-cookie-header

## 2.15.2 (September 2020)
- Update logcat library
- Bug fixes, including:
  + Fixed a crash when browsing up
  + Fixed a crash when logging camera upload request
  + Fixed a crash related with available offline files
  + Fixed a crash related with database migration

## 2.15.1 (July 2020)
- Android 10: TLS 1.3 supported
- Update network libraries to more recent versions, OkHttp + dav4jvm (old dav4Android)
- Rearchitecture of avatar and quota features
- Bug fixes, including:
  + Fixed some authentication problems regarding password edition
  + Fixed available offline bad behaviour when the amount of files is huge
  + Fixed a crash related with FileDataStorageManager
  + Fixed problem related with server setting `version.hide` to allow users login if such setting is enabled.

## 2.15 (June 2020)
- Login rearchitecture
- Support for OpenId Connect
- Native biometrical lock
- UI improvements, including:
  + New bottom navigation bar
- Support for usernames with '+' (Available since oC 10.4.1)
- Chunking adaption to oCIS
- End of support for Android KitKat (4.4)
- End of support for servers older than 10 version
- Bug fixes, including:
  + Fix crash when changing orientation in some operations
  + Fix OAuth2 token is not renewed after being revoked
  + Fix occasional crash when opening share by link
  + Fix navigation loop in shared by link and Av. Offline options

## 2.15 beta v2 (May 2020)
- Login rearchitecture
- Support for OpenId Connect
- Native biometrical lock
- UI improvements, including:
  + New bottom navigation bar
- Support for usernames with '+' (Available since oC 10.4.1)
- Chunking adaption to oCIS
- End of support for Android KitKat (4.4)
- End of support for servers older than 10 version
- Bug fixes, including:
  + Fix crash when changing orientation in some operations
  + Fix OAuth2 token is not renewed after being revoked

## 2.15 beta v1 (May 2020)
- Login rearchitecture
- Support for OpenId Connect
- Native biometrical lock
- UI improvements, including:
  + New bottom navigation bar
- Support for usernames with '+' (Available since oC 10.4.1)
- End of support for Android KitKat (4.4)
- End of support for servers older than 10 version
- Bug fixes, including:
  + Fix crash when changing orientation in some operations
  + Fix OAuth2 token is not renewed after being revoked

## 2.14.2 (January 2020)
- Fix crash triggered when trying to connect to server secured with self signed certificate

## 2.14.1 (December 2019)
- Some improvements in wizard

## 2.14 (December 2019)
- Splash screen
- Shortcut to shared by link files from side menu (contribution)
- Use new server parameter to set a minimum number of characters for searching users, groups or federated shares
- End of support for SAML authentication.
- UI improvements, including:
  + Mix files and folders when sorting them by date (contribution) or size
  + Redesign logs view with new tabs, filters and share options (contribution)
  + Resize cloud image in side menu to not overlap the new side menu options
- Bug fixes, including:
  + Avoid overwritten files with the same name during copy or move operations
  + Retry camera uploads when recovering wifi connectivity and "Upload with wifi only" option is enabled

## 2.13.1 (October 2019)
- Improve oAuth user experience flow and wording when token expires or becomes invalid

## 2.13 (September 2019)
- Copy and move files from other third-party apps or internal storage to an ownCloud account through Downloads or Files app
- Save files in an ownCloud account from third-party apps
- Copy and move files within the same ownCloud account through Downloads or Files app
- Add more logs coverage to gather information about known but difficult to reproduce issues
- UI improvements, including:
  + Show date and size for every file in Available Offline option from side menu

## 2.12 (August 2019)
- Shares rearchitecture
- UI improvements, including:
  + Private link accessible when share API is disabled
- Bug fixes, including:
  + Fix images not detected in Android 9 gallery after being downloaded

## 2.12 beta v1 (August 2019)
- Shares rearchitecture
- UI improvements, including:
  + Private link accessible when share API is disabled
- Bug fixes, including:
  + Fix images not detected in Android 9 gallery after being downloaded

## 2.11.1 (June 2019)
- Fix crash triggered when notifying upload results

## 2.11 (June 2019)
- Replace ownCloud file picker with the Android native one when uploading files (contribution)
- Send logs to support, enable it via new developer menu (contribution)
- Logs search (contribution)
- Shortcut to available offline files from side menu
- Document provider: files and folders rename, edition and deletion.
- Document provider: folder creation
- Document provider: multiaccount support
- UI improvements, including:
  + Notch support
  + Batched permission errors when deleting multiple files (contribution)
- Bug fixes, including:
  + Fix just created folder disappears when synchronizing parent folder
  + Fix crash when clearing successful/failed uploads (contribution)
  + Fix download progress bar still visible after successful download
  + Fix UI glitch in warning icon when sharing a file publicly (contribution)
  + Fix crash when sharing files with ownCloud and creating new folder (contribution)
  + Fix canceling dialog in settings turns on setting (contribution)
  + Bring back select all and select inverse icons to the app bar (contribution)
  + Fix folder with brackets [ ] does not show the content
  + Fix login fails with "§" in password

## 2.11 beta v1 (May 2019)
- Send logs to support, enable it via new developer menu (contribution)
- Logs search (contribution)
- Shortcut to available offline files from side menu
- Document provider: files and folders rename, edition and deletion.
- Document provider: folder creation
- Document provider: multiaccount support
- UI improvements, including:
  + Notch support
- Bug fixes, including:
  + Fix download progress bar still visible after successful download
  + Fix UI glitch in warning icon when sharing a file publicly (contribution)
  + Fix crash when sharing files with ownCloud and creating new folder (contribution)
  + Fix canceling dialog in settings turns on setting (contribution)
  + Bring back select all and select inverse icons to the app bar (contribution)
  + Fix folder with brackets [ ] does not show the content
  + Fix login fails with "§" in password

## 2.10.1 (April 2019)
- Content provider improvements

## 2.10.0 (March 2019)
- Android 9 (P) support (contribution)
- Allow light filtering apps (optional)
- Show additional info (user ID, email) when sharing with users with same display name
- Support more options to enforce password when sharing publicly
- Select all and inverse when uploading files (contribution)
- Sorting options in sharing view (contribution)
- Batched notifications for file deletions (contribution)
- Commit hash in settings (contribution)
- UI improvements, including:
  + Disable log in button when credentials are empty (contribution)
  + Warning to properly set camera folder in camera uploads
- Bug fixes, including:
  + Some camera upload issues in Android 9 (P) (contribution)
  + Fix eye icon not visible to show/hide password in public shares (contribution)
  + Fix welcome wizard rotation (contribution)

## 2.10.0 beta v1 (February 2019)
- Android 9 (P) support (contribution)
- Select all and inverse when uploading files (contribution)
- Sorting options in sharing view (contribution)
- Batched notifications for file deletions (contribution)
- Commit hash in settings (contribution)
- UI improvements, including:
  + Disable log in button when credentials are empty (contribution)
  + Warning to properly set camera folder in camera uploads
- Bug fixes, including:
  + Some camera upload issues in Android 9 (P) (contribution)
  + Fix eye icon not visible to show/hide password in public shares (contribution)
  + Fix welcome wizard rotation (contribution)

## 2.9.3 (November 2018)
- Bug fixes for users with username containing @ character

## 2.9.2 (November 2018)
- Bug fixes for users with username containing spaces

## 2.9.1 (November 2018)
- Bug fixes for LDAP users using uid:
  + Fix login not working
  + Fix empty list of files

## 2.9.0 (November 2018)
- Search in current folder
- Select all/inverse files (contribution)
- Improve available offline files synchronization and conflict resolution (Android 5 or higher required)
- Sort files in file picker when uploading (contribution)
- Access ownCloud files from files apps, even with files not downloaded
- New login view
- Show re-shares
- Switch apache and jackrabbit deprecated network libraries to more modern and active library, OkHttp + Dav4Android
- UI improvements, including:
  + Change edit share icon
  + New gradient in top of the list of files (contribution)
  + More accurate message when creating folders with the same name (contribution)
- Bug fixes, including:
  + Fix some crashes:
    - When rebooting the device
    - When copying, moving files or choosing a folder within camera uploads feature
    - When creating private/public link
  + Fix some failing downloads
  + Fix pattern lock being asked very often after disabling fingerprint lock (contribution)

## 2.9.0 beta v2 (October 2018)
- Bug fixes, including:
  + Fix some crashes:
    - When rebooting the device
    - When copying, moving files or choosing a folder within camera uploads feature
  + Fix some failing downloads
  + Fix pattern lock being asked very often after disabling fingerprint lock

## 2.9.0 beta v1 (September 2018)
- Switch apache and jackrabbit deprecated libraries to more modern and active library, OkHttp
- Search in current folder
- Select all/inverse files
- New login view
- Show re-shares
- UI improvements, including:
  + Change edit share icon
  + New gradient in top of the list of files

## 2.8.0 (July 2018)
- Side menu redesign
- User quota in side menu
- Descending option when sorting
- New downloaded/offline icons and pins
- One panel design for tablets
- Custom tabs for OAuth
- Improve public link sharing permissions for folders
- Redirect to login view when SAML session expires
- UI improvements, including:
  + Fab button above snackbar
  + Toggle to control password visibility when sharing via link
  + Adaptive icons support (Android 8 required)
- Bug fixes, including:
  + Fix block for deleted basic/oauth accounts
  + Fix available offline when renaming files
  + Fix camera directory not selectable in root
  + Fix guest account showing an empty file list
  + Hide keyboard when going back from select user view
  + Fix black "downloading screen" message when downloading an image offline
  + Show proper timestamp in uploads/downloads notification
  + Fix sharing when disabling files versioning app in server

## 2.8.0 beta v1 (May 2018)
- Side menu redesign
- User quota in side menu
- Descending option when sorting
- New downloaded/offline icons and pins
- One panel design for tablets
- Custom tabs for OAuth
- UI improvements, including:
  + Fab button above snackbar
  + Toggle to control password visibility when sharing via link
- Bug fixes, including:
  + Fix block for deleted basic/oauth accounts
  + Fix available offline when renaming files
  + Fix camera directory not selectable in root
  + Fix guest account showing an empty file list
  + Hide keyboard when going back from select user view
  + Fix black "downloading screen" message when downloading an image offline.

## 2.7.0 (April 2018)
- Fingerprint lock
- Pattern lock (contribution)
- Upload picture directly from camera (contribution)
- GIF support
- New features wizard
- UI improvements, including:
  + Display file size during upload (contribution)
  + Animations when switching folders
- Bug fixes, including:
  + Hide always visible notification in Android 8

## 2.7.0 beta v1 (March 2018)
- Fingerprint lock
- Pattern lock (contribution)
- Upload picture directly from camera (contribution)
- GIF support
- New features wizard
- UI improvements, including:
  + Display file size during upload (contribution)
- Bug fixes, including:
  + Hide always visible notification in Android 8

## 2.6.0 (February 2018)
- Camera uploads, replacing instant uploads (Android 5 or higher required)
- Android 8 support
- Notification channels (Android 8 required)
- Private link (OC X required)
- Fixed typos in some translations

## 2.5.1 beta v1 (November 2017)
- Camera uploads (replacing instant uploads)
- Android O support
- Notification channels (Android O required)
- Private link (OC X required)
- Fixed typos in some translations

## 2.5.0 (October 2017)
- OAuth2 support
- Show file listing option (anonymous upload) when sharing a folder (OC X required)
- First approach to fix instant uploads
- UI improvements, including:
  + Hide share icon when resharing is forbidden
  + Improve feedback when uploading infected files
- Bug fixes

## 2.4.0 (May 2017)
- Video streaming
- Multiple public links per file (OC X required)
- Share with custom groups (OC X required)
- Automated retry of failed transfers in Android 6 and 7
- Save shared text as new file
- File count per section in uploads view
- UI improvements, including:
  + Share view update
- Bug fixes

## 2.3.0 (March 2017)
- Included privacy policy.
- Error messages improvement.
- Design/UI improvement: snackbars replace toasts.
- Bugs fixed, including:
  + Crash when other app uses same account name.

## 2.2.0 (December 2016)
- Set folders as Available Offline
- New navigation drawer, with avatar and account switch.
- New account manager, accessible from navigation drawer.
- Set edit permissions in federated shares of folders (OC server >= 9.1)
- Monitor and revoke session from web UI (OC server >= 9.1)
- Improved look and contents of file menu.
- Bugs fixed, including:
  + Keep modification time of uploaded files.
  + Stop audio when file is deleted.
  + Upload of big files.

## 2.1.2 (September 2016)
- Instant uploads fixed in Android 6.

## 2.1.1 (September 2016)
- Instant uploads work in Android 7.
- Select your camera folder to upload pictures or videos from any
 camera app.
- Multi-Window support for Android 7.
- Size of folders shown in list of files.
- Sort by size your list of files.

## 2.1.0 (August 2016)
- Select and handle multiple files
- Sync files on tap
- Access files through Documents Provider
- "Can share" option for federated shares (server 9.1+)
- Full name shown instead of user name
- New icon
- Style and sorting fixes
- Bugs fixed, including:
  + Icon "available offline" shown when set
  + Trim blanks of username in login view
  + Protect password field from suggestions

## 2.0.1 (June 2016)
- Favorite files are now called AVAILABLE OFFLINE
- New overlay icons
- Bugs fixed, including:
 + Upload content from other apps works again
 + Passwords with non-alphanumeric characters work fine
 + Sending files from other apps does not duplicate them
 + Favorite setting is not lost after uploading
 + Instant uploads waiting for Wi-Fi are not shown as failed

## 2.0.0 (April 2016)
- Uploads view: track the progress of your uploads and handle failures
- Federated sharing: share files with users in other ownCloud servers
- Improvements on the UI following material design lines
- Set a shared-by-link folder as editable
- Wifi-only for instant uploads stop on Wifi loss
- Be warned of server certificate changed in any action
- Improvements when other apps send files to ownCloud
- Bug fixing

## 1.9.1 (February 2016)
- Set and edit permissions on internal shared data
- Instant uploads: avoid file duplications, set policy in app settings
- Control duplication of files uploaded via 'Upload' button
- Select view mode: either list or grid per folder
- More Material Design: buttons and checkboxes
- Fixed battery drain in automatic synchronization
- Security fixes related to passcode
- Wording fixes

## 1.9.0 (December 2015)
- Share privately with users or groups in your server
- Share link with password protection and expiration date
- Fully sync a folder in two ways (manually)
- Detect share configuration in server
- Fingerprints in untrusted certificate dialog
- Thumbnail in details view
- OC color in notifications
- Fixed video preview
- Fixed sorting with accents
- Error shown when no app can "open with" a file
- Fixed relative date in some languages
- Media scanner triggered after uploads

## 1.8.0 (September 2015)
- New MATERIAL DESIGN theme
- Updated FILE TYPE ICONS
- Preview TXT files within the app
- COPY files & folders
- Preview the full file/folder name from the long press menu
- Set a file as FAVORITE (kept-in-sync) from the CONTEXT MENU
- Updated CONFLICT RESOLUTION dialog (wording)
- Updated background for images with TRANSPARENCY in GALLERY
- Hidden files will not enforce list view instead of GRID VIEW (folders from Picasa & others)
- Security:
  + Updated network stack with security fixes (Jackrabbit 2.10.1)
- Bugs fixed:
  + Fixed crash when ETag is lost
  + Passcode creation not restarted on device rotation
  + Recovered share icon shown on folders 'shared with me'
  + User name added to subject when sending a share link through e-mail (fixed on SAMLed apps)

## 1.7.2 (July 2015)
- New navigation drawer
- Improved Passcode
- Automatic grid view just for folders full of images
- More characters allowed in file names
- Support for servers in same domain, different path
- Bugs fixed:
  + Frequent crashes in folder with several images
  + Sync error in servers with huge quota and external storage enable
  + Share by link error
  + Some other crashes and minor bugs

## 1.7.1 (April 2015)

- Share link even with password enforced by server
- Get the app ready for oc 8.1 servers
- Added option to create new folder in uploads from external apps
- Improved management of deleted users
- Bugs fixed
  + Fixed crash on Android 2.x devices
  + Improvements on uploads

## 1.7.0 (February 2015)

- Download full folders
- Grid view for images
- Remote thumbnails (OC Server 8.0+)
- Added number of files and folders at the end of the list
- "Open with" in contextual menu
- Downloads added to Media Provider
- Uploads:
  + Local thumbnails in section "Files"
  + Multiple selection in "Content from other apps" (Android 4.3+)
- Gallery:
  + proper handling of EXIF
  + obey sorting in the list of files
- Settings view updated
- Improved subjects in e-mails
- Bugs fixed
