Changelog for ownCloud Android Client [unreleased] (UNRELEASED)
=======================================
The following sections list the changes in ownCloud Android Client unreleased relevant to
ownCloud admins and users.

[unreleased]: https://github.com/owncloud/android/compare/v2.18.1...master

Summary
-------

* Bugfix - Lack of back button in Logs view: [#3357](https://github.com/owncloud/android/issues/3357)
* Bugfix - Passcode input misbehaving: [#3342](https://github.com/owncloud/android/issues/3342)
* Bugfix - ANR after removing account with too many downloaded files: [#3362](https://github.com/owncloud/android/issues/3362)
* Bugfix - Account removed is not removed from the drawer: [#3340](https://github.com/owncloud/android/issues/3340)
* Bugfix - Crash in FileDataStorageManager: [#2896](https://github.com/owncloud/android/issues/2896)
* Enhancement - Delete old user directories in order to free memory: [#3336](https://github.com/owncloud/android/pull/3336)
* Enhancement - Delete old logs every week: [#3328](https://github.com/owncloud/android/issues/3328)
* Enhancement - Lock delay for app: [#3344](https://github.com/owncloud/android/issues/3344)
* Enhancement - Instant upload only when charging: [#465](https://github.com/owncloud/android/issues/465)

Details
-------

* Bugfix - Lack of back button in Logs view: [#3357](https://github.com/owncloud/android/issues/3357)

   A new back arrow button has been added in the toolbar in Logs screen, so that now it's possible to
   return to the settings screen without the use of physical buttons of the device.

   https://github.com/owncloud/android/issues/3357
   https://github.com/owncloud/android/pull/3363

* Bugfix - Passcode input misbehaving: [#3342](https://github.com/owncloud/android/issues/3342)

   Passcode text fields have been made not selectable once a number is written on them, so that we
   avoid bugs with the digits of the passcode and the way of entering them.

   https://github.com/owncloud/android/issues/3342
   https://github.com/owncloud/android/pull/3365

* Bugfix - ANR after removing account with too many downloaded files: [#3362](https://github.com/owncloud/android/issues/3362)

   Previously, when a user account was deleted, the application could freeze when trying to
   delete a large number of files. Now, the application has been fixed so that it doesn't freeze
   anymore by doing this.

   https://github.com/owncloud/android/issues/3362
   https://github.com/owncloud/android/pull/3380

* Bugfix - Account removed is not removed from the drawer: [#3340](https://github.com/owncloud/android/issues/3340)

   When an account was deleted from the device settings, in the accounts section, it was not
   removed from the Navigation Drawer. Now, when deleting an account from there, the Navigation
   Drawer is refreshed and the removed account is no more shown.

   https://github.com/owncloud/android/issues/3340
   https://github.com/owncloud/android/pull/3381

* Bugfix - Crash in FileDataStorageManager: [#2896](https://github.com/owncloud/android/issues/2896)

   A possible null value with the account that caused certain crashes on Android 10 devices has
   been controlled.

   https://github.com/owncloud/android/issues/2896
   https://github.com/owncloud/android/pull/3383

* Enhancement - Delete old user directories in order to free memory: [#3336](https://github.com/owncloud/android/pull/3336)

   Previously, when users deleted an account the synchronized files of this account stayed on the
   SD-Card. So if the user didn't want them anymore he had to delete them manually. Now, the app
   automatically removes the files associated with an account.

   https://github.com/owncloud/android/issues/125
   https://github.com/owncloud/android/pull/3336

* Enhancement - Delete old logs every week: [#3328](https://github.com/owncloud/android/issues/3328)

   Previously, logs were stored but never deleted. It used a lot of storage when logs were enabled
   for some time. Now, the logs are removed periodically every week.

   https://github.com/owncloud/android/issues/3328
   https://github.com/owncloud/android/pull/3337

* Enhancement - Lock delay for app: [#3344](https://github.com/owncloud/android/issues/3344)

   A new preference has been added to choose the interval in which the app will be unlocked after
   having unlocked it once, making it more comfortable for those who access the app frequently and
   have a security lock set.

   https://github.com/owncloud/android/issues/3344
   https://github.com/owncloud/android/pull/3375

* Enhancement - Instant upload only when charging: [#465](https://github.com/owncloud/android/issues/465)

   A new option has been added in the auto upload pictures/videos screen, so that now it's possible
   to upload pictures or videos only when charging.

   https://github.com/owncloud/android/issues/465
   https://github.com/owncloud/android/issues/3315
   https://github.com/owncloud/android/pull/3385

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

* Bugfix - Fix navbar is visible in file preview screen after rotation: [#3184](https://github.com/owncloud/android/pull/3184)
* Bugfix - Fix a bug when some fields where not retrieved from OIDC Discovery: [#3202](https://github.com/owncloud/android/pull/3202)
* Bugfix - Snackbar in passcode view is not displayed: [#2722](https://github.com/owncloud/android/issues/2722)
* Bugfix - Fixed problem when a file is edited externally: [#2752](https://github.com/owncloud/android/issues/2752)
* Bugfix - Fix permissions were displayed in share creation view after rotation: [#3204](https://github.com/owncloud/android/issues/3204)
* Change - Error handling for pattern lock: [#3215](https://github.com/owncloud/android/issues/3215)
* Change - Hide biometrical if device does not support it: [#3217](https://github.com/owncloud/android/issues/3217)
* Enhancement - Replace blank view in music player with cover art: [#3121](https://github.com/owncloud/android/issues/3121)
* Enhancement - Move to AndroidX Preference and new structure for settings: [#2867](https://github.com/owncloud/android/issues/2867)
* Enhancement - Support for apk files: [#2691](https://github.com/owncloud/android/issues/2691)
* Enhancement - Align previews actions: [#3155](https://github.com/owncloud/android/issues/3155)
* Enhancement - Settings accessible even when no account is attached: [#2638](https://github.com/owncloud/android/issues/2638)
* Enhancement - Fixed account for camera uploads: [#3166](https://github.com/owncloud/android/issues/3166)

Details
-------

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

* Enhancement - Replace blank view in music player with cover art: [#3121](https://github.com/owncloud/android/issues/3121)

   Blank view in the music preview player with styled up cover art was replaced. For music files
   that does not have cover art embodied, it is displayed a placeholder.

   https://github.com/owncloud/android/issues/3121
   https://github.com/owncloud/android/pull/3182

* Enhancement - Move to AndroidX Preference and new structure for settings: [#2867](https://github.com/owncloud/android/issues/2867)

   Settings have been updated to use the current Android's recommendation, AndroidX framework.
   In addition, they have been reorganized into subsections for a better understanding and
   navigation structure. Also, new features have been added: now, source path and behaviour in
   auto uploads can be chosen differently for pictures and videos.

   https://github.com/owncloud/android/issues/2867
   https://github.com/owncloud/android/pull/3143

* Enhancement - Support for apk files: [#2691](https://github.com/owncloud/android/issues/2691)

   Apk files could be installed from the app after being downloaded. Installation process will be
   triggered by the system.

   https://github.com/owncloud/android/issues/2691
   https://github.com/owncloud/android/pull/3156
   https://github.com/owncloud/android/pull/3162

* Enhancement - Align previews actions: [#3155](https://github.com/owncloud/android/issues/3155)

   Behaviour was aligned through every preview fragment. Images, videos, audios and texts show
   the same actions now.

   https://github.com/owncloud/android/issues/3155
   https://github.com/owncloud/android/pull/3177

* Enhancement - Settings accessible even when no account is attached: [#2638](https://github.com/owncloud/android/issues/2638)

   Now, settings can be accessed via a button in the login screen, removing the necessity to have an
   attached account. However, auto picture and video uploads won't be available until an account
   is registered in the app.

   https://github.com/owncloud/android/issues/2638
   https://github.com/owncloud/android/pull/3218

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
  + Fix ocasional crash when opening share by link
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
