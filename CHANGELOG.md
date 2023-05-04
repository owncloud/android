Changelog for ownCloud Android Client [unreleased] (UNRELEASED)
=======================================
The following sections list the changes in ownCloud Android Client unreleased relevant to
ownCloud admins and users.

[unreleased]: https://github.com/owncloud/android/compare/v3.0.4...master

Summary
-------

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
* Enhancement - Updated WebFinger flow: [#3998](https://github.com/owncloud/android/issues/3998)
* Enhancement - Monochrome icon for the app: [#4001](https://github.com/owncloud/android/pull/4001)
* Enhancement - Add prompt parameter to OIDC flow: [#4011](https://github.com/owncloud/android/pull/4011)

Details
-------

* Bugfix - Error message for protocol exception: [#3948](https://github.com/owncloud/android/issues/3948)

   Previously, when the network connection is lost while uploading a file, "Unknown error" was
   shown. Now, we show a more specific error.

   https://github.com/owncloud/android/issues/3948
   https://github.com/owncloud/android/pull/4013
   https://github.com/owncloud/android-library/pull/558

* Bugfix - Incorrect list of files in av. offline when browsing from details: [#3986](https://github.com/owncloud/android/issues/3986)

   When opening the details view of a file accessed from the available offline shortcut, browsing
   back led to a incorrect list of files. Now, browsing back leads to the list of available offline
   files again.

   https://github.com/owncloud/android/issues/3986
   https://github.com/owncloud/android/pull/4026

* Change - Bump target SDK to 33: [#3617](https://github.com/owncloud/android/issues/3617)

   Target SDK was upgraded to 33 to keep the app updated with the latest android changes. A new
   setting was introduced to manage notifications in an easier way.

   https://github.com/owncloud/android/issues/3617
   https://github.com/owncloud/android/pull/3972
   https://developer.android.com/about/versions/13/behavior-changes-13

* Change - Use ViewBinding in FolderPickerActivity: [#3796](https://github.com/owncloud/android/issues/3796)

   The use of findViewById method was replaced by using ViewBinding in the
   FolderPickerActivity.

   https://github.com/owncloud/android/issues/3796
   https://github.com/owncloud/android/pull/4014

* Change - Use ViewBinding in WhatsNewActivity: [#3796](https://github.com/owncloud/android/issues/3796)

   The use of findViewById method was replaced by using ViewBinding in the WhatsNewActivity.

   https://github.com/owncloud/android/issues/3796
   https://github.com/owncloud/android/pull/4021

* Enhancement - Support for Markdown files: [#3716](https://github.com/owncloud/android/issues/3716)

   Markdown files preview will now be rendered to show its content in a prettier way.

   https://github.com/owncloud/android/issues/3716
   https://github.com/owncloud/android/pull/4017

* Enhancement - Support for spaces: [#3851](https://github.com/owncloud/android/pull/3851)

   Spaces are now supported in oCIS accounts. A new tab has been added, which allows to list and
   browse through all the available spaces for the current account. The supported operations for
   files in spaces are: download, upload, remove, rename, create folder, copy and move. The
   documents provider has been adapted as well to be able to browse through spaces and perform the
   operations already mentioned.

   https://github.com/owncloud/android/pull/3851

* Enhancement - Update label on Camera Uploads: [#3930](https://github.com/owncloud/android/pull/3930)

   Update label on camera uploads to avoid confusions with the behavior of original files. Now, it
   is clear that original files will be removed.

   https://github.com/owncloud/android/pull/3930

* Enhancement - Authenticated WebFinger: [#3943](https://github.com/owncloud/android/issues/3943)

   Authenticated WebFinger was introduced into the authentication flow. Now, WebFinger is used
   to retrieve the OpenID Connect issuer and the available ownCloud instances. For the moment,
   multiple oC instances are not supported, only the first available instance is used.

   https://github.com/owncloud/android/issues/3943
   https://github.com/owncloud/android/pull/3945
   https://doc.owncloud.com/ocis/next/deployment/services/s-list/webfinger.html

* Enhancement - Link in drawer menu: [#3949](https://github.com/owncloud/android/pull/3949)

   Customers will be able now to set a personalized label and link that will appear in the drawer
   menu, together with the drawer logo as an icon.

   https://github.com/owncloud/android/issues/3907
   https://github.com/owncloud/android/pull/3949

* Enhancement - Send language header in all requests: [#3980](https://github.com/owncloud/android/issues/3980)

   Added Accept-Language header to all requests so the android App can receive translated
   content.

   https://github.com/owncloud/android/issues/3980
   https://github.com/owncloud/android/pull/3982
   https://github.com/owncloud/android-library/pull/551

* Enhancement - Open in specific web provider: [#3994](https://github.com/owncloud/android/issues/3994)

   We've added the specific web app providers instead of opening the file with the default web
   provider.

   The user can open their files with any of the available specific web app providers from the
   server. Previously, file was opened with the default one.

   https://github.com/owncloud/android/issues/3994
   https://github.com/owncloud/android/pull/3990
   https://owncloud.dev/services/app-registry/apps/#app-registry

* Enhancement - Updated WebFinger flow: [#3998](https://github.com/owncloud/android/issues/3998)

   WebFinger call won't follow redirections. WebFinger will be requested first and will skip
   status.php in case it's successful, and in case the lookup server is not directly accessible,
   we will continue the authentication flow with the regular status.php.

   https://github.com/owncloud/android/issues/3998
   https://github.com/owncloud/android/pull/4000
   https://github.com/owncloud/android-library/pull/555

* Enhancement - Monochrome icon for the app: [#4001](https://github.com/owncloud/android/pull/4001)

   From Android 13, if the user has enabled themed app icons in their device settings, the app will
   be shown with a monochrome icon.

   https://github.com/owncloud/android/pull/4001

* Enhancement - Add prompt parameter to OIDC flow: [#4011](https://github.com/owncloud/android/pull/4011)

   Added prompt parameter to the authorization request in case OIDC is supported. By default,
   select_account will be sent. It can be changed via branding or MDM.

   https://github.com/owncloud/android/issues/3862
   https://github.com/owncloud/android/issues/3984
   https://github.com/owncloud/android/pull/4011

Changelog for ownCloud Android Client [3.0.4] (2023-03-07)
=======================================
The following sections list the changes in ownCloud Android Client 3.0.4 relevant to
ownCloud admins and users.

[3.0.4]: https://github.com/owncloud/android/compare/v3.0.3...v3.0.4

Summary
-------

* Security - Fix for security issues with database: [#3952](https://github.com/owncloud/android/pull/3952)
* Enhancement - HTTP logs show more info: [#547](https://github.com/owncloud/android-library/pull/547)

Details
-------

* Security - Fix for security issues with database: [#3952](https://github.com/owncloud/android/pull/3952)

   Some fixes have been added so that now no part of the app's database can be accessed from other
   apps.

   https://github.com/owncloud/android/pull/3952

* Enhancement - HTTP logs show more info: [#547](https://github.com/owncloud/android-library/pull/547)

   When enabling HTTP logs, now the URL for each log will be shown as well to make debugging easier.

   https://github.com/owncloud/android-library/pull/547

Changelog for ownCloud Android Client [3.0.3] (2023-02-13)
=======================================
The following sections list the changes in ownCloud Android Client 3.0.3 relevant to
ownCloud admins and users.

[3.0.3]: https://github.com/owncloud/android/compare/v3.0.2...v3.0.3

Summary
-------

* Bugfix - Error messages too long in folders operation: [#3852](https://github.com/owncloud/android/pull/3852)
* Bugfix - Fix problems after authentication: [#3889](https://github.com/owncloud/android/pull/3889)
* Bugfix - Toolbar in file details view: [#3899](https://github.com/owncloud/android/pull/3899)

Details
-------

* Bugfix - Error messages too long in folders operation: [#3852](https://github.com/owncloud/android/pull/3852)

   Error messages when trying to perform a non-allowed action for copying and moving folders have
   been shortened so that they are shown completely in the snackbar.

   https://github.com/owncloud/android/issues/3820
   https://github.com/owncloud/android/pull/3852

* Bugfix - Fix problems after authentication: [#3889](https://github.com/owncloud/android/pull/3889)

   Client for session are now fetched on demand to avoid reinitialize DI, making the process
   smoother

   https://github.com/owncloud/android/pull/3889

* Bugfix - Toolbar in file details view: [#3899](https://github.com/owncloud/android/pull/3899)

   When returning from the share screen to details screen, the toolbar didn't show the correct
   options and title. Now it does.

   https://github.com/owncloud/android/issues/3866
   https://github.com/owncloud/android/pull/3899

Changelog for ownCloud Android Client [3.0.2] (2023-01-26)
=======================================
The following sections list the changes in ownCloud Android Client 3.0.2 relevant to
ownCloud admins and users.

[3.0.2]: https://github.com/owncloud/android/compare/v3.0.1...v3.0.2

Summary
-------

* Bugfix - Fix reauthentication prompt: [#534](https://github.com/owncloud/android-library/pull/534)
* Enhancement - Branded scope for OpenID Connect: [#3869](https://github.com/owncloud/android/pull/3869)

Details
-------

* Bugfix - Fix reauthentication prompt: [#534](https://github.com/owncloud/android-library/pull/534)

   Potential fix to oauth error after logging in for first time that makes user to reauthenticate

   https://github.com/owncloud/android-library/pull/534

* Enhancement - Branded scope for OpenID Connect: [#3869](https://github.com/owncloud/android/pull/3869)

   OpenID Connect scope is now brandable via setup.xml file or MDM

   https://github.com/owncloud/android/pull/3869

Changelog for ownCloud Android Client [3.0.1] (2022-12-21)
=======================================
The following sections list the changes in ownCloud Android Client 3.0.1 relevant to
ownCloud admins and users.

[3.0.1]: https://github.com/owncloud/android/compare/v3.0.0...v3.0.1

Summary
-------

* Bugfix - Fix crash when upgrading from 2.18: [#3837](https://github.com/owncloud/android/pull/3837)
* Bugfix - Fix crash when opening uploads section: [#3841](https://github.com/owncloud/android/pull/3841)

Details
-------

* Bugfix - Fix crash when upgrading from 2.18: [#3837](https://github.com/owncloud/android/pull/3837)

   Upgrading from 2.18 or older versions made the app crash due to camera uploads data migration.
   This problem has been solved and now the app upgrades correctly.

   https://github.com/owncloud/android/pull/3837

* Bugfix - Fix crash when opening uploads section: [#3841](https://github.com/owncloud/android/pull/3841)

   When upgrading from an old version with uploads with "forget" behaviour, app crashed when
   opening the uploads tab. Now, this has been fixed so that it works correctly.

   https://github.com/owncloud/android/pull/3841

Changelog for ownCloud Android Client [3.0.0] (2022-12-12)
=======================================
The following sections list the changes in ownCloud Android Client 3.0.0 relevant to
ownCloud admins and users.

[3.0.0]: https://github.com/owncloud/android/compare/v2.21.2...v3.0.0

Summary
-------

* Bugfix - Fix for thumbnails: [#3719](https://github.com/owncloud/android/pull/3719)
* Enhancement - Sync engine rewritten: [#2934](https://github.com/owncloud/android/pull/2934)
* Enhancement - Faster browser authentication: [#3632](https://github.com/owncloud/android/pull/3632)
* Enhancement - Several transfers running simultaneously: [#3710](https://github.com/owncloud/android/pull/3710)
* Enhancement - Empty views improved: [#3728](https://github.com/owncloud/android/pull/3728)
* Enhancement - Automatic conflicts propagation: [#3766](https://github.com/owncloud/android/pull/3766)

Details
-------

* Bugfix - Fix for thumbnails: [#3719](https://github.com/owncloud/android/pull/3719)

   Some thumbnails were not shown in the file list. Now, they are all shown correctly.

   https://github.com/owncloud/android/issues/2818
   https://github.com/owncloud/android/pull/3719

* Enhancement - Sync engine rewritten: [#2934](https://github.com/owncloud/android/pull/2934)

   The whole synchronization engine has been refactored to a new architecture to make it better
   structured and more efficient.

   https://github.com/owncloud/android/issues/2818
   https://github.com/owncloud/android/pull/2934

* Enhancement - Faster browser authentication: [#3632](https://github.com/owncloud/android/pull/3632)

   Login flow has been improved by saving a click when the server is OAuth2/OIDC and it is valid.
   Also, when authenticating again in a OAuth2/OIDC account already saved in the app, the
   username is already shown in the browser.

   https://github.com/owncloud/android/issues/3759
   https://github.com/owncloud/android/pull/3632

* Enhancement - Several transfers running simultaneously: [#3710](https://github.com/owncloud/android/pull/3710)

   With the sync engine refactor, now several downloads and uploads can run at the same time,
   improving efficiency.

   https://github.com/owncloud/android/issues/3426
   https://github.com/owncloud/android/pull/3710

* Enhancement - Empty views improved: [#3728](https://github.com/owncloud/android/pull/3728)

   When the list of items is empty, we now show a more attractive view. This applies to file list,
   available offline list, shared by link list, uploads list, logs list and external share list.

   https://github.com/owncloud/android/issues/3026
   https://github.com/owncloud/android/pull/3728

* Enhancement - Automatic conflicts propagation: [#3766](https://github.com/owncloud/android/pull/3766)

   Conflicts are now propagated automatically to parent folders, and cleaned when solved or
   removed. Before, it was needed to navigate to the file location for the conflict to propagate.
   Also, move, copy and remove actions work properly with conflicts.

   https://github.com/owncloud/android/issues/3005
   https://github.com/owncloud/android/pull/3766

Changelog for ownCloud Android Client [2.21.2] (2022-09-07)
=======================================
The following sections list the changes in ownCloud Android Client 2.21.2 relevant to
ownCloud admins and users.

[2.21.2]: https://github.com/owncloud/android/compare/v2.21.1...v2.21.2

Summary
-------

* Enhancement - Open in web: [#3672](https://github.com/owncloud/android/issues/3672)
* Enhancement - Shares from propfind: [#3711](https://github.com/owncloud/android/issues/3711)
* Enhancement - Private link capability: [#3732](https://github.com/owncloud/android/issues/3732)

Details
-------

* Enhancement - Open in web: [#3672](https://github.com/owncloud/android/issues/3672)

   OCIS feature, to open files with mime types supported by the server in the web browser using
   collaborative or specific tools

   https://github.com/owncloud/android/issues/3672
   https://github.com/owncloud/android/pull/3737

* Enhancement - Shares from propfind: [#3711](https://github.com/owncloud/android/issues/3711)

   Added a new property to the propfind, so that, we can get if the files in a folder are shared
   directly with just one request. Previously, a propfind and another additional request were
   needed to the shares api to retrieve the shares of the folder.

   https://github.com/owncloud/android/issues/3711
   https://github.com/owncloud/android-library/pull/496

* Enhancement - Private link capability: [#3732](https://github.com/owncloud/android/issues/3732)

   Private link capability is now respected. Option is shown/hidden depending on its value

   https://github.com/owncloud/android/issues/3732
   https://github.com/owncloud/android/pull/3738
   https://github.com/owncloud/android-library/pull/505

Changelog for ownCloud Android Client [2.21.1] (2022-06-15)
=======================================
The following sections list the changes in ownCloud Android Client 2.21.1 relevant to
ownCloud admins and users.

[2.21.1]: https://github.com/owncloud/android/compare/v2.21.0...v2.21.1

Summary
-------

* Bugfix - Fix crash when opening from details screen: [#3696](https://github.com/owncloud/android/pull/3696)

Details
-------

* Bugfix - Fix crash when opening from details screen: [#3696](https://github.com/owncloud/android/pull/3696)

   Fixed a crash when opening a non downloaded file from the details view.

   https://github.com/owncloud/android/pull/3696

Changelog for ownCloud Android Client [2.21.0] (2022-06-07)
=======================================
The following sections list the changes in ownCloud Android Client 2.21.0 relevant to
ownCloud admins and users.

[2.21.0]: https://github.com/owncloud/android/compare/v2.20.0...v2.21.0

Summary
-------

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

Details
-------

* Bugfix - Prevented signed in user in the list of users to be shared: [#1419](https://github.com/owncloud/android/issues/1419)

   Previously, user list for sharing contains signed in user, now this user is omitted to avoid
   errors.

   https://github.com/owncloud/android/issues/1419
   https://github.com/owncloud/android/pull/3643

* Bugfix - Corrupt picture error controlled: [#3441](https://github.com/owncloud/android/issues/3441)

   Previously, If a file is not correct or is damaged, it is downloaded but not previewed. An
   infinite spinner on a black window is shown instead. Now, an error appears warning to the user.

   https://github.com/owncloud/android/issues/3441
   https://github.com/owncloud/android/pull/3644

* Bugfix - Security flags for recording screen: [#3468](https://github.com/owncloud/android/issues/3468)

   Previously, if passcode or pattern were enabled, no screen from the app could be viewed from a
   recording screen app. Now, only the login, passcode and pattern screens are protected against
   recording.

   https://github.com/owncloud/android/issues/3468
   https://github.com/owncloud/android/pull/3560

* Bugfix - Crash when changing orientation in Details view: [#3571](https://github.com/owncloud/android/issues/3571)

   Previously, the app crashes when changing orientation in Details view after installing Now,
   app shows correctly the details after installing.

   https://github.com/owncloud/android/issues/3571
   https://github.com/owncloud/android/pull/3589

* Bugfix - Lock displays shown again: [#3591](https://github.com/owncloud/android/issues/3591)

   Previously, if you clicked on passcode or pattern lock to remove it, and then you clicked on
   cancel, the lock display was shown again to put the passcode or pattern. Now, if you cancel it,
   you come back to settings screen.

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

   Previously,they appeared exactly the same and there was no way of knowing which was which. Now
   they are differentiated by the text on the action button.

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

   Two parameters (server url and server url input visibility) can be now managed via MDM. These
   were the first parameters used to test integration with Android Enterprise and Android
   Management API.

   https://github.com/owncloud/android/issues/3415
   https://github.com/owncloud/android/pull/3419

* Enhancement - Provide app feedback to MDM admins: [#3420](https://github.com/owncloud/android/issues/3420)

   Now, when a MDM configuration is applied for the first time or changed by an IT administrator,
   the app sends feedback that will be shown in the EMM console.

   https://github.com/owncloud/android/issues/3420
   https://github.com/owncloud/android/pull/3480

* Enhancement - Lock delay enforced: [#3440](https://github.com/owncloud/android/issues/3440)

   A new local setup's option has been added for the application to lock after the selected
   interval

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

   Redesign of the passcode screen to have the numeric keyboard in the screen instead of using the
   Android one.

   https://github.com/owncloud/android/issues/3516
   https://github.com/owncloud/android/pull/3582

* Enhancement - Extended security enforced: [#3543](https://github.com/owncloud/android/issues/3543)

   New extended branding options have been added to make app lock via passcode or pattern
   compulsory.

   https://github.com/owncloud/android/issues/3543
   https://github.com/owncloud/android/pull/3544

* Enhancement - Improvements for the UI in the pattern screen: [#3580](https://github.com/owncloud/android/issues/3580)

   Redesign of the pattern screen. Cancel button deleted and new back arrow in the toolbar.

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

   When a user clicks on a file's detail view thumbnail, the file is automatically downloaded and
   previewed.

   https://github.com/owncloud/android/pull/3653

Changelog for ownCloud Android Client [2.20.0] (2022-02-16)
=======================================
The following sections list the changes in ownCloud Android Client 2.20.0 relevant to
ownCloud admins and users.

[2.20.0]: https://github.com/owncloud/android/compare/v2.19.0...v2.20.0

Summary
-------

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

Details
-------

* Bugfix - Small glitch when side menu is full of accounts: [#3437](https://github.com/owncloud/android/pull/3437)

   Previously, when users set up a large number of accounts, the side menu overlapped the
   available space quota. Now, everything is contained within a scroll to avoid this.

   https://github.com/owncloud/android/issues/3060
   https://github.com/owncloud/android/pull/3437

* Bugfix - Small bug when privacy policy disabled: [#3542](https://github.com/owncloud/android/pull/3542)

   Previously, when privacy policy setup was disabled, the side menu showed the privacy policy
   menu item. Now, option is hidden when privacy policy is disabled.

   https://github.com/owncloud/android/issues/3521
   https://github.com/owncloud/android/pull/3542

* Enhancement - Permission dialog removal: [#2524](https://github.com/owncloud/android/pull/2524)

   The old permission request dialog has been removed. It was not needed after migrating the
   storage to scoped storage, read and write permissions are guaranteed in our scoped storage.

   https://github.com/owncloud/android/pull/2524

* Enhancement - Brute force protection: [#3320](https://github.com/owncloud/android/issues/3320)

   Previously, when setting passcode lock, an unlimited number of attempts to unlock the app
   could be done in a row. Now, from the third incorrect attempt, there will be an exponential
   growing waiting time until next unlock attempt.

   https://github.com/owncloud/android/issues/3320
   https://github.com/owncloud/android/pull/3463

* Enhancement - Lock delay for app: [#3344](https://github.com/owncloud/android/issues/3344)

   A new preference has been added to choose the interval in which the app will be unlocked after
   having unlocked it once, making it more comfortable for those who access the app frequently and
   have a security lock set.

   https://github.com/owncloud/android/issues/3344
   https://github.com/owncloud/android/pull/3375

* Enhancement - Allow access from document provider preference: [#3379](https://github.com/owncloud/android/issues/3379)

   Previously, files of ownCloud accounts couldn't be accessed via documents provider when
   there was a lock set in the app. Now, a new preference has been added to allow/disallow the
   access, so users have more control over their files.

   https://github.com/owncloud/android/issues/3379
   https://github.com/owncloud/android/issues/3520
   https://github.com/owncloud/android/pull/3384
   https://github.com/owncloud/android/pull/3538

* Enhancement - Security enforced: [#3434](https://github.com/owncloud/android/pull/3434)

   A new branding/MDM option has been added to make app lock via passcode or pattern compulsory,
   whichever the user chooses.

   https://github.com/owncloud/android/issues/3400
   https://github.com/owncloud/android/pull/3434

* Enhancement - Respect capability for Avatar support: [#3438](https://github.com/owncloud/android/pull/3438)

   Previously, the user's avatar was shown by default. Now, it is shown or not depending on a new
   capability.

   https://github.com/owncloud/android/issues/3285
   https://github.com/owncloud/android/pull/3438

* Enhancement - "Open with" action now allows editing: [#3475](https://github.com/owncloud/android/issues/3475)

   Previously, when a document file was opened and edited with an external app, changes weren't
   saved because it didn't synchronized with the server. Now, when you edit a document and
   navigate or refresh in the ownCloud app, it synchronizes automatically, keeping consistence
   of your files.

   https://github.com/owncloud/android/issues/3475
   https://github.com/owncloud/android/pull/3499

* Enhancement - Enable logs by default in debug mode: [#3526](https://github.com/owncloud/android/issues/3526)

   Now, when the app is built in DEBUG mode, the logs are enabled by default.

   https://github.com/owncloud/android/issues/3526
   https://github.com/owncloud/android/pull/3527

* Enhancement - Suggest the user to enable enhanced security: [#3539](https://github.com/owncloud/android/pull/3539)

   When a user sets the passcode or pattern lock on the security screen, the application suggests
   the user whether to enable or not a biometric lock to unlock the application.

   https://github.com/owncloud/android/pull/3539

Changelog for ownCloud Android Client [2.19.0] (2021-11-15)
=======================================
The following sections list the changes in ownCloud Android Client 2.19.0 relevant to
ownCloud admins and users.

[2.19.0]: https://github.com/owncloud/android/compare/v2.18.3...v2.19.0

Summary
-------

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

Details
-------

* Bugfix - Crash in FileDataStorageManager: [#2896](https://github.com/owncloud/android/issues/2896)

   A possible null value with the account that caused certain crashes on Android 10 devices has
   been controlled.

   https://github.com/owncloud/android/issues/2896
   https://github.com/owncloud/android/pull/3383

* Bugfix - Account removed is not removed from the drawer: [#3340](https://github.com/owncloud/android/issues/3340)

   When an account was deleted from the device settings, in the accounts section, it was not
   removed from the Navigation Drawer. Now, when deleting an account from there, the Navigation
   Drawer is refreshed and the removed account is no more shown.

   https://github.com/owncloud/android/issues/3340
   https://github.com/owncloud/android/pull/3381

* Bugfix - Passcode input misbehaving: [#3342](https://github.com/owncloud/android/issues/3342)

   Passcode text fields have been made not selectable once a number is written on them, so that we
   avoid bugs with the digits of the passcode and the way of entering them.

   https://github.com/owncloud/android/issues/3342
   https://github.com/owncloud/android/pull/3365

* Bugfix - Lack of back button in Logs view: [#3357](https://github.com/owncloud/android/issues/3357)

   A new back arrow button has been added in the toolbar in Logs screen, so that now it's possible to
   return to the settings screen without the use of physical buttons of the device.

   https://github.com/owncloud/android/issues/3357
   https://github.com/owncloud/android/pull/3363

* Bugfix - ANR after removing account with too many downloaded files: [#3362](https://github.com/owncloud/android/issues/3362)

   Previously, when a user account was deleted, the application could freeze when trying to
   delete a large number of files. Now, the application has been fixed so that it doesn't freeze
   anymore by doing this.

   https://github.com/owncloud/android/issues/3362
   https://github.com/owncloud/android/pull/3380

* Bugfix - Camera Upload manual retry: [#3418](https://github.com/owncloud/android/pull/3418)

   Previously, when users selected to retry a single camera upload, an error message appeared.
   Now, the retry of a single upload is enqueued again as expected.

   https://github.com/owncloud/android/issues/3417
   https://github.com/owncloud/android/pull/3418

* Bugfix - Device rotation moves to root in folder picker: [#3431](https://github.com/owncloud/android/pull/3431)

   Previously, when users rotate the device trying to share photos with oC selecting a non-root
   folder, folder picker shows the root folder Now, folder picker shows the folder that the user
   browsed.

   https://github.com/owncloud/android/issues/3163
   https://github.com/owncloud/android/pull/3431

* Bugfix - Logging does not stop when the user deactivates it: [#3436](https://github.com/owncloud/android/pull/3436)

   Previously, when users disabled the logging option in the settings, the application would not
   stop logging and the size of the log files would increase. Now, the option to disable it works
   perfectly and no logs are collected if disabled.

   https://github.com/owncloud/android/issues/3325
   https://github.com/owncloud/android/pull/3436

* Enhancement - Instant upload only when charging: [#465](https://github.com/owncloud/android/issues/465)

   A new option has been added in the auto upload pictures/videos screen, so that now it's possible
   to upload pictures or videos only when charging.

   https://github.com/owncloud/android/issues/465
   https://github.com/owncloud/android/issues/3315
   https://github.com/owncloud/android/pull/3385

* Enhancement - Scoped Storage: [#2877](https://github.com/owncloud/android/issues/2877)

   The way to store files in the device has changed completely. Previously, the files were stored
   in the shared storage. That means that apps that had access to the shared storage, could read,
   write or do whatever they wanted with the ownCloud files.

   Now, ownCloud files are stored in the Scoped Storage, so they are safer. Other apps can access
   ownCloud files using the Documents Provider, which is the native way to do it, and that means
   that the ownCloud app has full control of its files.

   Furthermore, if the app is removed, the files downloaded to ownCloud are removed too. So, files
   are not lost or forgotten in the device after uninstalling the app.

   https://github.com/owncloud/android/issues/2877
   https://github.com/owncloud/android/pull/3269

* Enhancement - Delete old logs every week: [#3328](https://github.com/owncloud/android/issues/3328)

   Previously, logs were stored but never deleted. It used a lot of storage when logs were enabled
   for some time. Now, the logs are removed periodically every week.

   https://github.com/owncloud/android/issues/3328
   https://github.com/owncloud/android/pull/3337

* Enhancement - New Logging Screen 2.0: [#3333](https://github.com/owncloud/android/issues/3333)

   A new option has been added to the logging screen, so that now it's possible to share/delete log
   files or open them.

   https://github.com/owncloud/android/issues/3333
   https://github.com/owncloud/android/pull/3408

* Enhancement - Delete old user directories in order to free memory: [#3336](https://github.com/owncloud/android/pull/3336)

   Previously, when users deleted an account the synchronized files of this account stayed on the
   SD-Card. So if the user didn't want them anymore he had to delete them manually. Now, the app
   automatically removes the files associated with an account.

   https://github.com/owncloud/android/issues/125
   https://github.com/owncloud/android/pull/3336

Changelog for ownCloud Android Client [2.18.3] (2021-10-27)
=======================================
The following sections list the changes in ownCloud Android Client 2.18.3 relevant to
ownCloud admins and users.

[2.18.3]: https://github.com/owncloud/android/compare/v2.18.1...v2.18.3

Summary
-------

* Enhancement - Privacy policy button more accessible: [#3423](https://github.com/owncloud/android/pull/3423)

Details
-------

* Enhancement - Privacy policy button more accessible: [#3423](https://github.com/owncloud/android/pull/3423)

   The privacy policy button has been removed from "More" settings section, and it has been added
   to general settings screen as well as to the drawer menu, so that it is easier and more accessible
   for users.

   https://github.com/owncloud/android/issues/3422
   https://github.com/owncloud/android/pull/3423

Changelog for ownCloud Android Client [2.18.1] (2021-07-20)
=======================================
The following sections list the changes in ownCloud Android Client 2.18.1 relevant to
ownCloud admins and users.

[2.18.1]: https://github.com/owncloud/android/compare/v2.18.0...v2.18.1

Summary
-------

* Security - Add PKCE support: [#3310](https://github.com/owncloud/android/pull/3310)
* Enhancement - Replace picker to select camera folder with native one: [#2899](https://github.com/owncloud/android/issues/2899)
* Enhancement - Hide "More" section if all options are disabled: [#3271](https://github.com/owncloud/android/issues/3271)
* Enhancement - Note icon in music player to be branded: [#3272](https://github.com/owncloud/android/issues/3272)

Details
-------

* Security - Add PKCE support: [#3310](https://github.com/owncloud/android/pull/3310)

   PKCE (Proof Key for Code Exchange) support defined in RFC-7636 was added to prevent
   authorization code interception attacks.

   https://github.com/owncloud/android/pull/3310

* Enhancement - Replace picker to select camera folder with native one: [#2899](https://github.com/owncloud/android/issues/2899)

   The custom picker to select the camera folder was replaced with the native one. Now, it is ready
   for scoped storage and some problems to select a folder in the SD Card were fixed. Also, a new
   field to show the last synchronization timestamp was added.

   https://github.com/owncloud/android/issues/2899
   https://github.com/owncloud/android/pull/3293

* Enhancement - Hide "More" section if all options are disabled: [#3271](https://github.com/owncloud/android/issues/3271)

   A blank view was shown when all options in "More" subsection were disabled. Now, the subsection
   is only shown if at least one option is enabled.

   https://github.com/owncloud/android/issues/3271
   https://github.com/owncloud/android/pull/3296

* Enhancement - Note icon in music player to be branded: [#3272](https://github.com/owncloud/android/issues/3272)

   The note icon in the music player will have the same color as the toolbar, so branded apps can have
   the icon tinted using their custom theme.

   https://github.com/owncloud/android/issues/3272
   https://github.com/owncloud/android/pull/3297

Changelog for ownCloud Android Client [2.18.0] (2021-05-24)
=======================================
The following sections list the changes in ownCloud Android Client 2.18.0 relevant to
ownCloud admins and users.



Summary
-------

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

Details
-------

* Bugfix - Snackbar in passcode view is not displayed: [#2722](https://github.com/owncloud/android/issues/2722)

   Snackbar telling about an error in a failed enter or reenter of the passcode wasn't visible.
   Now, the message is shown in a text just below the passcode input.

   https://github.com/owncloud/android/issues/2722
   https://github.com/owncloud/android/pull/3210

* Bugfix - Fixed problem when a file is edited externally: [#2752](https://github.com/owncloud/android/issues/2752)

   If an external editor modifies a file, the new size will not match when it is assembled in server
   side. Fixed by removing the if-match header from the proper place

   https://github.com/owncloud/android/issues/2752
   https://github.com/owncloud/android/pull/3220

* Bugfix - Fix navbar is visible in file preview screen after rotation: [#3184](https://github.com/owncloud/android/pull/3184)

   Glitch was fixed where the navigation bar became visible in a file preview screen when rotating
   the device.

   https://github.com/owncloud/android/issues/3139
   https://github.com/owncloud/android/pull/3184

* Bugfix - Fix a bug when some fields where not retrieved from OIDC Discovery: [#3202](https://github.com/owncloud/android/pull/3202)

   Problem when requesting the OIDC discovery was fixed. Some fields were handled as mandatory,
   but they are recommended according to the docs. It prevented from a proper login. Now it is
   possible to login as expected when some fields are not retrieved.

   https://github.com/owncloud/android/pull/3202
   https://github.com/owncloud/android-library/pull/392

* Bugfix - Fix permissions were displayed in share creation view after rotation: [#3204](https://github.com/owncloud/android/issues/3204)

   Permissions view was shown when creating a share for a file after rotation. Capabilities were
   taken into account just once. Now, the permissions view is shown only when capabilities match.

   https://github.com/owncloud/android/issues/3204
   https://github.com/owncloud/android/pull/3234

* Change - Error handling for pattern lock: [#3215](https://github.com/owncloud/android/issues/3215)

   Error messages when an incorrect pattern was entered were shown in a snackbar. Now, they are
   displayed in a text below the pattern input, just like in the passcode screen.

   https://github.com/owncloud/android/issues/3215
   https://github.com/owncloud/android/pull/3221

* Change - Hide biometrical if device does not support it: [#3217](https://github.com/owncloud/android/issues/3217)

   Biometric lock preference in "Security" settings subsection was shown even when the device
   didn't support biometrics (if it was Android 6.0 or later versions). Now, the preference is
   only shown if the device has the suitable hardware for it.

   https://github.com/owncloud/android/issues/3217
   https://github.com/owncloud/android/pull/3230

* Enhancement - Settings accessible even when no account is attached: [#2638](https://github.com/owncloud/android/issues/2638)

   Now, settings can be accessed via a button in the login screen, removing the necessity to have an
   attached account. However, auto picture and video uploads won't be available until an account
   is registered in the app.

   https://github.com/owncloud/android/issues/2638
   https://github.com/owncloud/android/pull/3218

* Enhancement - Support for apk files: [#2691](https://github.com/owncloud/android/issues/2691)

   Apk files could be installed from the app after being downloaded. Installation process will be
   triggered by the system.

   https://github.com/owncloud/android/issues/2691
   https://github.com/owncloud/android/pull/3156
   https://github.com/owncloud/android/pull/3162

* Enhancement - Move to AndroidX Preference and new structure for settings: [#2867](https://github.com/owncloud/android/issues/2867)

   Settings have been updated to use the current Android's recommendation, AndroidX framework.
   In addition, they have been reorganized into subsections for a better understanding and
   navigation structure. Also, new features have been added: now, source path and behaviour in
   auto uploads can be chosen differently for pictures and videos.

   https://github.com/owncloud/android/issues/2867
   https://github.com/owncloud/android/pull/3143

* Enhancement - Replace blank view in music player with cover art: [#3121](https://github.com/owncloud/android/issues/3121)

   Blank view in the music preview player with styled up cover art was replaced. For music files
   that does not have cover art embodied, it is displayed a placeholder.

   https://github.com/owncloud/android/issues/3121
   https://github.com/owncloud/android/pull/3182

* Enhancement - Align previews actions: [#3155](https://github.com/owncloud/android/issues/3155)

   Behaviour was aligned through every preview fragment. Images, videos, audios and texts show
   the same actions now.

   https://github.com/owncloud/android/issues/3155
   https://github.com/owncloud/android/pull/3177

* Enhancement - Fixed account for camera uploads: [#3166](https://github.com/owncloud/android/issues/3166)

   Camera uploads will be uploaded to a fixed account independently of the current account.
   Removing the account attached to camera uploads will disable this feature. User will be warned
   when removing an account that has camera uploads attached.

   https://github.com/owncloud/android/issues/3166
   https://github.com/owncloud/android/pull/3226

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
