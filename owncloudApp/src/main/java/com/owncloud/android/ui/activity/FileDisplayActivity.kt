/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.Manifest
import android.accounts.Account
import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.AppRater
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.authentication.PatternManager
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.files.services.TransferRequester
import com.owncloud.android.lib.common.authentication.OwnCloudBearerCredentials
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.syncadapter.FileSyncAdapter
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.ui.helpers.FilesUploadHelper
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoActivity
import com.owncloud.android.ui.preview.PreviewVideoFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.Extras
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.android.synthetic.main.nav_coordinator_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import kotlin.coroutines.CoroutineContext

/**
 * Displays, what files the user has available in his ownCloud. This is the main view.
 */

class FileDisplayActivity : FileActivity(), FileFragment.ContainerActivity, OnEnforceableRefreshListener,
    CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var syncBroadcastReceiver: SyncBroadcastReceiver? = null
    private var uploadBroadcastReceiver: UploadBroadcastReceiver? = null
    private var downloadBroadcastReceiver: DownloadBroadcastReceiver? = null
    private var lastSslUntrustedServerResult: RemoteOperationResult<*>? = null

    private var leftFragmentContainer: View? = null
    private var rightFragmentContainer: View? = null
    private var selectAllMenuItem: MenuItem? = null
    private var mainMenu: Menu? = null

    private var fileWaitingToPreview: OCFile? = null

    private var syncInProgress = false

    private var fileListOption = FileListOption.ALL_FILES
    private var waitingToSend: OCFile? = null

    private var localBroadcastManager: LocalBroadcastManager? = null

    var filesUploadHelper: FilesUploadHelper? = null
        internal set

    private val listOfFilesFragment: OCFileListFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as OCFileListFragment?

    private val secondFragment: FileFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SECOND_FRAGMENT) as FileFragment?

    private val isFabOpen: Boolean
        get() = listOfFilesFragment?.fabMain?.isExpanded ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate() start")

        super.onCreate(savedInstanceState) // this calls onAccountChanged() when ownCloud Account is valid

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        /// Load of saved instance state
        if (savedInstanceState != null) {
            Timber.d(savedInstanceState.toString())

            fileWaitingToPreview = savedInstanceState.getParcelable(KEY_WAITING_TO_PREVIEW)
            syncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS)
            waitingToSend = savedInstanceState.getParcelable(KEY_WAITING_TO_SEND)
            filesUploadHelper = savedInstanceState.getParcelable(KEY_UPLOAD_HELPER)
            fileListOption =
                savedInstanceState.getParcelable(KEY_FILE_LIST_OPTION) as? FileListOption ?: FileListOption.ALL_FILES
            if (account != null) {
                filesUploadHelper?.init(this, account.name)
            }
        } else {
            fileWaitingToPreview = null
            syncInProgress = false
            waitingToSend = null

            fileListOption =
                intent.getParcelableExtra(EXTRA_FILE_LIST_OPTION) as? FileListOption ?: FileListOption.ALL_FILES

            filesUploadHelper = FilesUploadHelper(
                this,
                if (account == null) "" else account.name
            )
        }

        /// USER INTERFACE

        // Inflate and set the layout view
        setContentView(R.layout.activity_main)

        // setup toolbar
        setupRootToolbar(
            isSearchEnabled = true,
            title = getString(R.string.default_display_name_for_root_folder),
        )

        // setup drawer
        setupDrawer()

        setupNavigationBottomBar(getMenuItemForFileListOption(fileListOption))

        leftFragmentContainer = findViewById(R.id.left_fragment_container)
        rightFragmentContainer = findViewById(R.id.right_fragment_container)

        // Init Fragment without UI to retain AsyncTask across configuration changes
        val fm = supportFragmentManager
        var taskRetainerFragment =
            fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT) as TaskRetainerFragment?
        if (taskRetainerFragment == null) {
            taskRetainerFragment = TaskRetainerFragment()
            fm.beginTransaction()
                .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit()
        }   // else, Fragment already created and retained across configuration change

        Timber.v("onCreate() end")

        if (resources.getBoolean(R.bool.enable_rate_me_feature) && !MainApp.isDeveloper) {
            AppRater.appLaunched(this, packageName)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show explanation to the user and then request permission
                val snackbar = Snackbar.make(
                    findViewById(R.id.list_layout),
                    R.string.permission_storage_access,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) { PermissionUtil.requestWriteExternalStoreagePermission(this@FileDisplayActivity) }

                DisplayUtils.colorSnackbar(this, snackbar)

                snackbar.show()
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestWriteExternalStoreagePermission(this)
            }
        }

        if (savedInstanceState == null) {
            createMinFragments()
        }

        setBackgroundText()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionUtil.PERMISSIONS_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startSyncFolderOperation(file, false)
                }
                // If permission denied --> do nothing
                return
            }
        }
    }

    /**
     * Called when the ownCloud [Account] associated to the Activity was just updated.
     */
    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)
        if (account != null) {
            /// Check whether the 'main' OCFile handled by the Activity is contained in the
            // current Account
            var file: OCFile? = file
            // get parent from path
            val parentPath: String
            if (file != null) {
                if (file.isDown && file.lastSyncDateForProperties == 0L) {
                    // upload in progress - right now, files are not inserted in the local
                    // cache until the upload is successful get parent from path
                    parentPath = file.remotePath.substring(
                        0,
                        file.remotePath.lastIndexOf(file.fileName)
                    )
                    if (storageManager.getFileByPath(parentPath) == null) {
                        file = null // not able to know the directory where the file is uploading
                    }
                } else {
                    file = storageManager.getFileByPath(file.remotePath)
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = storageManager.getFileByPath(OCFile.ROOT_PATH)  // never returns null
            }
            setFile(file)

            if (mAccountWasSet) {
                setAccountInDrawer(account)
            }

            if (!stateWasRecovered) {
                Timber.d("Initializing Fragments in onAccountChanged..")
                initFragmentsWithFile()
                file?.isFolder?.let { isFolder ->
                    if (isFolder) {
                        startSyncFolderOperation(file, false)
                    }
                }

            } else {
                file?.isFolder?.let { isFolder ->
                    updateFragmentsVisibility(!isFolder)
                    updateToolbar(if (isFolder) null else file)
                }
            }
        }
    }

    private fun createMinFragments() {
        val listOfFiles = OCFileListFragment.newInstance(false, fileListOption, false, false, true)
        listOfFiles.setSearchListener(findViewById(R.id.root_toolbar_search_view))
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES)
        transaction.commit()
    }

    private fun initFragmentsWithFile() {
        if (account != null && file != null) {
            /// First fragment
            listOfFilesFragment?.listDirectory(currentDir)
                ?: Timber.e("Still have a chance to lose the initialization of list fragment >(")

            /// Second fragment
            val file = file
            val secondFragment = chooseInitialSecondFragment(file)
            secondFragment?.let {
                setSecondFragment(it)
                updateFragmentsVisibility(true)
                updateToolbar(file)
            } ?: cleanSecondFragment()

        } else {
            Timber.e("initFragmentsWithFile() called with invalid nulls! account is $account, file is $file")
        }
    }

    /**
     * Choose the second fragment that is going to be shown
     *
     * @param file used to decide which fragment should be chosen
     * @return a new second fragment instance if it has not been chosen before, or the fragment
     * previously chosen otherwhise
     */
    private fun chooseInitialSecondFragment(file: OCFile?): Fragment? {

        var secondFragment = supportFragmentManager.findFragmentByTag(TAG_SECOND_FRAGMENT)

        if (secondFragment == null) { // If second fragment has not been chosen yet, choose it
            if (file != null && !file.isFolder) {
                if ((PreviewAudioFragment.canBePreviewed(file) || PreviewVideoFragment.canBePreviewed(file)) && file.lastSyncDateForProperties > 0  // temporal fix
                ) {
                    val startPlaybackPosition = intent.getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0)
                    val autoplay = intent.getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true)

                    if (PreviewAudioFragment.canBePreviewed(file)) {

                        secondFragment = PreviewAudioFragment.newInstance(
                            file,
                            account,
                            startPlaybackPosition,
                            autoplay
                        )

                    } else {

                        secondFragment = PreviewVideoFragment.newInstance(
                            file,
                            account,
                            startPlaybackPosition,
                            autoplay
                        )
                    }

                } else if (PreviewTextFragment.canBePreviewed(file)) {
                    secondFragment = PreviewTextFragment.newInstance(
                        file,
                        account
                    )

                } else {
                    secondFragment = FileDetailFragment.newInstance(file, account)
                }
            }
        }

        return secondFragment
    }

    /**
     * Replaces the second fragment managed by the activity with the received as
     * a parameter.
     *
     *
     * Assumes never will be more than two fragments managed at the same time.
     *
     * @param fragment New second Fragment to set.
     */
    private fun setSecondFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.right_fragment_container, fragment, TAG_SECOND_FRAGMENT)
        transaction.commit()
    }

    fun showBottomNavBar(show: Boolean) {
        bottom_nav_view.isVisible = show
    }

    private fun updateFragmentsVisibility(existsSecondFragment: Boolean) {
        if (existsSecondFragment) {
            if (leftFragmentContainer?.visibility != View.GONE) {
                leftFragmentContainer?.visibility = View.GONE
            }
            if (rightFragmentContainer?.visibility != View.VISIBLE) {
                rightFragmentContainer?.visibility = View.VISIBLE
            }
            showBottomNavBar(show = false)
        } else {
            if (leftFragmentContainer?.visibility != View.VISIBLE) {
                leftFragmentContainer?.visibility = View.VISIBLE
                showBottomNavBar(show = true)
            }
            if (rightFragmentContainer?.visibility != View.GONE) {
                rightFragmentContainer?.visibility = View.GONE
            }
        }
    }

    private fun cleanSecondFragment() {
        val second = secondFragment
        if (second != null) {
            val tr = supportFragmentManager.beginTransaction()
            tr.remove(second)
            tr.commit()
        }
        updateFragmentsVisibility(false)
        updateToolbar(null)
    }

    fun refreshListOfFilesFragment(reloadData: Boolean) {
        val fileListFragment = listOfFilesFragment
        fileListFragment?.listDirectory(reloadData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater

        // Allow or disallow touches with other visible windows
        val actionBarView = findViewById<View>(R.id.action_bar)
        if (actionBarView != null) {
            actionBarView.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(applicationContext)
        }

        inflater.inflate(R.menu.main_menu, menu)

        selectAllMenuItem = menu.findItem(R.id.action_select_all)
        if (secondFragment == null) {
            selectAllMenuItem?.isVisible = true
        }
        mainMenu = menu

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_select_all -> {
                listOfFilesFragment?.selectAll()
            }
            android.R.id.home -> {
                val second = secondFragment
                val currentDir = currentDir

                val inRootFolder = currentDir != null && currentDir.parentId == 0L
                val fileFragmentVisible = second != null && second.file != null

                if (!inRootFolder || fileFragmentVisible) {
                    onBackPressed()
                } else if (isDrawerOpen()) {
                    closeDrawer()
                } else {
                    openDrawer()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Called, when the user selected something for uploading
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BiometricManager.getBiometricManager(this).bayPassUnlockOnce()
        }
        PassCodeManager.getPassCodeManager().bayPassUnlockOnce()
        PatternManager.getPatternManager().bayPassUnlockOnce()

        // Hanndle calls form internal activities.
        if (requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS && (resultCode == Activity.RESULT_OK || resultCode == RESULT_OK_AND_MOVE)) {

            requestUploadOfContentFromApps(data, resultCode)

        } else if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA) {
            if (resultCode == Activity.RESULT_OK || resultCode == RESULT_OK_AND_MOVE) {
                filesUploadHelper?.onActivityResult(object : FilesUploadHelper.OnCheckAvailableSpaceListener {
                    override fun onCheckAvailableSpaceStart() {

                    }

                    override fun onCheckAvailableSpaceFinished(
                        hasEnoughSpace: Boolean,
                        capturedFilePaths: Array<String>
                    ) {
                        if (hasEnoughSpace) {
                            requestUploadOfFilesFromFileSystem(capturedFilePaths, FileUploader.LOCAL_BEHAVIOUR_MOVE)
                        }
                    }
                })
            } else if (requestCode == Activity.RESULT_CANCELED) {
                filesUploadHelper?.deleteImageFile()
            }

            // requestUploadOfFilesFromFileSystem(data,resultCode);
        } else if (requestCode == REQUEST_CODE__MOVE_FILES && resultCode == Activity.RESULT_OK) {
            handler.postDelayed(
                { requestMoveOperation(data!!) },
                DELAY_TO_REQUEST_OPERATIONS_LATER
            )

        } else if (requestCode == REQUEST_CODE__COPY_FILES && resultCode == Activity.RESULT_OK) {
            handler.postDelayed(
                { requestCopyOperation(data!!) },
                DELAY_TO_REQUEST_OPERATIONS_LATER
            )
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requestUploadOfFilesFromFileSystem(filePaths: Array<String>?, behaviour: Int) {
        if (filePaths != null) {
            val remotePaths = arrayOfNulls<String>(filePaths.size)
            val remotePathBase = currentDir?.remotePath
            for (j in remotePaths.indices) {
                remotePaths[j] = remotePathBase + File(filePaths[j]).name
            }

            val requester = TransferRequester()
            requester.uploadNewFiles(
                this,
                account,
                filePaths,
                remotePaths, null, // MIME type will be detected from file name
                behaviour,
                false, // do not create parent folder if not existent
                UploadFileOperation.CREATED_BY_USER
            )

        } else {
            Timber.d("User clicked on 'Update' with no selection")
            showMessageInSnackbar(R.id.list_layout, getString(R.string.filedisplay_no_file_selected))
        }
    }

    private fun requestUploadOfContentFromApps(contentIntent: Intent?, resultCode: Int) {

        val streamsToUpload = ArrayList<Uri>()

        if (contentIntent!!.clipData != null && contentIntent.clipData!!.itemCount > 0) {
            for (i in 0 until contentIntent.clipData!!.itemCount) {
                streamsToUpload.add(contentIntent.clipData!!.getItemAt(i).uri)
            }
        } else {
            streamsToUpload.add(contentIntent.data!!)
        }

        val behaviour = if (resultCode == RESULT_OK_AND_MOVE)
            FileUploader.LOCAL_BEHAVIOUR_MOVE
        else
            FileUploader.LOCAL_BEHAVIOUR_COPY

        val currentDir = currentDir
        val remotePath = if (currentDir != null) currentDir.remotePath else OCFile.ROOT_PATH

        val uploader = UriUploader(
            this,
            streamsToUpload,
            remotePath,
            account,
            behaviour,
            false, null// Not needed copy temp task listener
        )// Not show waiting dialog while file is being copied from private storage

        uploader.uploadUris()
    }

    /**
     * Request the operation for moving the file/folder from one path to another
     *
     * @param data Intent received
     */
    private fun requestMoveOperation(data: Intent) {
        val folderToMoveAt = data.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER)
        val files = data.getParcelableArrayListExtra<OCFile>(FolderPickerActivity.EXTRA_FILES)
        fileOperationsHelper.moveFiles(files, folderToMoveAt)
    }

    /**
     * Request the operation for copying the file/folder from one path to another
     *
     * @param data Intent received
     */
    private fun requestCopyOperation(data: Intent) {
        val folderToMoveAt = data.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER)
        val files = data.getParcelableArrayListExtra<OCFile>(FolderPickerActivity.EXTRA_FILES)
        fileOperationsHelper.copyFiles(files, folderToMoveAt)
    }

    override fun onBackPressed() {
        val isFabOpen = isFabOpen

        /*
         * BackPressed priority/hierarchy:
         *    1. close drawer if opened
         *    2. close FAB if open (only if drawer isn't open)
         *    3. navigate up (only if drawer and FAB aren't open)
         */
        if (isDrawerOpen() && isFabOpen) {
            // close drawer first
            super.onBackPressed()
        } else if (isDrawerOpen() && !isFabOpen) {
            // close drawer
            super.onBackPressed()
        } else if (!isDrawerOpen() && isFabOpen) {
            // close fab
            listOfFilesFragment?.fabMain?.collapse()
        } else {
            // all closed
            val listOfFiles = listOfFilesFragment
            if (secondFragment == null) {
                val currentDir = currentDir
                if (currentDir == null || currentDir.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong()) {
                    finish()
                    return
                }
                listOfFiles?.onBrowseUp()
            }
            if (listOfFiles != null) {  // should never be null, indeed
                file = listOfFiles.currentFile
                listOfFiles.listDirectory(false)
            }
            cleanSecondFragment()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Timber.v("onSaveInstanceState() start")
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_WAITING_TO_PREVIEW, fileWaitingToPreview)
        outState.putBoolean(KEY_SYNC_IN_PROGRESS, syncInProgress)
        outState.putParcelable(KEY_FILE_LIST_OPTION, fileListOption)
        //outState.putBoolean(KEY_REFRESH_SHARES_IN_PROGRESS,
        // mRefreshSharesInProgress);
        outState.putParcelable(KEY_WAITING_TO_SEND, waitingToSend)
        outState.putParcelable(KEY_UPLOAD_HELPER, filesUploadHelper)

        Timber.v("onSaveInstanceState() end")
    }

    override fun onResume() {
        Timber.v("onResume() start")
        super.onResume()

        setCheckedItemAtBottomBar(getMenuItemForFileListOption(fileListOption))

        // refresh list of files
        refreshListOfFilesFragment(true)

        // Listen for sync messages
        val syncIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
        syncBroadcastReceiver = SyncBroadcastReceiver()
        localBroadcastManager!!.registerReceiver(syncBroadcastReceiver!!, syncIntentFilter)

        // Listen for upload messages
        val uploadIntentFilter = IntentFilter(FileUploader.getUploadFinishMessage())
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage())
        uploadBroadcastReceiver = UploadBroadcastReceiver()
        localBroadcastManager!!.registerReceiver(uploadBroadcastReceiver!!, uploadIntentFilter)

        // Listen for download messages
        val downloadIntentFilter = IntentFilter(
            FileDownloader.getDownloadAddedMessage()
        )
        downloadIntentFilter.addAction(FileDownloader.getDownloadFinishMessage())
        downloadBroadcastReceiver = DownloadBroadcastReceiver()
        localBroadcastManager!!.registerReceiver(downloadBroadcastReceiver!!, downloadIntentFilter)

        Timber.v("onResume() end")
    }

    override fun onPause() {
        Timber.v("onPause() start")
        if (syncBroadcastReceiver != null) {
            localBroadcastManager!!.unregisterReceiver(syncBroadcastReceiver!!)
            syncBroadcastReceiver = null
        }
        if (uploadBroadcastReceiver != null) {
            localBroadcastManager!!.unregisterReceiver(uploadBroadcastReceiver!!)
            uploadBroadcastReceiver = null
        }
        if (downloadBroadcastReceiver != null) {
            localBroadcastManager!!.unregisterReceiver(downloadBroadcastReceiver!!)
            downloadBroadcastReceiver = null
        }

        super.onPause()
        Timber.v("onPause() end")
    }

    private inner class SyncBroadcastReceiver : BroadcastReceiver() {

        /**
         * [BroadcastReceiver] to enable syncing feedback in UI
         */
        override fun onReceive(context: Context, intent: Intent) {
            val event = intent.action
            Timber.d("Received broadcast $event")
            val accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME)

            val synchFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH)
            val serverVersion = intent.getParcelableExtra<OwnCloudVersion>(FileSyncAdapter.EXTRA_SERVER_VERSION)

            if (serverVersion != null && !serverVersion.isServerVersionSupported) {
                Timber.d("Server version not supported")
                showRequestAccountChangeNotice(getString(R.string.server_not_supported), true)
            }

            val synchResult = intent.getSerializableExtra(FileSyncAdapter.EXTRA_RESULT) as? RemoteOperationResult<*>
            val sameAccount = account != null && accountName == account.name && storageManager != null

            if (sameAccount) {

                if (FileSyncAdapter.EVENT_FULL_SYNC_START == event) {
                    syncInProgress = true

                } else {
                    var currentFile: OCFile? = if (file == null)
                        null
                    else
                        storageManager.getFileByPath(file.remotePath)
                    val currentDir = if (currentDir == null)
                        null
                    else
                        storageManager.getFileByPath(currentDir!!.remotePath)

                    if (currentDir == null) {
                        // current folder was removed from the server
                        showMessageInSnackbar(
                            R.id.list_layout,
                            String.format(
                                getString(R.string.sync_current_folder_was_removed),
                                synchFolderRemotePath
                            )
                        )
                        browseToRoot()

                    } else {
                        if (currentFile == null && !file.isFolder) {
                            // currently selected file was removed in the server, and now we
                            // know it
                            cleanSecondFragment()
                            currentFile = currentDir
                        }

                        if (synchFolderRemotePath != null && currentDir.remotePath == synchFolderRemotePath) {
                            val fileListFragment = listOfFilesFragment
                            fileListFragment?.listDirectory(true)
                        }
                        file = currentFile
                    }

                    syncInProgress =
                        FileSyncAdapter.EVENT_FULL_SYNC_END != event &&
                                RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED != event

                    if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED == event) {
                        if (!synchResult?.isSuccess!!) {
                            /// TODO refactor and make common
                            if (ResultCode.UNAUTHORIZED == synchResult.code ||
                                synchResult.isException && synchResult.exception is AuthenticatorException
                            ) {
                                launch(Dispatchers.IO) {
                                    val credentials =
                                        com.owncloud.android.lib.common.accounts.AccountUtils.getCredentialsForAccount(
                                            MainApp.appContext,
                                            account
                                        )

                                    launch(Dispatchers.Main) {
                                        if (credentials is OwnCloudBearerCredentials) { // OAuth
                                            showRequestRegainAccess()
                                        } else {
                                            showRequestAccountChangeNotice(
                                                getString(R.string.auth_failure_snackbar),
                                                false
                                            )
                                        }
                                    }
                                }
                            } else if (ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED == synchResult.code) {
                                showUntrustedCertDialog(synchResult)
                            }
                        }

                        if (synchFolderRemotePath == OCFile.ROOT_PATH) {
                            setAccountInDrawer(account)
                        }
                    }
                }

                val fileListFragment = listOfFilesFragment
                fileListFragment?.setProgressBarAsIndeterminate(syncInProgress)
                Timber.d("Setting progress visibility to $syncInProgress")

                setBackgroundText()
            }

            if (synchResult?.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                lastSslUntrustedServerResult = synchResult
            } else if (synchResult?.code == ResultCode.SPECIFIC_SERVICE_UNAVAILABLE) {
                if (synchResult.httpCode == 503) {
                    if (synchResult.httpPhrase == "Error: Call to a member function getUID() on null") {
                        showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                    } else {
                        showMessageInSnackbar(R.id.list_layout, synchResult.httpPhrase)
                    }
                } else {
                    showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                }
            }
        }
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    fun setBackgroundText() {
        val ocFileListFragment = listOfFilesFragment
        if (ocFileListFragment != null) {
            if (selectAllMenuItem != null) {
                selectAllMenuItem!!.isVisible = true
                if (ocFileListFragment.noOfItems == 0) {
                    selectAllMenuItem!!.isVisible = false
                }
            }
            var message = R.string.file_list_loading
            if (!syncInProgress) {
                // In case file list is empty
                message =
                    when (fileListOption) {
                        FileListOption.AV_OFFLINE -> R.string.file_list_empty_available_offline
                        FileListOption.SHARED_BY_LINK -> R.string.file_list_empty_shared_by_links
                        else -> R.string.file_list_empty
                    }
                ocFileListFragment.progressBar.visibility = View.GONE
                ocFileListFragment.shadowView.visibility = View.VISIBLE
            }
            ocFileListFragment.setMessageForEmptyList(getString(message))
        } else {
            Timber.e("OCFileListFragment is null")
        }
    }

    /**
     * Once the file upload has finished -> update view
     */
    private inner class UploadBroadcastReceiver : BroadcastReceiver() {
        /**
         * Once the file upload has finished -> update view
         *
         * @author David A. Velasco
         * [BroadcastReceiver] to enable upload feedback in UI
         */
        override fun onReceive(context: Context, intent: Intent) {
            val uploadedRemotePath = intent.getStringExtra(Extras.EXTRA_REMOTE_PATH)
            val accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME)
            val sameAccount = account != null && accountName == account.name
            val currentDir = currentDir
            val isDescendant = currentDir != null &&
                    uploadedRemotePath != null &&
                    uploadedRemotePath.startsWith(currentDir.remotePath)
            val renamedInUpload = file.remotePath == intent.getStringExtra(Extras.EXTRA_OLD_REMOTE_PATH)
            val sameFile = renamedInUpload || file.remotePath == uploadedRemotePath
            val success = intent.getBooleanExtra(Extras.EXTRA_UPLOAD_RESULT, false)

            if (sameAccount && isDescendant) {
                val linkedToRemotePath = intent.getStringExtra(Extras.EXTRA_LINKED_TO_PATH)
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    refreshListOfFilesFragment(true)
                }
            }

            if (sameAccount && sameFile) {
                if (success) {
                    file = storageManager.getFileByPath(uploadedRemotePath)
                }
                refreshSecondFragment(
                    intent.action,
                    success
                )
                if (renamedInUpload) {
                    val newName = File(uploadedRemotePath).name
                    showMessageInSnackbar(
                        R.id.list_layout,
                        String.format(getString(R.string.filedetails_renamed_in_upload_msg), newName)
                    )
                    updateToolbar(file)
                }
            }
        }

        private fun refreshSecondFragment(uploadEvent: String?, success: Boolean) {

            val secondFragment = secondFragment

            if (secondFragment != null) {
                if (!success && !file.fileExists()) {
                    cleanSecondFragment()
                } else {
                    val file = file
                    var fragmentReplaced = false
                    if (success && secondFragment is FileDetailFragment) {
                        // start preview if previewable
                        fragmentReplaced = true
                        when {
                            PreviewImageFragment.canBePreviewed(file) -> startImagePreview(file)
                            PreviewAudioFragment.canBePreviewed(file) -> startAudioPreview(file, 0)
                            PreviewVideoFragment.canBePreviewed(file) -> startVideoPreview(file, 0)
                            PreviewTextFragment.canBePreviewed(file) -> startTextPreview(file)
                            else -> fragmentReplaced = false
                        }
                    }
                    if (!fragmentReplaced) {
                        secondFragment.onSyncEvent(uploadEvent, success, file)
                    }
                }
            }
        }

        // TODO refactor this receiver, and maybe DownloadBroadcastReceiver; this method is duplicated :S
        private fun isAscendant(linkedToRemotePath: String): Boolean {
            val currentDir = currentDir
            return currentDir != null && currentDir.remotePath.startsWith(linkedToRemotePath)
        }
    }

    /**
     * Class waiting for broadcast events from the [FileDownloader] service.
     *
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private inner class DownloadBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val sameAccount = isSameAccount(intent)
            val downloadedRemotePath = intent.getStringExtra(Extras.EXTRA_REMOTE_PATH)
            val isDescendant = isDescendant(downloadedRemotePath)

            if (sameAccount && isDescendant) {
                val linkedToRemotePath = intent.getStringExtra(Extras.EXTRA_LINKED_TO_PATH)
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    refreshListOfFilesFragment(true)
                }
                refreshSecondFragment(
                    intent.action,
                    downloadedRemotePath,
                    intent.getBooleanExtra(Extras.EXTRA_DOWNLOAD_RESULT, false)
                )
                invalidateOptionsMenu()
            }

            if (waitingToSend != null) {
                waitingToSend = storageManager.getFileByPath(waitingToSend!!.remotePath)
                if (waitingToSend!!.isDown) {
                    sendDownloadedFile()
                }
            }
        }

        private fun isDescendant(downloadedRemotePath: String?): Boolean {
            val currentDir = currentDir
            return currentDir != null &&
                    downloadedRemotePath != null &&
                    downloadedRemotePath.startsWith(currentDir.remotePath)
        }

        private fun isAscendant(linkedToRemotePath: String): Boolean {
            val currentDir = currentDir
            return currentDir != null && currentDir.remotePath.startsWith(linkedToRemotePath)
        }

        private fun isSameAccount(intent: Intent): Boolean {
            val accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME)
            return accountName != null && account != null &&
                    accountName == account.name
        }

        private fun refreshSecondFragment(
            downloadEvent: String?, downloadedRemotePath: String,
            success: Boolean
        ) {
            val secondFragment = secondFragment
            if (secondFragment != null) {
                var fragmentReplaced = false
                if (secondFragment is FileDetailFragment) {
                    /// user was watching download progress
                    val detailsFragment = secondFragment as FileDetailFragment?
                    val fileInFragment = detailsFragment?.file
                    if (fileInFragment != null && downloadedRemotePath != fileInFragment.remotePath) {
                        // the user browsed to other file ; forget the automatic preview
                        fileWaitingToPreview = null

                    } else if (downloadEvent == FileDownloader.getDownloadFinishMessage()) {
                        //  replace the right panel if waiting for preview
                        val waitedPreview = fileWaitingToPreview?.remotePath == downloadedRemotePath
                        if (waitedPreview) {
                            if (success) {
                                // update the file from database, to get the local storage path
                                fileWaitingToPreview = storageManager.getFileById(fileWaitingToPreview!!.fileId)
                                when {
                                    PreviewAudioFragment.canBePreviewed(fileWaitingToPreview) -> {
                                        fragmentReplaced = true
                                        startAudioPreview(fileWaitingToPreview!!, 0)
                                    }
                                    PreviewVideoFragment.canBePreviewed(fileWaitingToPreview) -> {
                                        fragmentReplaced = true
                                        startVideoPreview(fileWaitingToPreview!!, 0)
                                    }
                                    PreviewTextFragment.canBePreviewed(fileWaitingToPreview) -> {
                                        fragmentReplaced = true
                                        startTextPreview(fileWaitingToPreview)
                                    }
                                    else -> fileOperationsHelper.openFile(fileWaitingToPreview)
                                }
                            }
                            fileWaitingToPreview = null
                        }
                    }
                }
                if (!fragmentReplaced && downloadedRemotePath == secondFragment.file.remotePath) {
                    secondFragment.onSyncEvent(downloadEvent, success, null)
                }
            }
        }
    }

    fun browseToRoot() {
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null) {  // should never be null, indeed
            val root = storageManager.getFileByPath(OCFile.ROOT_PATH)
            listOfFiles.listDirectory(root)
            file = listOfFiles.currentFile
            startSyncFolderOperation(root, false)
        }
        cleanSecondFragment()
    }

    /**
     * {@inheritDoc}
     * Updates action bar and second fragment, if in dual pane mode.
     */
    override fun onBrowsedDownTo(directory: OCFile) {
        file = directory
        cleanSecondFragment()
        // Sync Folder
        startSyncFolderOperation(directory, false)
    }

    /**
     * Shows the information of the [OCFile] received as a
     * parameter in the second fragment.
     *
     * @param file [OCFile] whose details will be shown
     */
    override fun showDetails(file: OCFile) {
        val detailFragment = FileDetailFragment.newInstance(file, account)
        setSecondFragment(detailFragment)
        updateFragmentsVisibility(true)
        updateToolbar(file)
        setFile(file)
    }

    private fun updateToolbar(chosenFileFromParam: OCFile?) {
        val chosenFile = chosenFileFromParam ?: file // If no file is passed, current file decides

        if (chosenFile == null || chosenFile.remotePath == OCFile.ROOT_PATH) {
            val title =
                when (fileListOption) {
                    FileListOption.AV_OFFLINE -> getString(R.string.drawer_item_only_available_offline)
                    FileListOption.SHARED_BY_LINK -> getString(R.string.drawer_item_shared_by_link_files)
                    FileListOption.ALL_FILES -> getString(R.string.default_display_name_for_root_folder)
                }
            setupRootToolbar(title, isSearchEnabled = true)
            listOfFilesFragment?.setSearchListener(findViewById(R.id.root_toolbar_search_view))
        } else {
            updateStandardToolbar(chosenFile.fileName, true, true)
        }
    }

    override fun newTransferenceServiceConnection(): ServiceConnection? {
        return ListServiceConnection()
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private inner class ListServiceConnection : ServiceConnection {

        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component == ComponentName(this@FileDisplayActivity, FileDownloader::class.java)) {
                Timber.d("Download service connected")
                mDownloaderBinder = service as FileDownloaderBinder

                if (fileWaitingToPreview != null) {
                    if (storageManager != null) {
                        // update the file
                        fileWaitingToPreview = storageManager.getFileById(fileWaitingToPreview!!.fileId)
                        if (!fileWaitingToPreview!!.isDown) {
                            // If the file to preview isn't downloaded yet, check if it is being
                            // downloaded in this moment or not
                            requestForDownload()
                        }
                    }
                }

                if (file != null && mDownloaderBinder.isDownloading(account, file)) {

                    // If the file is being downloaded, assure that the fragment to show is details
                    // fragment, not the streaming video fragment which has been previously
                    // set in chooseInitialSecondFragment method

                    val secondFragment = secondFragment
                    if (secondFragment != null && secondFragment is PreviewVideoFragment) {
                        cleanSecondFragment()

                        showDetails(file)
                    }
                }

            } else if (component == ComponentName(
                    this@FileDisplayActivity,
                    FileUploader::class.java
                )
            ) {
                Timber.d("Upload service connected")
                mUploaderBinder = service as FileUploaderBinder
            } else {
                return
            }
            val listOfFiles = listOfFilesFragment
            listOfFiles?.listDirectory(false)
            val secondFragment = secondFragment
            secondFragment?.onTransferServiceConnected()
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(
                    this@FileDisplayActivity,
                    FileDownloader::class.java
                )
            ) {
                Timber.d("Download service disconnected")
                mDownloaderBinder = null
            } else if (component == ComponentName(
                    this@FileDisplayActivity,
                    FileUploader::class.java
                )
            ) {
                Timber.d("Upload service disconnected")
                mUploaderBinder = null
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        when (operation) {
            is RemoveFileOperation -> onRemoveFileOperationFinish(operation, result)
            is RenameFileOperation -> onRenameFileOperationFinish(operation, result)
            is SynchronizeFileOperation -> onSynchronizeFileOperationFinish(operation, result)
            is CreateFolderOperation -> onCreateFolderOperationFinish(operation, result)
            is MoveFileOperation -> onMoveFileOperationFinish(operation, result)
            is CopyFileOperation -> onCopyFileOperationFinish(operation, result)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to
     * remove a file.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    private fun onRemoveFileOperationFinish(
        operation: RemoveFileOperation,
        result: RemoteOperationResult<*>
    ) {

        if (listOfFilesFragment!!.isSingleItemChecked || result.isException || !result.isSuccess) {
            if (result.code != ResultCode.FORBIDDEN || result.code == ResultCode.FORBIDDEN && operation.isLastFileToRemove) {
                showMessageInSnackbar(
                    R.id.list_layout,
                    ErrorMessageAdapter.getResultMessage(result, operation, resources)
                )
            }
        }

        if (result.isSuccess) {
            val removedFile = operation.file
            val second = secondFragment
            if (second != null && removedFile == second.file) {
                if (second is PreviewAudioFragment) {
                    second.stopPreview()
                } else if (second is PreviewVideoFragment) {
                    second.releasePlayer()
                }
                file = storageManager.getFileById(removedFile.parentId)
                cleanSecondFragment()
            }
            if (storageManager.getFileById(removedFile.parentId) == currentDir) {
                refreshListOfFilesFragment(true)
            }
            invalidateOptionsMenu()
        } else {
            if (result.isSslRecoverableException) {
                lastSslUntrustedServerResult = result
                showUntrustedCertDialog(lastSslUntrustedServerResult)
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to move a
     * file.
     *
     * @param operation Move operation performed.
     * @param result    Result of the move operation.
     */
    private fun onMoveFileOperationFinish(
        operation: MoveFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            refreshListOfFilesFragment(true)
        } else {
            try {
                showMessageInSnackbar(
                    R.id.list_layout,
                    ErrorMessageAdapter.getResultMessage(result, operation, resources)
                )

            } catch (e: NotFoundException) {
                Timber.e(e, "Error while trying to show fail message")
            }

        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a
     * file.
     *
     * @param operation Copy operation performed.
     * @param result    Result of the copy operation.
     */
    private fun onCopyFileOperationFinish(operation: CopyFileOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            refreshListOfFilesFragment(true)
        } else {
            try {
                showMessageInSnackbar(
                    R.id.list_layout,
                    ErrorMessageAdapter.getResultMessage(result, operation, resources)
                )

            } catch (e: NotFoundException) {
                Timber.e(e, "Error while trying to show fail message")
            }

        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename
     * a file.
     *
     * @param operation Renaming operation performed.
     * @param result    Result of the renaming.
     */
    private fun onRenameFileOperationFinish(
        operation: RenameFileOperation,
        result: RemoteOperationResult<*>
    ) {
        var renamedFile: OCFile? = operation.file
        if (result.isSuccess) {
            val details = secondFragment
            if (details != null && renamedFile == details.file) {
                renamedFile = storageManager.getFileById(renamedFile!!.fileId)
                file = renamedFile
                details.onFileMetadataChanged(renamedFile)
                updateToolbar(renamedFile)
            }

            if (storageManager.getFileById(renamedFile!!.parentId) == currentDir) {
                refreshListOfFilesFragment(true)
            }

        } else {
            showMessageInSnackbar(
                R.id.list_layout,
                ErrorMessageAdapter.getResultMessage(result, operation, resources)
            )

            if (result.isSslRecoverableException) {
                lastSslUntrustedServerResult = result
                showUntrustedCertDialog(lastSslUntrustedServerResult)
            }
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            if (operation.transferWasRequested()) {
                // this block is probably useless duy
                val syncedFile = operation.localFile
                refreshListOfFilesFragment(false)
                val secondFragment = secondFragment
                if (secondFragment != null && syncedFile == secondFragment.file) {
                    secondFragment.onSyncEvent(FileDownloader.getDownloadAddedMessage(), false, null)
                    invalidateOptionsMenu()
                }

            } else if (secondFragment == null) {
                showMessageInSnackbar(
                    R.id.list_layout,
                    ErrorMessageAdapter.getResultMessage(result, operation, resources)
                )
            }
        }

        /// no matter if sync was right or not - if there was no transfer and the file is down, OPEN it
        val waitedForPreview = fileWaitingToPreview?.let { it == operation.localFile && it.isDown } ?: false
        if (!operation.transferWasRequested() and waitedForPreview) {
            fileOperationsHelper.openFile(fileWaitingToPreview)
            fileWaitingToPreview = null
        }

    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a
     * new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private fun onCreateFolderOperationFinish(
        operation: CreateFolderOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            refreshListOfFilesFragment(true)
        } else {
            try {
                showMessageInSnackbar(
                    R.id.list_layout,
                    ErrorMessageAdapter.getResultMessage(result, operation, resources)
                )
            } catch (e: NotFoundException) {
                Timber.e(e, "Error while trying to show fail message")
            }

        }
    }

    private fun requestForDownload() {
        val account = account

        //if (!fileWaitingToPreview.isDownloading()) {
        // If the file is not being downloaded, start the download
        if (!mDownloaderBinder.isDownloading(account, fileWaitingToPreview)) {
            val i = Intent(this, FileDownloader::class.java)
            i.putExtra(FileDownloader.KEY_ACCOUNT, account)
            i.putExtra(FileDownloader.KEY_FILE, fileWaitingToPreview)
            startService(i)
        }
    }

    override fun onSavedCertificate() {
        startSyncFolderOperation(currentDir, false)
    }

    /**
     * Starts an operation to refresh the requested folder.
     *
     *
     * The operation is run in a new background thread created on the fly.
     *
     *
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including
     * associated shares), but not their contents. Only the contents of files marked to be kept-in-sync are
     * synchronized too.
     *
     * @param folder     Folder to refresh.
     * @param ignoreETag If 'true', the data from the server will be fetched and sync'ed even if the eTag
     * didn't change.
     */
    fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {

        // the execution is slightly delayed to allow the activity get the window focus if it's being started
        // or if the method is called from a dialog that is being dismissed
        handler.postDelayed(
            {
                if (hasWindowFocus()) {
                    syncInProgress = true

                    // perform folder synchronization
                    val synchFolderOp = RefreshFolderOperation(
                        folder,
                        ignoreETag,
                        account,
                        applicationContext
                    )
                    synchFolderOp.execute(
                        storageManager,
                        MainApp.appContext, null, null
                    )// unneeded, handling via SyncBroadcastReceiver

                    val fileListFragment = listOfFilesFragment
                    fileListFragment?.setProgressBarAsIndeterminate(true)

                    setBackgroundText()
                }   // else: NOTHING ; lets' not refresh when the user rotates the device but there is
                // another window floating over
            },
            DELAY_TO_REQUEST_OPERATIONS_LATER + 350
        )
    }

    private fun requestForDownload(file: OCFile?) {
        val account = account
        if (!mDownloaderBinder.isDownloading(account, fileWaitingToPreview)) {
            val i = Intent(this, FileDownloader::class.java)
            i.putExtra(FileDownloader.KEY_ACCOUNT, account)
            i.putExtra(FileDownloader.KEY_FILE, file)
            startService(i)
        }
    }

    private fun sendDownloadedFile() {
        fileOperationsHelper.sendDownloadedFile(waitingToSend)
        waitingToSend = null
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file [OCFile] to download and preview.
     */
    fun startDownloadForSending(file: OCFile) {
        waitingToSend = file
        requestForDownload(waitingToSend)
        val hasSecondFragment = secondFragment != null
        updateFragmentsVisibility(hasSecondFragment)
    }

    /**
     * Opens the image gallery showing the image [OCFile] received as parameter.
     *
     * @param file Image [OCFile] to show.
     */
    fun startImagePreview(file: OCFile) {
        val showDetailsIntent = Intent(this, PreviewImageActivity::class.java)
        showDetailsIntent.putExtra(EXTRA_FILE, file)
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, account)
        startActivity(showDetailsIntent)

    }

    /**
     * Stars the preview of an already down audio [OCFile].
     *
     * @param file                  Media [OCFile] to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     * in milliseconds.
     */
    fun startAudioPreview(file: OCFile, startPlaybackPosition: Int) {
        val mediaFragment = PreviewAudioFragment.newInstance(
            file,
            account,
            startPlaybackPosition,
            true
        )
        setSecondFragment(mediaFragment)
        updateFragmentsVisibility(true)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Stars the preview of an already down video [OCFile].
     *
     * @param file                  Media [OCFile] to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     * in milliseconds.
     */
    fun startVideoPreview(file: OCFile, startPlaybackPosition: Int) {
        val mediaFragment = PreviewVideoFragment.newInstance(
            file,
            account,
            startPlaybackPosition,
            true
        )
        setSecondFragment(mediaFragment)
        updateFragmentsVisibility(true)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Stars the preview of a text file [OCFile].
     *
     * @param file Text [OCFile] to preview.
     */
    fun startTextPreview(file: OCFile?) {
        val textPreviewFragment = PreviewTextFragment.newInstance(
            file,
            account
        )
        setSecondFragment(textPreviewFragment)
        updateFragmentsVisibility(true)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Requests the synchronization of the received [OCFile],
     * updates the UI to monitor the progress and prepares the activity
     * to preview or open the file when the download finishes.
     *
     * @param file [OCFile] to sync and open.
     */
    fun startSyncThenOpen(file: OCFile) {
        val detailFragment = FileDetailFragment.newInstance(file, account)
        setSecondFragment(detailFragment)
        fileWaitingToPreview = file
        fileOperationsHelper.syncFile(file)
        updateFragmentsVisibility(true)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Request stopping the upload/download operation in progress over the given [OCFile] file.
     *
     * @param file [OCFile] file which operation are wanted to be cancel
     */
    fun cancelTransference(file: OCFile) {
        fileOperationsHelper.cancelTransference(file)
        fileWaitingToPreview?.let {
            if (it.remotePath == file.remotePath) {
                fileWaitingToPreview = null
            }
        }

        waitingToSend?.let {
            if (it.remotePath == file.remotePath) {
                waitingToSend = null
            }
        }
        refreshListOfFilesFragment(false)

        val secondFragment = secondFragment
        if (secondFragment != null && file == secondFragment.file) {
            if (!file.fileExists()) {
                cleanSecondFragment()
            } else {
                secondFragment.onSyncEvent(FileDownloader.getDownloadFinishMessage(), false, null)
            }
        }

        invalidateOptionsMenu()
    }

    /**
     * Request stopping all upload/download operations in progress over the given [OCFile] files.
     *
     * @param files list of [OCFile] files which operations are wanted to be cancel
     */
    fun cancelTransference(files: List<OCFile>) {
        for (file in files) {
            cancelTransference(file)
        }
    }

    override fun onRefresh(ignoreETag: Boolean) {
        refreshList(ignoreETag)
    }

    override fun onRefresh() {
        refreshList(true)
    }

    private fun refreshList(ignoreETag: Boolean) {
        listOfFilesFragment?.let {
            it.currentFile?.let { folder ->
                startSyncFolderOperation(folder, ignoreETag)
            }
        }
    }

    private fun navigateTo(newFileListOption: FileListOption) {
        if (fileListOption != newFileListOption) {
            if (listOfFilesFragment != null) {
                fileListOption = newFileListOption
                file = storageManager.getFileByPath(OCFile.ROOT_PATH)
                listOfFilesFragment?.updateFileListOption(newFileListOption)
                updateToolbar(null)
            } else {
                super.navigateToOption(FileListOption.ALL_FILES)
            }
        } else {
            browseToRoot()
        }
    }

    override fun navigateToOption(fileListOption: FileListOption) {
        navigateTo(fileListOption)
    }

    private fun getMenuItemForFileListOption(fileListOption: FileListOption?): Int = when (fileListOption) {
        FileListOption.SHARED_BY_LINK -> R.id.nav_shared_by_link_files
        FileListOption.AV_OFFLINE -> R.id.nav_available_offline_files
        else -> R.id.nav_all_files
    }

    companion object {
        private const val TAG_LIST_OF_FILES = "LIST_OF_FILES"
        private const val TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT"

        private const val KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW"
        private const val KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS"
        private const val KEY_WAITING_TO_SEND = "WAITING_TO_SEND"
        private const val KEY_UPLOAD_HELPER = "FILE_UPLOAD_HELPER"
        private const val KEY_FILE_LIST_OPTION = "FILE_LIST_OPTION"

        const val ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS"

        const val REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1
        const val REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 2
        const val REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 3
        const val REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 4
        const val RESULT_OK_AND_MOVE = Activity.RESULT_FIRST_USER
    }
}
