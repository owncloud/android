==============================
Using the ownCloud Android App
==============================

Accessing your files on your ownCloud server via the Web interface is easy and
convenient, as you can use any Web browser on any operating system without
installing special client software. 
However, the ownCloud Android app offers some advantages over the Web
interface:

* A simplified interface that fits nicely on a tablet or smartphone
* Automatic synchronization of your files
* Share files with other ownCloud users and groups, and create public share links
* Instant uploads of photos or videos recorded on your Android device
* Easily add files from your device to ownCloud
* Two-factor authentication

Getting the ownCloud Android App
--------------------------------

One way to get your ownCloud Android app is to log into your ownCloud server
from your Android device using a Web browser such as Chrome, Firefox, or
Dolphin. 

The first time you log into a new ownCloud account, you'll see a screen with
a download link to the ownCloud app in the `Google Play Store
<https://play.google.com/store/apps/details?id=com.owncloud.android>`_.

.. figure:: images/android-1.png
   :alt: Android app new account welcome screen.

You will also find these links on your Personal page in the ownCloud Web
interface. 
Find source code and more information from the `ownCloud download page
<http://owncloud.org/install/#mobile>`_.
Users of customized ownCloud Android apps, for example from their employer,
should follow their employer's instructions.

Upgrading
---------

When you download your ownCloud Android App from the Google Play store, it will
be upgraded just like any other Play Store application, according to your
settings on your Android device. 

It will either update automatically or give you a notification that an upgrade
is available. If you are using an ownCloud Android app from a custom
repository, e.g., your employer, then it will update in accordance with their
policies.

Connecting to Your ownCloud Server
----------------------------------

The first time you run your ownCloud Android app, it opens to a configuration
screen. 
Enter your server URL, login name, password, and click the Connect button. 
Click the eyeball to the right of your password to expose your password.

.. figure:: images/android-2.png
   :alt: New account creation screen.

For best security, your ownCloud server should be SSL-enabled so that you can
connect via ``https``. 
The ownCloud app will test your connection as soon as you provide it and tell
you if you entered it correctly. 

If your server has a self-signed SSL certificate, you'll get a warning that it
is not to be trusted. 
If this happens, click the OK button to accept the certificate and complete
your account setup.

.. figure:: images/android-3.png 
   :alt: SSL certificate warning.

With that completed, you're now ready to use the Android application. 
At this point, you'll be on the All files screen, which you see below.

.. figure:: images/android-all-files-overview.png 
   :alt: All files screen.

By clicking the main menu at the top left, you will be able to manage the core functionality of the app. The options are:

- Manage Users Accounts
- Current **Uploads** 
- All Files
- Application **Settings**

Manage Users Accounts
~~~~~~~~~~~~~~~~~~~~~

Initially the path to this section isn't visible. 
To get to it, first click the down arrow, in the user details section, which
will replace the "All files" and "Uploads" buttons with "Add account" and
"Manage accounts". 
Then, click "Manage accounts". 
From there, you can see all of the currently active user accounts, along with a button to add a new account.

User Accounts
^^^^^^^^^^^^^

After clicking "Manage Accounts", you will see a list of the currently active
accounts in the application, as in the screenshot below. 
Each entry in the list has shortcuts to:

- Viewing the user's files
- Changing the user's password
- Removing the account

.. figure:: images/android-6.png
   :alt: Top-right menu.   

Change User Password
^^^^^^^^^^^^^^^^^^^^

To change a user's password, click the key icon, next to the user's details. 
This will display the user details page, with the ownCloud server URI and user account, pre-filled.
Enter a new password, and click "Connect", and the password will be updated.

.. figure:: images/android-13.png
   :alt: Change password or remove account dialog.

If you want extra security, please refer to the `Passcode Locks & Pins`_ section.

Removing An Account / Logging Out
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To remove an account, click the rubbish bin icon, next to the key icon. 
This will display a confirmation dialog, asking if you want to remove the account.
If you click "Yes", the account will be removed. 
This action logs you out of the server deletes the database with the list of files. 
But, any files that were downloaded onto the device at the moment of the removal will still be there afterwards
You can find them in the public partition.

.. figure:: images/android-6.png
   :alt: Top-right menu.   

.. NOTE:: 
   There is no logout function that both logs out of your account **and** leaves
   all your files on your Android device. 

Add Account
^^^^^^^^^^^

To add a new account is identical to creating the first account. 
Click "Add account", and then follow the instructions in `Connecting to Your ownCloud Server`_.

Current Uploads
~~~~~~~~~~~~~~~

The Uploads page displays the status of files currently uploading, a list of your recently uploaded files, and a Retry option for any failed uploads. 
If credentials to access the file have changed, you'll see a credentials error. 
Tap the file to retry, and you'll get a login screen to enter the new credentials. 

