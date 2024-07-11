/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.accounts.Account
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.owncloud.android.AppRater
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.databinding.ActivityMainBinding
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.exceptions.AccountNotFoundException
import com.owncloud.android.domain.exceptions.DeepLinkException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.SSLRecoverablePeerUnverifiedException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.checkPasscodeEnforced
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.isDownloadPending
import com.owncloud.android.extensions.manageOptionLockSelected
import com.owncloud.android.extensions.observeWorkerTillItFinishes
import com.owncloud.android.extensions.openOCFile
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.authentication.OwnCloudBearerCredentials
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.operations.SyncProfileOperation
import com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.conflicts.ConflictsResolveActivity
import com.owncloud.android.presentation.files.details.FileDetailsFragment
import com.owncloud.android.presentation.files.filelist.MainFileListFragment
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.security.bayPassUnlockOnce
import com.owncloud.android.presentation.shares.SharesFragment
import com.owncloud.android.presentation.spaces.SpacesListFragment
import com.owncloud.android.presentation.spaces.SpacesListFragment.Companion.BUNDLE_KEY_CLICK_SPACE
import com.owncloud.android.presentation.spaces.SpacesListFragment.Companion.REQUEST_KEY_CLICK_SPACE
import com.owncloud.android.presentation.spaces.SpacesListViewModel
import com.owncloud.android.presentation.transfers.TransfersViewModel
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.syncadapter.FileSyncAdapter
import com.owncloud.android.ui.dialog.FileAlreadyExistsDialog
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.ui.helpers.FilesUploadHelper
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoActivity
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Displays, what files the user has available in his ownCloud. This is the main view.
 */
