## 2.6.0 (January 2018)
- Camera uploads, replacing instant uploads (Android 6 or higher required)
- Android O support
- Notification channels (Android O required)
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
