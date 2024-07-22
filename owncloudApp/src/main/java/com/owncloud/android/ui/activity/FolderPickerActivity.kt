/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesFolderPickerBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.files.filelist.MainFileListFragment
import com.owncloud.android.presentation.spaces.SpacesListFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

open class FolderPickerActivity : FileActivity(),
    FileFragment.ContainerActivity,
    MainFileListFragment.FileActions {

    protected val mainFileListFragment: MainFileListFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FOLDERS) as MainFileListFragment?

    private lateinit var pickerMode: PickerMode

    private lateinit var binding: FilesFolderPickerBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate() start")

        super.onCreate(savedInstanceState)

        binding = FilesFolderPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Allow or disallow touches with other visible windows
        binding.filesFolderPickerLayout.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        pickerMode = intent.getSerializableExtra(EXTRA_PICKER_MODE) as PickerMode

        if (savedInstanceState == null) {
            when (pickerMode) {
                PickerMode.MOVE -> {
                    // Show the space where the files come from
                    val targetFiles = intent.getParcelableArrayListExtra<OCFile>(EXTRA_FILES)
                    val spaceIdOfFiles = targetFiles?.get(0)?.spaceId
                    initAndShowListOfFilesFragment(spaceId = spaceIdOfFiles)
                }
                PickerMode.COPY -> {
                    val targetFiles = intent.getParcelableArrayListExtra<OCFile>(EXTRA_FILES)
                    if (targetFiles?.get(0)?.spaceId != null) {
                        // Show the list of spaces
                        initAndShowListOfSpaces()
                    } else {
                        // Show the personal space
                        initAndShowListOfFilesFragment(spaceId = null)
                    }
                }
                PickerMode.CAMERA_FOLDER -> {
                    val spaceId = intent.getStringExtra(KEY_SPACE_ID)

                    if (spaceId != null) {
                        // Show the list of spaces
                        initAndShowListOfSpaces()
                    } else {
                        val accountName = intent.getStringExtra(KEY_ACCOUNT_NAME)
                        account = AccountUtils.getOwnCloudAccountByName(this, accountName)
                        // Show the personal space
                        initAndShowListOfFilesFragment(spaceId = null)
                    }
                }
            }
        }

        // Set callback listeners for UI elements
        initPickerListeners()

        // Action bar setup
        setupStandardToolbar(
            title = null,
            displayHomeAsUpEnabled = false,
            homeButtonEnabled = false,
            displayShowTitleEnabled = true,
        )

        // Set action button text
        setActionButtonText()

        supportFragmentManager.setFragmentResultListener(SpacesListFragment.REQUEST_KEY_CLICK_SPACE, this) { _, bundle ->
            val rootSpaceFolder = bundle.getParcelable<OCFile>(SpacesListFragment.BUNDLE_KEY_CLICK_SPACE)
            file = rootSpaceFolder
            initAndShowListOfFilesFragment()
        }

        Timber.d("onCreate() end")
    }

    override fun onResume() {
        super.onResume()
        updateToolbar(null, mainFileListFragment?.getCurrentSpace())
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)

        if (account != null) {
            updateFileFromDB()

            var folder = file
            if (folder == null || !folder.isFolder) {
                // Fall back to root folder
                file = storageManager.getRootPersonalFolder()
                folder = file
            }

            if (!stateWasRecovered) {
                mainFileListFragment?.navigateToFolder(folder)
            }

            updateNavigationElementsInActionBar()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.removeItem(menu.findItem(R.id.action_share_current_folder)?.itemId ?: 0)
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

    override fun onBackPressed() {
        val currentDirDisplayed = mainFileListFragment?.getCurrentFile()
        // If current file is null (we are in the spaces list, for example), close the activity
        if (currentDirDisplayed == null) {
            finish()
            return
        }
        // If current file is root folder
        else if (currentDirDisplayed.parentId == OCFile.ROOT_PARENT_ID) {
            // If we are not in COPY mode, or if we are in COPY mode and spaces are not allowed, close the activity
            if (pickerMode != PickerMode.COPY || (pickerMode == PickerMode.COPY && currentDirDisplayed.spaceId == null)) {
                finish()
                return
            }
            // If we are in COPY mode and inside a space, navigate back to the spaces list
            if (mainFileListFragment?.getCurrentSpace()?.isProject == true || mainFileListFragment?.getCurrentSpace()?.isPersonal == true) {
                file = null
                initAndShowListOfSpaces()
                updateToolbar(null)
                binding.folderPickerNoPermissionsMessage.isVisible = false
            }
        } else {
            mainFileListFragment?.onBrowseUp()
        }
    }

    override fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace?) {
        updateToolbar(newCurrentFolder, currentSpace)
        updateButtonsVisibilityAccordingToPermissions(newCurrentFolder)
        file = newCurrentFolder
    }

    override fun initDownloadForSending(file: OCFile) {
        // Nothing to do. Downloading files is not allowed.
    }

    override fun cancelFileTransference(files: ArrayList<OCFile>) {
        // Nothing to do. Transferring files is not allowed.
    }

    override fun setBottomBarVisibility(isVisible: Boolean) {
        // Nothing to do. No changes will be done in the bottom bar visibility.
    }

    override fun onFileClicked(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun onShareFileClicked(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun syncFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun openFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun sendDownloadedFile(file: OCFile) {
        // Nothing to do. Clicking on files is not allowed.
    }

    override fun showDetails(file: OCFile) {
        // Nothing to do. Details can't be opened here.
    }

    private fun initAndShowListOfFilesFragment(spaceId: String? = null) {
        val safeInitialFolder = if (file == null) {
            if (account == null) {
                account = AccountUtils.getCurrentOwnCloudAccount(applicationContext)
            }
            val fileDataStorageManager = FileDataStorageManager(account)
            fileDataStorageManager.getFileByPath(OCFile.ROOT_PATH, spaceId)
        } else {
            file
        }

        file = safeInitialFolder

        safeInitialFolder?.let {
            val mainListOfFiles = MainFileListFragment.newInstance(it, true, FileListOption.ALL_FILES)
            mainListOfFiles.fileActions = this
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, mainListOfFiles, TAG_LIST_OF_FOLDERS)
            transaction.commit()
        }


        binding.folderPickerBtnChoose.isVisible = true
    }

    private fun initAndShowListOfSpaces() {
        val accountNameIntent = intent.getStringExtra(KEY_ACCOUNT_NAME)
        val accountName = accountNameIntent ?: AccountUtils.getCurrentOwnCloudAccount(applicationContext).name

        val listOfSpaces = SpacesListFragment.newInstance(showPersonalSpace = true, accountName = accountName)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, listOfSpaces)
        transaction.commit()
        binding.folderPickerBtnChoose.isVisible = false
    }

    /**
     * Set per-view controllers
     */
    private fun initPickerListeners() {

        binding.folderPickerBtnCancel.setOnClickListener {
            finish()
        }

        binding.folderPickerBtnChoose.setOnClickListener {
            val data = Intent().apply {
                val targetFiles = intent.getParcelableArrayListExtra<OCFile>(EXTRA_FILES)
                putExtra(EXTRA_FOLDER, getCurrentFolder())
                putParcelableArrayListExtra(EXTRA_FILES, targetFiles)
            }
            setResult(RESULT_OK, data)

            finish()
        }
    }

    private fun setActionButtonText() {
        binding.folderPickerBtnChoose.text = getString(pickerMode.toStringRes())
    }

    private fun getCurrentFolder(): OCFile? {
        if (mainFileListFragment != null) {
            return mainFileListFragment?.getCurrentFile()
        }
        return null
    }

    private fun updateToolbar(chosenFileFromParam: OCFile?, space: OCSpace? = null) {
        val chosenFile = chosenFileFromParam ?: file // If no file is passed, current file decides
        val isRootFromPersonalInCopyMode =
            chosenFile != null && chosenFile.remotePath == OCFile.ROOT_PATH && space?.isProject == false && pickerMode == PickerMode.COPY
        val isRootFromPersonal = chosenFile == null || (chosenFile.remotePath == OCFile.ROOT_PATH && (space == null || !space.isProject))
        val isRootFromProject = space?.isProject == true && chosenFile.remotePath == OCFile.ROOT_PATH

        if (isRootFromPersonalInCopyMode) {
            updateStandardToolbar(
                title = getString(R.string.default_display_name_for_root_folder),
                displayHomeAsUpEnabled = true,
                homeButtonEnabled = true
            )
        } else if (isRootFromPersonal) {
            updateStandardToolbar(
                title = getString(R.string.default_display_name_for_root_folder),
                displayHomeAsUpEnabled = false,
                homeButtonEnabled = false
            )
        } else if (isRootFromProject) {
            updateStandardToolbar(
                title = space!!.name,
                displayHomeAsUpEnabled = pickerMode == PickerMode.COPY,
                homeButtonEnabled = pickerMode == PickerMode.COPY
            )
        } else {
            updateStandardToolbar(title = chosenFile.fileName, displayHomeAsUpEnabled = true, homeButtonEnabled = true)
        }
    }

    private fun updateButtonsVisibilityAccordingToPermissions(currentFolder: OCFile) {
        currentFolder.hasAddFilePermission.let {
            binding.folderPickerBtnChoose.isVisible = it
            binding.folderPickerNoPermissionsMessage.isVisible = !it
        }
    }

    protected fun updateNavigationElementsInActionBar() {
        val currentDir = try {
            getCurrentFolder()
        } catch (e: NullPointerException) {
            file
        }

        val atRoot = (currentDir == null || currentDir.parentId == 0L)
        updateStandardToolbar(
            title = if (atRoot) getString(R.string.default_display_name_for_root_folder) else currentDir!!.fileName,
            displayHomeAsUpEnabled = !atRoot,
            homeButtonEnabled = !atRoot,
        )
    }

    enum class PickerMode {
        MOVE, COPY, CAMERA_FOLDER;

        @StringRes
        fun toStringRes(): Int {
            return when (this) {
                MOVE -> R.string.folder_picker_move_here_button_text
                COPY -> R.string.folder_picker_copy_here_button_text
                CAMERA_FOLDER -> R.string.folder_picker_choose_button_text
            }
        }
    }

    companion object {
        const val KEY_ACCOUNT_NAME = "KEY_ACCOUNT_NAME"
        const val KEY_SPACE_ID = "KEY_PERSONAL_SPACE_ID"
        const val EXTRA_FOLDER = "FOLDER_PICKER_EXTRA_FOLDER"
        const val EXTRA_FILES = "FOLDER_PICKER_EXTRA_FILES"
        const val EXTRA_PICKER_MODE = "FOLDER_PICKER_EXTRA_PICKER_MODE"
        private const val TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS"
    }
}