If the upload fails because you're trying to upload to a folder that you do not have permission to access, you will see a "Permissions error." 
Change the permissions on the folder and retry the upload, or cancel and then upload the file to a different folder.

.. figure:: images/android-15.png
   :alt: Top-left menu.

All Files
~~~~~~~~~

When you are on the All Files view, all files that you have permission to
access on your ownCloud server are displayed in your Android app. 
But, they are not downloaded until you click on them. 
Downloaded files are marked with a green tick, on the top-right of the file's
icon.

.. figure:: images/android-all-files-view.jpg
   :alt: Downloaded files are marked with green ticks.

Download and view a file with a short press on the file's name or icon.  
Then, a short press on the overflow button opens a menu with options for
managing your file.

.. figure:: images/android-file-overflow-menu.jpg
   :alt: File management options.
   
When you are on your main Files page, a long press on any file or folder
displays a list of options, which you can see in the image below. 

.. figure:: images/android-file-list-overflow-menu.jpg
   :alt: Folder and file management options.

Creating New Content
^^^^^^^^^^^^^^^^^^^^

To add new content, whether files, folders, or content from other apps, click the blue button at the bottom right to expose the **Upload**, **Content from other apps**, and **New folder** buttons.

Use the **Upload** button to add files to your ownCloud account from your Android filesystem. 
Use **Content from other apps** to upload files from Android apps, such as the Gallery app.

.. figure:: images/android-4.png 
   :alt: Your ownCloud Files page.
   
Click the overflow button at the top right (that's the one with three vertical dots) to open a user menu. 
**Grid view** toggles between grid and list view. **Refresh account** syncs with the server, and **Sort** 
gives you the option to sort your files by date, or alphabetically.

.. figure:: images/android-6.png
   :alt: Top-right menu.   
  
Sharing Files
^^^^^^^^^^^^^

You can share with other ownCloud users and groups, and create public share
links. 
To share a file, you first need to either:

1. Long-click its name, and click the share icon at the top of the screen 
2. Click its name and then click  the share icon at the top of the screen

The dialog which appears shows a list of users with whom the file is already
shared. 

.. figure:: images/android-12.png
   :alt: Sharing files.

From here you can:

- Share the file with one or more users and groups
- Share a link to the file via a range of options
- Enable password protection
- Set a share expiration date

To share the file with a new user or group, click "Add User Or Group", where
you will be able to enter their details. 

.. NOTE:: 
   If your ownCloud server administrator has enabled username auto-completion,
   when you start typing user or group names they will auto-complete. 
   
You can create a Federated Share Link by entering the username and remote URL
of the person you want to share with in this format: ``user@domain.com``. 
You don't have to guess; the Personal page in the ownCloud Web GUI tells the
exact Federated Cloud ID. 
Just ask them to copy and paste and send it to you.

.. figure:: images/android-14.png
   :alt: Federated share creation.
   
Application Settings
~~~~~~~~~~~~~~~~~~~~

Use the **Settings** screen to control your ownCloud applications settings and functionality. 

.. figure:: images/android-settings-page.jpg
   :alt: the Settings screen.

Instant Uploads
^^^^^^^^^^^^^^^

If you take photos or create videos with your Android device, they can be
instantly uploaded to your ownCloud server. 
To enable this, under "Instant Uploads" tap one or both of:

- "Instant picture uploads" 
- "Instant video uploads"

.. figure:: images/android-settings-instant-upload.png
   :alt: the Settings screen.

These start the process of uploading any new photos and/or videos which you
create.
If you’re concerned about mobile data usage, or have an account with limited
data available, you can limit uploading to only when a WiFi is in use. 
This option is visible once you've enabled the respective option.
For photos tap **"Upload pictures via wifi only"**:sup:`1`. 
For videos tap **"Upload videos via wifi only"**:sup:`2`

.. figure:: images/android-settings-enable-instant-upload.png
   :alt: Enabling instant upload picture and video 

By default, photos and videos are uploaded to a directory called
file:`/InstantUpload`. 
However, you also have the option to choose any other existing directory, or
to create a new one. 
To change the upload location, tap on **"Upload path"** under either photos or
videos, and choose one of the folders which is displayed. 

To create a new folder, click the three dots in the top right-hand corner. 
This will display the menu option: **"New folder"**. 
Tap it and enter the name of the new folder in the **"Folder name"** dialog.
Then, tap the newly created folder and tap **"Choose"** in the bottom right-hand
corner. 
You'll see that the path has been updated.

Passcode Locks & Pins
^^^^^^^^^^^^^^^^^^^^^

You can also set a passcode lock to further protect your files and folders.
And, if you want extra security, you can set a login PIN on your Android device, and also on your ownCloud account. 
If you are using a shared Android device, other users can access your files in the file manager if you are sharing a single user account. 
To avoid this, you could set up multiple user accounts to protect your files.

The bottom section of the **Settings** screen has links to:

- Help
- Recommend to a friend**
- Feedback 
- The version number