class FileDisplayActivity : FileActivity(),
    CoroutineScope,
    FileFragment.ContainerActivity,
    SecurityEnforced,
    MainFileListFragment.FileActions,
    MainFileListFragment.UploadActions {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var syncBroadcastReceiver: SyncBroadcastReceiver? = null
    private var lastSslUntrustedServerResult: RemoteOperationResult<*>? = null

    /**
     * FileDisplayActivity is based on those two containers.
     * Left one is used for showing a list of files - [mainFileListFragment]
     * Right one is used for showing previews, details... - [secondFragment]
     *
     * We should rename them to a more accurate names.
     *
     * When one is shown, the other is hidden. The main logic for this is inside [updateFragmentsVisibility]
     */
    private var leftFragmentContainer: FrameLayout? = null
    private var rightFragmentContainer: FrameLayout? = null

    private val mainFileListFragment: MainFileListFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FILES) as MainFileListFragment?

    private val secondFragment: FileFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SECOND_FRAGMENT) as FileFragment?

    private var selectAllMenuItem: MenuItem? = null

    private var fileWaitingToPreview: OCFile? = null

    private var syncInProgress = false

    private var fileListOption = FileListOption.ALL_FILES
    private var waitingToSend: OCFile? = null
    private var waitingToOpen: OCFile? = null

    private var localBroadcastManager: LocalBroadcastManager? = null

    private val fileOperationsViewModel: FileOperationsViewModel by viewModel()
    private val transfersViewModel: TransfersViewModel by viewModel()

    private val sharedPreferences: SharedPreferencesProvider by inject()

    var filesUploadHelper: FilesUploadHelper? = null
        internal set

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate() start")

        super.onCreate(savedInstanceState) // this calls onAccountChanged() when ownCloud Account is valid

        checkPasscodeEnforced(this)

        if (BuildConfig.DEBUG) {
            sharedPreferences.putInt(MainApp.PREFERENCE_KEY_LAST_SEEN_VERSION_CODE, MainApp.versionCode)
        }
        sharedPreferences.putBoolean(PREFERENCE_CLEAR_DATA_ALREADY_TRIGGERED, true)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        handleDeepLink()

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // setup toolbar
        setupRootToolbar(
            isSearchEnabled = true,
            title = getString(R.string.default_display_name_for_root_folder),
            isAvatarRequested = true,
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

        supportFragmentManager.setFragmentResultListener(REQUEST_KEY_CLICK_SPACE, this) { _, bundle ->
            val rootSpaceFolder = bundle.getParcelable<OCFile>(BUNDLE_KEY_CLICK_SPACE)
            file = rootSpaceFolder
            initAndShowListOfFiles()
        }

        if (resources.getBoolean(R.bool.enable_rate_me_feature) && !BuildConfig.DEBUG) {
            AppRater.appLaunched(this, packageName)
        }


        checkNotificationPermission()
        Timber.v("onCreate() end")
    }

    private fun checkNotificationPermission() {
        // Ask for permission only in case it's api >= 33 and notifications are not granted.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) return

        // Permission denied. Can be because notifications are off by default or because they were denied by the user.
        val alreadyRequested = sharedPreferences.getBoolean(PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED, false)
        val shouldShowPermissionRequest = shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)
        Timber.d("Already requested notification permission $alreadyRequested and should ask again $shouldShowPermissionRequest")
        if (!alreadyRequested || shouldShowPermissionRequest) {
            // Not requested yet or system considers we can request the permission again.
            val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    Timber.d("Permission to send notifications granted: $isGranted")
                    if (!isGranted) {
                        showSnackMessage(getString(R.string.notifications_permission_denied))
                    }
                    sharedPreferences.putBoolean(PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED, true)
                }
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (savedInstanceState == null && mAccountWasSet) {
            val capabilitiesViewModel: CapabilityViewModel by viewModel {
                parametersOf(
                    account?.name
                )
            }
            capabilitiesViewModel.capabilities.observe(this, Event.EventObserver {
                onCapabilitiesOperationFinish(it)
            })
            navigateTo(fileListOption, initialState = true)
        }

        startListeningToOperations()
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
                if (file.isAvailableLocally) {
                    // upload in progress - right now, files are not inserted in the local
                    // cache until the upload is successful get parent from path
                    parentPath = file.remotePath.substring(
                        0,
                        file.remotePath.lastIndexOf(file.fileName)
                    )
                    if (storageManager.getFileByPath(parentPath, file.spaceId) == null) {
                        file = null // not able to know the directory where the file is uploading
                    }
                } else {
                    file = storageManager.getFileByPath(file.remotePath, file.spaceId)
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = storageManager.getRootPersonalFolder()  // never returns null
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
                val syncProfileOperation = SyncProfileOperation(account)
                syncProfileOperation.syncUserProfile()
                val workManagerProvider = WorkManagerProvider(context = baseContext)
                workManagerProvider.enqueueAvailableOfflinePeriodicWorker()
            } else {
                file?.isFolder?.let { isFolder ->
                    updateFragmentsVisibility(!isFolder)
                    updateToolbar(if (isFolder) null else file)
                }
            }
        }

        val spacesListViewModel: SpacesListViewModel by viewModel {
            parametersOf(account.name, false)
        }
        spacesListViewModel.refreshSpacesFromServer()
    }

    private fun initAndShowListOfFiles(fileListOption: FileListOption = FileListOption.ALL_FILES) {
        val mainListOfFiles = MainFileListFragment.newInstance(
            initialFolderToDisplay = file,
            fileListOption = fileListOption,
        ).apply {
            fileActions = this@FileDisplayActivity
            uploadActions = this@FileDisplayActivity
            setSearchListener(findViewById(R.id.root_toolbar_search_view))
        }
        this.fileListOption = fileListOption
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.left_fragment_container, mainListOfFiles, TAG_LIST_OF_FILES)
        transaction.commit()
    }

    private fun initAndShowListOfSpaces() {
        val listOfSpaces = SpacesListFragment.newInstance(
            showPersonalSpace = false,
            accountName = com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount(applicationContext).name
        ).apply {
            setSearchListener(findViewById(R.id.root_toolbar_search_view))
        }
        this.fileListOption = FileListOption.SPACES_LIST
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.left_fragment_container, listOfSpaces, TAG_LIST_OF_SPACES)
        transaction.commit()
    }

    private fun initAndShowListOfShares() {
        val sharesFragment = SharesFragment()
        this.fileListOption = FileListOption.SHARED_BY_LINK
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.left_fragment_container, sharesFragment)
        transaction.commit()
    }

    private fun initFragmentsWithFile() {
        if (account != null && file != null) {
            /// First fragment
            mainFileListFragment?.navigateToFolder(currentDir)
                ?: Timber.e("Still have a chance to lose the initialization of list fragment >(")

            /// Second fragment
            val file = file
            val secondFragment = chooseInitialSecondFragment(file)
            secondFragment?.let {
                setSecondFragment(it)
                updateToolbar(it.file)
            } ?: cleanSecondFragment()

        } else {
            Timber.e("initFragmentsWithFile() called with invalid nulls! account is $account, file is $file")
        }
    }

    /**
     * Choose the second fragment that is going to be shown
     *
     * @param file used to decide which fragment should be chosen.
     *
     * @return a new second fragment instance if it has not been chosen before, or the fragment
     * previously chosen otherwise
     */
    private fun chooseInitialSecondFragment(file: OCFile): FileFragment? {
        val secondFragment = supportFragmentManager.findFragmentByTag(TAG_SECOND_FRAGMENT) as FileFragment?

        // Return second fragment if it has been already chosen
        if (secondFragment != null) return secondFragment

        // Return null if we receive a folder. This way, second fragment will be cleared. We should move this logic out of here.
        if (file.isFolder) return null

        // Otherwise, decide which fragment should be shown.
        return when {
            PreviewAudioFragment.canBePreviewed(file) -> {
                val startPlaybackPosition = intent.getIntExtra(PreviewVideoActivity.EXTRA_PLAY_POSITION, 0)
                val autoplay = intent.getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true)
                PreviewAudioFragment.newInstance(
                    file,
                    account,
                    startPlaybackPosition,
                    autoplay
                )
            }

            PreviewTextFragment.canBePreviewed(file) -> {
                PreviewTextFragment.newInstance(
                    file,
                    account
                )
            }

            else -> {
                FileDetailsFragment.newInstance(file, account, false)
            }
        }
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
        transaction.commitNow()
        updateFragmentsVisibility(true)
    }

    private fun showBottomNavBar(show: Boolean) {
        binding.navCoordinatorLayout.bottomNavView.isVisible = show
    }

    /**
     * Handle the visibility of the two main containers in the activity.
     *
     * Showing list of files should hide right container
     * Showing preview or details should hide left container
     *
     * @param existsSecondFragment - true if showing details or preview of a file
     */
    private fun updateFragmentsVisibility(existsSecondFragment: Boolean) {
        leftFragmentContainer?.isVisible = !existsSecondFragment
        rightFragmentContainer?.isVisible = existsSecondFragment

        showBottomNavBar(show = !existsSecondFragment)
    }

    private fun cleanSecondFragment() {
        val second = secondFragment
        if (second != null) {
            val tr = supportFragmentManager.beginTransaction()
            tr.remove(second)
            tr.commitNow()
        }
        updateFragmentsVisibility(false)
        updateToolbar(null)
    }

    private fun refreshListOfFilesFragment() {
        // TODO Remove commented code
        /*val fileListFragment = listOfFilesFragment
        fileListFragment?.listDirectory(reloadData)*/
        if (file != null) {
            val fileListFragment = mainFileListFragment
            mainFileListFragment?.fileActions = this
            fileListFragment?.navigateToFolder(file)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Allow or disallow touches with other visible windows
        val actionBarView = findViewById<View>(R.id.action_bar)
        if (actionBarView != null) {
            actionBarView.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(applicationContext)
        }

        selectAllMenuItem = menu.findItem(R.id.action_select_all)
        if (secondFragment == null) {
            selectAllMenuItem?.isVisible = true
        } else {
            val shareFileMenuItem = menu.findItem(R.id.action_share_current_folder)
            menu.removeItem(shareFileMenuItem.itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Called, when the user selected something for uploading
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        bayPassUnlockOnce()

        // Handle calls form internal activities.
        if (requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS && (resultCode == RESULT_OK || resultCode == RESULT_OK_AND_MOVE)) {

            requestUploadOfContentFromApps(data, resultCode)

        } else if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA) {
            if (resultCode == RESULT_OK || resultCode == RESULT_OK_AND_MOVE) {
                filesUploadHelper?.onActivityResult(object : FilesUploadHelper.OnCheckAvailableSpaceListener {
                    override fun onCheckAvailableSpaceStart() {

                    }

                    override fun onCheckAvailableSpaceFinished(
                        hasEnoughSpace: Boolean,
                        capturedFilePaths: Array<String>
                    ) {
                        if (hasEnoughSpace) {
                            requestUploadOfFilesFromFileSystem(capturedFilePaths, UploadBehavior.MOVE.toLegacyLocalBehavior())
                        }
                    }
                })
            } else if (requestCode == RESULT_CANCELED) {
                filesUploadHelper?.deleteImageFile()
            }

            // requestUploadOfFilesFromFileSystem(data,resultCode);
        } else if (requestCode == REQUEST_CODE__MOVE_FILES && resultCode == RESULT_OK) {
            requestMoveOperation(data!!)

        } else if (requestCode == REQUEST_CODE__COPY_FILES && resultCode == RESULT_OK) {
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

            transfersViewModel.uploadFilesFromSystem(
                accountName = account.name,
                listOfLocalPaths = filePaths.toList(),
                uploadFolderPath = remotePathBase!!,
                spaceId = currentDir.spaceId,
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

        val currentDir = currentDir
        val remotePath = currentDir?.remotePath ?: OCFile.ROOT_PATH

        // Try to retain access to that file for some time, so we have enough time to upload it
        streamsToUpload.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (remoteException: RemoteException) {
                Timber.w(remoteException)
            }
        }

        transfersViewModel.uploadFilesFromContentUri(
            accountName = account.name,
            listOfContentUris = streamsToUpload,
            uploadFolderPath = remotePath,
            spaceId = currentDir.spaceId,
        )
    }

    /**
     * Request the operation for moving the file/folder from one path to another
     *
     * @param data Intent received
     */
    private fun requestMoveOperation(data: Intent) {
        val folderToMoveAt = data.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER) ?: return
        val files = data.getParcelableArrayListExtra<OCFile>(FolderPickerActivity.EXTRA_FILES) ?: return
        val moveOperation = FileOperation.MoveOperation(
            listOfFilesToMove = files.toList(),
            targetFolder = folderToMoveAt,
            isUserLogged = com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount(this) != null,
        )
        fileOperationsViewModel.performOperation(moveOperation)
    }

    /**
     * Request the operation for copying the file/folder from one path to another
     *
     * @param data Intent received
     */
    private fun requestCopyOperation(data: Intent) {
        val folderToCopyAt = data.getParcelableExtra<OCFile>(FolderPickerActivity.EXTRA_FOLDER) ?: return
        val files = data.getParcelableArrayListExtra<OCFile>(FolderPickerActivity.EXTRA_FILES) ?: return
        val copyOperation = FileOperation.CopyOperation(
            listOfFilesToCopy = files.toList(),
            targetFolder = folderToCopyAt,
            isUserLogged = com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount(this) != null,
        )
        fileOperationsViewModel.performOperation(copyOperation)
    }

    override fun onBackPressed() {
        val isFabOpen = mainFileListFragment?.isFabExpanded() ?: false

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
            mainFileListFragment?.collapseFab()
        } else {
            // Every single menu is collapsed. We can navigate up.
            if (secondFragment != null) {
                // If secondFragment was shown, we need to navigate to the parent of the displayed file
                // Need a cleanup
                val folderIdToDisplay =
                    if (fileListOption == FileListOption.AV_OFFLINE) storageManager.getRootPersonalFolder()!!.id!! else secondFragment!!.file!!.parentId!!
                mainFileListFragment?.navigateToFolderId(folderIdToDisplay)
                cleanSecondFragment()
                updateToolbar(mainFileListFragment?.getCurrentFile())
            } else {
                val currentDirDisplayed = mainFileListFragment?.getCurrentFile()
                // If current file is null (we are in the spaces list, for example), close the app
                if (currentDirDisplayed == null) {
                    finish()
                    return
                }
                // If current file is root folder
                else if (currentDirDisplayed.parentId == ROOT_PARENT_ID) {
                    // If current space is a project space (not personal, not shares), navigate back to the spaces list
                    if (mainFileListFragment?.getCurrentSpace()?.isProject == true) {
                        navigateTo(FileListOption.SPACES_LIST)
                    }
                    // If current space is not a project space (personal or shares) or it is null ("Files" in oC10), close the app
                    else {
                        finish()
                        return
                    }
                } else {
                    mainFileListFragment?.onBrowseUp()
                }
            }
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

        if (mainFileListFragment?.getCurrentSpace()?.isProject == true) {
            setCheckedItemAtBottomBar(getMenuItemForFileListOption(FileListOption.SPACES_LIST))
            updateToolbar(null, mainFileListFragment?.getCurrentSpace())
        } else {
            setCheckedItemAtBottomBar(getMenuItemForFileListOption(fileListOption))
        }

        mainFileListFragment?.updateFileListOption(fileListOption, file)

        // refresh list of files
        refreshListOfFilesFragment()

        // Listen for sync messages
        val syncIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
        syncBroadcastReceiver = SyncBroadcastReceiver()
        localBroadcastManager!!.registerReceiver(syncBroadcastReceiver!!, syncIntentFilter)

        showDialogs()
        Timber.v("onResume() end")
    }

    override fun onPause() {
        Timber.v("onPause() start")
        if (syncBroadcastReceiver != null) {
            localBroadcastManager!!.unregisterReceiver(syncBroadcastReceiver!!)
            syncBroadcastReceiver = null
        }

        super.onPause()
        dismissDialogs()
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
                        storageManager.getFileByPath(file.remotePath, file.spaceId)
                    val currentDir = if (currentDir == null)
                        null
                    else
                        storageManager.getFileByPath(currentDir!!.remotePath, currentDir.spaceId)

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
                            mainFileListFragment?.navigateToFolder(currentDir)
                        }
                        file = currentFile
                    }

                    syncInProgress =
                        FileSyncAdapter.EVENT_FULL_SYNC_END != event
                }

                mainFileListFragment?.setProgressBarAsIndeterminate(syncInProgress)
                Timber.d("Setting progress visibility to $syncInProgress")
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

    fun browseToRoot() {
        val listOfFiles = mainFileListFragment
        if (listOfFiles != null) {  // should never be null, indeed
            val root = storageManager.getRootPersonalFolder()
            listOfFiles.navigateToFolder(root!!)
            file = root
            startSyncFolderOperation(root, false)
        }
        cleanSecondFragment()
    }

    /**
     * Shows the information of the [OCFile] received as a
     * parameter in the second fragment.
     *
     * @param file [OCFile] whose details will be shown
     */
    override fun showDetails(file: OCFile) {
        navigateToDetails(account = account, ocFile = file, syncFileAtOpen = false)
        updateToolbar(file)
        setFile(file)
    }

    override fun syncFile(file: OCFile) {
        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
    }

    override fun openFile(file: OCFile) {
        if (file.isAvailableLocally) {
            fileOperationsHelper.openFile(file)
            fileOperationsViewModel.setLastUsageFile(file)
        } else {
            startDownloadForOpening(file)
        }
    }

    override fun sendDownloadedFile(file: OCFile) {
        sendDownloadedFilesByShareSheet(listOf(file))
    }

    private fun updateToolbar(chosenFileFromParam: OCFile?, space: OCSpace? = null) {
        val chosenFile = chosenFileFromParam ?: file // If no file is passed, current file decides

        // If we come from a preview activity (image or video), not updating toolbar when initializing this activity or it will show the root folder one
        if (intent.action == ACTION_DETAILS && chosenFile?.remotePath == OCFile.ROOT_PATH && secondFragment is FileDetailsFragment) return

        if (chosenFile == null || (chosenFile.remotePath == OCFile.ROOT_PATH && (space == null || !space.isProject))) {
            val title =
                when (fileListOption) {
                    FileListOption.AV_OFFLINE -> getString(R.string.drawer_item_only_available_offline)
                    FileListOption.SHARED_BY_LINK -> if (chosenFile == null || chosenFile.spaceId != null) {
                        getString(R.string.bottom_nav_shares)
                    } else {
                        getString(R.string.bottom_nav_links)
                    }

                    FileListOption.ALL_FILES -> getString(R.string.default_display_name_for_root_folder)
                    FileListOption.SPACES_LIST -> getString(R.string.bottom_nav_spaces)
                }
            setupRootToolbar(title = title, isSearchEnabled = true, isAvatarRequested = false)
        } else if (space?.isProject == true && chosenFile.remotePath == OCFile.ROOT_PATH) {
            updateStandardToolbar(title = space.name, displayHomeAsUpEnabled = true, homeButtonEnabled = true)
        } else {
            updateStandardToolbar(title = chosenFile.fileName, displayHomeAsUpEnabled = true, homeButtonEnabled = true)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to
     * remove a file.
     */
    private fun onRemoveFileOperationResult(
        uiResult: UIResult<List<OCFile>>
    ) {
        when (uiResult) {
            is UIResult.Loading -> {
                showLoadingDialog(R.string.wait_a_moment)
            }

            is UIResult.Success -> {
                dismissLoadingDialog()
                val listOfFilesRemoved = uiResult.data ?: return
                val lastRemovedFile = listOfFilesRemoved.last()
                val singleRemoval = listOfFilesRemoved.size == 1

                if (singleRemoval) {
                    showMessageInSnackbar(message = getString(R.string.remove_success_msg))
                }

                // Clean second fragment and refresh first one
                val secondFragment = secondFragment
                if (secondFragment?.file == lastRemovedFile) {
                    when (secondFragment) {
                        is PreviewAudioFragment -> {
                            secondFragment.stopPreview()
                        }
                    }
                    file = storageManager.getFileById(lastRemovedFile.parentId!!)
                    cleanSecondFragment()
                }
                invalidateOptionsMenu()
            }

            is UIResult.Error -> {
                dismissLoadingDialog()
                showErrorInSnackbar(R.string.remove_fail_msg, uiResult.getThrowableOrNull())

                if (uiResult.getThrowableOrNull() is SSLRecoverablePeerUnverifiedException) {
                    showUntrustedCertDialogForThrowable(uiResult.getThrowableOrNull())
                }
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to move a
     * file.
     */
    private fun onMoveFileOperationFinish(
        uiResult: UIResult<List<OCFile>>
    ) {
        when (uiResult) {
            is UIResult.Loading -> {
                showLoadingDialog(R.string.wait_a_moment)
            }

            is UIResult.Success -> {
                dismissLoadingDialog()
                val replace = mutableListOf<Boolean?>()
                uiResult.data?.let {
                    showConflictDecisionDialog(uiResult = uiResult, data = it, replace = replace) { data, replace ->
                        launchMoveFile(data, replace)
                    }
                }
            }

            is UIResult.Error -> {
                dismissLoadingDialog()

                uiResult.error?.let {
                    showMessageInSnackbar(
                        message = it.parseError(getString(R.string.move_file_error), resources, true)
                    )
                }
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a
     * file.
     *
     * @param uiResult - UIResult wrapping the target folder where files were copied
     */
    private fun onCopyFileOperationFinish(
        uiResult: UIResult<List<OCFile>>
    ) {
        when (uiResult) {
            is UIResult.Loading -> {
                showLoadingDialog(R.string.wait_a_moment)
            }

            is UIResult.Success -> {
                dismissLoadingDialog()
                val replace = mutableListOf<Boolean?>()
                uiResult.data?.let {
                    showConflictDecisionDialog(uiResult = uiResult, data = it, replace = replace) { data, replace ->
                        launchCopyFile(data, replace)
                    }
                }
            }

            is UIResult.Error -> {
                dismissLoadingDialog()

                uiResult.error?.let {
                    showMessageInSnackbar(
                        message = it.parseError(
                            genericErrorMessage = getString(R.string.copy_file_error),
                            resources = resources,
                            showJustReason = true,
                        )
                    )
                }
            }
        }
    }

    private fun showConflictDecisionDialog(
        uiResult: UIResult.Success<List<OCFile>>,
        data: List<OCFile>,
        replace: MutableList<Boolean?>,
        launchAction: (List<OCFile>, List<Boolean?>) -> Unit,
    ) {
        if (!uiResult.data.isNullOrEmpty()) {
            val posArray = intArrayOf(0)
            var posDialog = intArrayOf(data.lastIndex)
            data.asReversed().forEachIndexed { index, file ->
                val countDownValue = index + 1

                val customDialog = FileAlreadyExistsDialog.newInstance(
                    titleText = this.getString(
                        if (file.isFolder) {
                            R.string.folder_already_exists
                        } else {
                            R.string.file_already_exists
                        }
                    ),
                    descriptionText = this.getString(
                        if (file.isFolder) {
                            R.string.folder_already_exists_description
                        } else {
                            R.string.file_already_exists_description
                        }, file.fileName
                    ),
                    checkboxText = this.getString(R.string.apply_to_all_conflicts, countDownValue.toString()),
                    checkboxVisible = countDownValue > 1
                )
                customDialog.isCancelable = false
                customDialog.show(this.supportFragmentManager, CUSTOM_DIALOG_TAG)

                fileOperationsViewModel.openDialogs.add(customDialog)


                customDialog.setDialogButtonClickListener(object : FileAlreadyExistsDialog.DialogButtonClickListener {

                    override fun onKeepBothButtonClick() {
                        applyAction(
                            posDialog = posDialog,
                            data = data,
                            replace = replace,
                            pos = posArray,
                            launchAction = launchAction,
                            uiResult = uiResult,
                            action = false
                        )
                    }

                    override fun onSkipButtonClick() {
                        applyAction(
                            posDialog = posDialog,
                            data = data,
                            replace = replace,
                            pos = posArray,
                            launchAction = launchAction,
                            uiResult = uiResult,
                            action = null
                        )
                    }

                    override fun onReplaceButtonClick() {
                        applyAction(
                            posDialog = posDialog,
                            data = data,
                            replace = replace,
                            pos = posArray,
                            launchAction = launchAction,
                            uiResult = uiResult,
                            action = true
                        )
                    }
                }
                )
            }
        }
    }

    private fun applyAction(
        posDialog: IntArray,
        data: List<OCFile>,
        replace: MutableList<Boolean?>,
        pos: IntArray,
        launchAction: (List<OCFile>, List<Boolean?>) -> Unit,
        uiResult: UIResult.Success<List<OCFile>>,
        action: Boolean?
    ) {
        var posDialog1 = posDialog
        var pos1 = pos
        if (fileOperationsViewModel.openDialogs[posDialog1[0]].isCheckBoxChecked) {
            repeat(data.asReversed().size) {
                replace.add(action)
                pos1[0]++
                if (pos1[0] == data.size) {
                    launchAction(
                        uiResult.data!!,
                        replace,
                    )
                }
            }
            dismissAllOpenDialogs()
        } else {
            replace.add(action)
            pos1[0]++
            if (pos1[0] == data.size) {
                launchAction(
                    uiResult.data!!,
                    replace,
                )
            }
            fileOperationsViewModel.openDialogs[posDialog1[0]].dismiss()
            fileOperationsViewModel.openDialogs.removeAt(posDialog1[0])
            if (posDialog1[0] == 0) {
                fileOperationsViewModel.openDialogs.clear()
            } else {
                posDialog1[0]--
            }
        }
    }

    private fun dismissAllOpenDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.dismiss()
        }
        fileOperationsViewModel.openDialogs.clear()
    }

    private fun showDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.show(this.supportFragmentManager, CUSTOM_DIALOG_TAG)
        }
    }

    private fun dismissDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.dismiss()
        }
    }

    private fun launchCopyFile(files: List<OCFile>, replace: List<Boolean?>) {
        fileOperationsViewModel.performOperation(
            FileOperation.CopyOperation(
                listOfFilesToCopy = files,
                targetFolder = null,
                replace = replace,
                isUserLogged = com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount(this) != null,
            )
        )
    }

    private fun launchMoveFile(files: List<OCFile>, replace: List<Boolean?>) {
        fileOperationsViewModel.performOperation(
            FileOperation.MoveOperation(
                listOfFilesToMove = files,
                targetFolder = null,
                replace = replace,
                isUserLogged = com.owncloud.android.presentation.authentication.AccountUtils.getCurrentOwnCloudAccount(this) != null,
            )
        )
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename
     * a file.
     *
     * @param uiResult - UIResult wrapping the file that was renamed
     */
    private fun onRenameFileOperationFinish(
        uiResult: UIResult<OCFile>
    ) {
        when (uiResult) {
            is UIResult.Loading -> {
                showLoadingDialog(R.string.wait_a_moment)
            }

            is UIResult.Success -> {
                dismissLoadingDialog()

                val details = secondFragment
                uiResult.data?.id?.let { fileId ->
                    if (details != null && uiResult.data == details.file) {
                        val updatedRenamedFile = storageManager.getFileById(fileId)
                        file = updatedRenamedFile
                        details.onFileMetadataChanged(updatedRenamedFile)
                        updateToolbar(updatedRenamedFile)
                    }
                }
            }

            is UIResult.Error -> {
                dismissLoadingDialog()

                showErrorInSnackbar(R.string.rename_server_fail_msg, uiResult.error)

                if (uiResult.getThrowableOrNull() is SSLRecoverablePeerUnverifiedException) {
                    showUntrustedCertDialogForThrowable(uiResult.getThrowableOrNull())
                }
            }
        }
    }

    private fun onSynchronizeFileOperationFinish(
        uiResult: UIResult<SynchronizeFileUseCase.SyncType>
    ) {
        when (uiResult) {
            is UIResult.Success -> {
                when (uiResult.data) {
                    SynchronizeFileUseCase.SyncType.AlreadySynchronized -> {
                        if (fileWaitingToPreview != null) {
                            startPreview(fileWaitingToPreview)
                            fileWaitingToPreview = null
                        } else {
                            showSnackMessage(getString(R.string.sync_file_nothing_to_do_msg))
                        }
                    }

                    is SynchronizeFileUseCase.SyncType.ConflictDetected -> {
                        val showConflictActivityIntent = Intent(this, ConflictsResolveActivity::class.java)
                        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, file)
                        startActivity(showConflictActivityIntent)
                    }

                    is SynchronizeFileUseCase.SyncType.DownloadEnqueued -> {
                        fileWaitingToPreview?.let {
                            showSnackMessage(getString(R.string.new_remote_version_found_msg))
                            startSyncThenOpen(it)
                            fileWaitingToPreview = null
                        } ?: showSnackMessage(getString(R.string.download_enqueued_msg))
                    }

                    SynchronizeFileUseCase.SyncType.FileNotFound -> {
                        /** Nothing to do atm. If we are in details view, go back to file list */
                    }

                    is SynchronizeFileUseCase.SyncType.UploadEnqueued -> showSnackMessage(getString(R.string.upload_enqueued_msg))

                    null -> TODO()
                }
            }

            is UIResult.Error -> {
                if (fileWaitingToPreview != null) {
                    startPreview(fileWaitingToPreview)
                    fileWaitingToPreview = null
                }
                when (uiResult.error) {
                    is AccountNotFoundException -> {
                        showRequestAccountChangeNotice(getString(R.string.sync_fail_ticker_unauthorized), false)
                    }

                    is UnauthorizedException -> {
                        launch(Dispatchers.IO) {
                            val credentials = AccountUtils.getCredentialsForAccount(MainApp.appContext, account)

                            launch(Dispatchers.Main) {
                                if (credentials is OwnCloudBearerCredentials) { // OAuth
                                    showRequestRegainAccess()
                                } else {
                                    showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                                }
                            }
                        }
                    }

                    is CertificateCombinedException -> {
                        showUntrustedCertDialogForThrowable(uiResult.error)
                    }

                    else -> {
                        showSnackMessage(getString(R.string.sync_fail_ticker))
                    }
                }
            }

            is UIResult.Loading -> {
                /** Not needed at the moment, we may need it later */
            }
        }
    }

    private fun openShortcutFileInBrowser(file: OCFile) {
        val url = extractUrlFromFile(file.storagePath.toString())
        val urlFormat = formatUrl(url!!)
        val message = getString(R.string.open_shortcut_description)
        val messageTextView = TextView(this).apply {
            text = message
            setPadding(0, 70, 0, 30)
            setTextColor(ContextCompat.getColor(this@FileDisplayActivity, android.R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        val urlTextView = TextView(this).apply {
            text = urlFormat
            setTextColor(ContextCompat.getColor(this@FileDisplayActivity, android.R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(urlTextView)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(62, 0, 62, 70)
            addView(messageTextView)
            addView(scrollView)
        }

        val dialog = AlertDialog.Builder(this@FileDisplayActivity)
            .setTitle(getString(R.string.open_shortcut_title))
            .setView(layout)
            .setPositiveButton(R.string.drawer_open) { view, _ ->
                urlFormat?.let {
                    goToUrl(urlFormat)
                }
                view.dismiss()
            }
            .setNegativeButton(R.string.share_cancel_public_link_button) { view, _ ->
                view.dismiss()
            }
            .setCancelable(true)
            .create()
        dialog.show()
    }

    private fun extractUrlFromFile(filePath: String): String? {
        val file = File(filePath)
        if (file.exists()) {
            val lines = file.readLines()
            for (line in lines) {
                if (line.startsWith("URL=")) {
                    return line.substringAfter("URL=").trim('"')
                }
            }
        }
        return null
    }

    private fun formatUrl(url: String): String {
        var formattedUrl = url
        if (!url.startsWith(PROTOCOL_HTTP) && !url.startsWith(PROTOCOL_HTTPS)) {
            formattedUrl = PROTOCOL_HTTPS + url
        }
        return formattedUrl
    }

    private fun onSynchronizeFolderOperationFinish(
        uiResult: UIResult<Unit>
    ) {
        when (uiResult) {
            is UIResult.Success -> {
                // Nothing to handle when synchronizing a folder succeeds
            }

            is UIResult.Error -> {
                when (uiResult.error) {
                    is UnauthorizedException -> {
                        launch(Dispatchers.IO) {
                            val credentials = AccountUtils.getCredentialsForAccount(MainApp.appContext, account)
                            launch(Dispatchers.Main) {
                                if (credentials is OwnCloudBearerCredentials) { // OAuth
                                    showRequestRegainAccess()
                                } else {
                                    showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                                }
                            }
                        }
                    }

                    is CertificateCombinedException -> {
                        showUntrustedCertDialogForThrowable(uiResult.error)
                    }

                    else -> {
                        showSnackMessage(getString(R.string.sync_fail_ticker))
                    }
                }
            }

            is UIResult.Loading -> {
                /** Not needed at the moment, we may need it later */
            }
        }
    }

    private fun onCapabilitiesOperationFinish(uiResult: UIResult<OCCapability>) {
        if (uiResult is UIResult.Success) {
            val capabilities = uiResult.data
            capabilities?.versionString?.let { capabilitiesVersionString ->
                val ownCloudVersion = OwnCloudVersion(capabilitiesVersionString)
                if (!ownCloudVersion.isServerVersionSupported) {
                    Timber.d("Server version not supported")
                    showRequestAccountChangeNotice(getString(R.string.server_not_supported), true)
                }
            }
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
     * @param ignoreETag If 'true', the data from the server will be fetched and synced even if the eTag
     * didn't change.
     */
    fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {
        // TODO: SYNC FOLDER
    }

    private fun requestForDownload(file: OCFile) {
        val downloadFileUseCase: DownloadFileUseCase by inject()

        val uuid = downloadFileUseCase(DownloadFileUseCase.Params(account.name, file)) ?: return

        WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(uuid).observeWorkerTillItFinishes(
            owner = this,
            onWorkEnqueued = {
                showMessageInSnackbar(
                    message = String.format(getString(R.string.downloader_download_enqueued_ticker), file.fileName)
                )
            },
            onWorkRunning = { progress -> Timber.d("Downloading - Progress $progress") },
            onWorkSucceeded = {
                CoroutineScope(Dispatchers.IO).launch {
                    if (file.mimeType == MIMETYPE_TEXT_URI_LIST) {
                        waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        launch(Dispatchers.Main) {
                            openShortcutFileInBrowser(waitingToOpen!!)
                        }
                    } else if (file.id == waitingToSend?.id) {
                        waitingToSend = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        sendDownloadedFile()
                    } else if (file.id == waitingToOpen?.id) {
                        waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        openDownloadedFile()
                    }
                }
            },
            onWorkFailed = {
                showMessageInSnackbar(
                    message = String.format(getString(R.string.downloader_download_failed_ticker), file.fileName)
                )
                if (file.id == waitingToSend?.id) {
                    waitingToSend = null
                } else if (file.id == waitingToOpen?.id) {
                    waitingToOpen = null
                }
            },
            onWorkCancelled = {
                if (file.id == waitingToSend?.id) {
                    waitingToSend = null
                } else if (file.id == waitingToOpen?.id) {
                    waitingToOpen = null
                }
            },
        )
    }

    private fun sendDownloadedFile() {
        waitingToSend?.let {
            sendDownloadedFilesByShareSheet(listOf(it))
        }

        waitingToSend = null
    }

    private fun openDownloadedFile() {
        waitingToOpen?.let {
            openOCFile(it)
        }

        waitingToOpen = null
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file [OCFile] to download and preview.
     */
    private fun startDownloadForSending(file: OCFile) {
        waitingToSend = file
        requestForDownload(file)
        val hasSecondFragment = secondFragment != null
        updateFragmentsVisibility(hasSecondFragment)
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI
     * to monitor the download progress and prepares the activity to open the file
     * when the download finishes.
     *
     * @param file [OCFile] to download and preview.
     */
    private fun startDownloadForOpening(file: OCFile) {
        waitingToOpen = file
        requestForDownload(file)
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
        val videoActivityIntent = Intent(this, PreviewVideoActivity::class.java)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_FILE, file)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_ACCOUNT, account)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_PLAY_POSITION, startPlaybackPosition)
        startActivity(videoActivityIntent)
    }

    /**
     * Stars the preview of a text file [OCFile].
     *
     * @param file Text [OCFile] to preview.
     */
    fun startTextPreview(file: OCFile) {
        val textPreviewFragment = PreviewTextFragment.newInstance(
            file,
            account
        )
        setSecondFragment(textPreviewFragment)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Chooses the suitable method to preview a file [OCFile].
     *
     * @param file File [OCFile] to preview.
     */
    private fun startPreview(file: OCFile?) {
        file?.let {
            when {
                PreviewTextFragment.canBePreviewed(file) -> {
                    startTextPreview(file)
                }

                PreviewAudioFragment.canBePreviewed(file) -> {
                    startAudioPreview(file, 0)
                }
            }
        }
    }

    /**
     * Requests the synchronization of the received [OCFile],
     * updates the UI to monitor the progress and prepares the activity
     * to preview or open the file when the download finishes.
     *
     * @param file [OCFile] to sync and open.
     */
    private fun startSyncThenOpen(file: OCFile) {
        if (file.mimeType == MIMETYPE_TEXT_URI_LIST) {
            openOrDownloadShortcutFile(file)
        } else {
            navigateToDetails(account = account, ocFile = file, syncFileAtOpen = true)
//        fileWaitingToPreview = file
//        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            updateToolbar(file)
            setFile(file)
        }
    }

    private fun openOrDownloadShortcutFile(file: OCFile) {
        if (file.isAvailableLocally) {
            waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
            openShortcutFileInBrowser(waitingToOpen!!)
        } else {
            requestForDownload(file)
        }
    }

    private fun navigateToDetails(account: Account, ocFile: OCFile, syncFileAtOpen: Boolean) {
        val detailsFragment = FileDetailsFragment.newInstance(
            fileToDetail = ocFile,
            account = account,
            syncFileAtOpen = syncFileAtOpen
        )
        setSecondFragment(detailsFragment)
    }

    private fun navigateTo(newFileListOption: FileListOption, initialState: Boolean = false) {
        val previousFileListOption = fileListOption
        when (newFileListOption) {
            FileListOption.ALL_FILES -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    file = storageManager.getRootPersonalFolder()
                    fileListOption = newFileListOption
                    mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                    updateToolbar(file)
                } else {
                    browseToRoot()
                }
            }

            FileListOption.SPACES_LIST -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    file = null
                    initAndShowListOfSpaces()
                    updateToolbar(null)
                }
            }

            FileListOption.SHARED_BY_LINK -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    val rootFolderForShares = storageManager.getRootSharesFolder()
                    val personalFolder = storageManager.getRootPersonalFolder()
                    if (rootFolderForShares == null && personalFolder?.spaceId != null) {
                        fileListOption = newFileListOption
                        initAndShowListOfShares()
                        updateToolbar(null)
                    } else {
                        file = rootFolderForShares ?: personalFolder
                        fileListOption = newFileListOption
                        mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                        updateToolbar(null)
                    }
                }
            }

            FileListOption.AV_OFFLINE -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    file = storageManager.getRootPersonalFolder()
                    fileListOption = newFileListOption
                    mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                    updateToolbar(file)
                }
            }
        }
    }

    override fun navigateToOption(fileListOption: FileListOption) {
        navigateTo(fileListOption)
    }

    private fun getMenuItemForFileListOption(fileListOption: FileListOption?): Int = when (fileListOption) {
        FileListOption.SPACES_LIST -> R.id.nav_spaces
        FileListOption.SHARED_BY_LINK -> R.id.nav_shared_by_link_files
        FileListOption.AV_OFFLINE -> R.id.nav_available_offline_files
        else -> R.id.nav_all_files
    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }

    private fun startListeningToOperations() {
        onDeepLinkManaged()

        fileOperationsViewModel.copyFileLiveData.observe(this, Event.EventObserver {
            onCopyFileOperationFinish(it)
        })
        fileOperationsViewModel.moveFileLiveData.observe(this, Event.EventObserver {
            onMoveFileOperationFinish(it)
        })
        fileOperationsViewModel.removeFileLiveData.observe(this, Event.EventObserver {
            onRemoveFileOperationResult(it)
        })
        fileOperationsViewModel.renameFileLiveData.observe(this, Event.EventObserver {
            onRenameFileOperationFinish(it)
        })
        fileOperationsViewModel.syncFileLiveData.observe(this, Event.EventObserver {
            onSynchronizeFileOperationFinish(it)
        })
        fileOperationsViewModel.refreshFolderLiveData.observe(this, Event.EventObserver {
            onSynchronizeFolderOperationFinish(it)
        })
        fileOperationsViewModel.syncFolderLiveData.observe(this, Event.EventObserver {
            onSynchronizeFolderOperationFinish(it)
        })
    }

    override fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace?) {
        updateToolbar(newCurrentFolder, currentSpace)
        file = newCurrentFolder
    }

    override fun onFileClicked(file: OCFile) {
        when {
            PreviewImageFragment.canBePreviewed(file) -> {
                // preview image - it handles the sync, if needed
                startImagePreview(file)
            }

            PreviewTextFragment.canBePreviewed(file) -> {
                setFile(file)
                fileWaitingToPreview = file
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            }

            PreviewAudioFragment.canBePreviewed(file) -> {
                setFile(file)
                fileWaitingToPreview = file
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            }

            PreviewVideoActivity.canBePreviewed(file) && !WorkManager.getInstance(this).isDownloadPending(account, file) -> {
                // Available offline but not downloaded yet, don't initialize streaming
                if (!file.isAvailableLocally && file.isAvailableOffline) {
                    // sync file content, then open with external apps
                    startSyncThenOpen(file)
                } else {
                    // media preview
                    startVideoPreview(file, 0)
                }

                // If the file is already downloaded sync it, just to update it if there is a
                // new available file version
                if (file.isAvailableLocally) {
                    fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
                }
            }

            else -> {
                startSyncThenOpen(file)
            }
        }
        fileOperationsViewModel.setLastUsageFile(file)
    }

    override fun onShareFileClicked(file: OCFile) {
        fileOperationsHelper.showShareFile(file)
    }

    override fun initDownloadForSending(file: OCFile) {
        startDownloadForSending(file)
    }

    override fun cancelFileTransference(files: ArrayList<OCFile>) {
        transfersViewModel.cancelTransfersRecursively(files, account.name)
    }

    override fun setBottomBarVisibility(isVisible: Boolean) {
        showBottomNavBar(isVisible)
    }

    override fun uploadFromCamera() {
        filesUploadHelper?.uploadFromCamera(REQUEST_CODE__UPLOAD_FROM_CAMERA)
    }

    override fun uploadShortcutFileFromApp(shortcutFilePath: Array<String>) {
        requestUploadOfFilesFromFileSystem(shortcutFilePath, UploadBehavior.MOVE.toLegacyLocalBehavior())
    }

    override fun uploadFromFileSystem() {
        val action = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            setType(ALL_FILES_SAF_REGEX).addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(
            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
            REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        )
    }

    private fun handleDeepLink() {
        intent.data?.let { uri ->
            fileOperationsViewModel.handleDeepLink(uri, getCurrentOwnCloudAccount(baseContext).name)
        }
    }

    private fun onDeepLinkManaged() {
        collectLatestLifecycleFlow(fileOperationsViewModel.deepLinkFlow) {
            it?.getContentIfNotHandled()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> {
                        showLoadingDialog(R.string.deep_link_loading)
                    }

                    is UIResult.Success -> {
                        intent?.data = null
                        dismissLoadingDialog()
                        uiResult.data?.let { it1 -> manageItem(it1) }
                    }

                    is UIResult.Error -> {
                        dismissLoadingDialog()
                        if (uiResult.error is FileNotFoundException) {
                            showMessageInSnackbar(message = getString(R.string.deep_link_user_no_access))
                            changeUser()
                        } else {
                            showMessageInSnackbar(
                                message = getString(
                                    if (uiResult.error is DeepLinkException) {
                                        R.string.invalid_deep_link_format
                                    } else {
                                        R.string.default_error_msg
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeUser() {
        val currentUser = getCurrentOwnCloudAccount(this)
        val usersChecked = intent?.getStringArrayListExtra(KEY_DEEP_LINK_ACCOUNTS_CHECKED) ?: arrayListOf()
        usersChecked.add(currentUser.name)
        com.owncloud.android.presentation.authentication.AccountUtils.getAccounts(this).forEach {
            if (!usersChecked.contains(it.name)) {
                MainApp.initDependencyInjection()
                val i = Intent(
                    this,
                    FileDisplayActivity::class.java
                )
                i.data = intent?.data
                i.putExtra(EXTRA_ACCOUNT, it)
                i.putExtra(KEY_DEEP_LINK_ACCOUNTS_CHECKED, usersChecked)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
            }
        }
    }

    private fun manageItem(file: OCFile) {
        setFile(file)
        account = com.owncloud.android.presentation.authentication.AccountUtils.getOwnCloudAccountByName(this, file.owner)

        if (file.isFolder) {
            refreshListOfFilesFragment()
        } else {
            initFragmentsWithFile()
            onFileClicked(file)
        }
    }

    companion object {
        private const val TAG_LIST_OF_FILES = "LIST_OF_FILES"
        private const val TAG_LIST_OF_SPACES = "LIST_OF_SPACES"
        private const val TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT"

        private const val KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW"
        private const val KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS"
        private const val KEY_WAITING_TO_SEND = "WAITING_TO_SEND"
        private const val KEY_UPLOAD_HELPER = "FILE_UPLOAD_HELPER"
        private const val KEY_FILE_LIST_OPTION = "FILE_LIST_OPTION"
        private const val MAX_URL_LENGTH = 90
        const val MIMETYPE_TEXT_URI_LIST = "text/uri-list"
        const val KEY_DEEP_LINK_ACCOUNTS_CHECKED = "DEEP_LINK_ACCOUNTS_CHECKED"

        private const val CUSTOM_DIALOG_TAG = "CUSTOM_DIALOG"

        private const val PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED = "PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED"
        const val PREFERENCE_CLEAR_DATA_ALREADY_TRIGGERED = "PREFERENCE_CLEAR_DATA_ALREADY_TRIGGERED"
        const val ALL_FILES_SAF_REGEX = "*/*"

        const val ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS"

        const val REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1
        const val REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 2
        const val REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 3
        const val REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 4
        const val RESULT_OK_AND_MOVE = RESULT_FIRST_USER
        const val PROTOCOL_HTTPS = "https://"
        const val PROTOCOL_HTTP = "http://"
    }
}
