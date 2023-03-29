/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * @author Jose Antonio Barros Ramos
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.files.filelist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.owncloud.android.R
import com.owncloud.android.databinding.MainFileListFragmentBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.model.OCFileSyncInfo
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.toDrawableRes
import com.owncloud.android.extensions.toSubtitleStringRes
import com.owncloud.android.extensions.toTitleStringRes
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.common.BottomSheetFragmentItemView
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.files.SortBottomSheetFragment
import com.owncloud.android.presentation.files.SortBottomSheetFragment.Companion.newInstance
import com.owncloud.android.presentation.files.SortBottomSheetFragment.SortDialogListener
import com.owncloud.android.presentation.files.SortOptionsView
import com.owncloud.android.presentation.files.SortOrder
import com.owncloud.android.presentation.files.SortType
import com.owncloud.android.presentation.files.ViewType
import com.owncloud.android.presentation.files.createfolder.CreateFolderDialogFragment
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment.Companion.FRAGMENT_TAG_RENAME_FILE
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class MainFileListFragment : Fragment(),
    CreateFolderDialogFragment.CreateFolderListener,
    FileListAdapter.FileListAdapterListener,
    SearchView.OnQueryTextListener,
    SortDialogListener,
    SortOptionsView.CreateFolderListener,
    SortOptionsView.SortOptionsListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel> {
        parametersOf(
            requireArguments().getParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY),
            requireArguments().getParcelable(ARG_FILE_LIST_OPTION),
        )
    }
    private val fileOperationsViewModel by sharedViewModel<FileOperationsViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var viewType: ViewType

    var actionMode: ActionMode? = null

    private var statusBarColorActionMode: Int? = null
    private var statusBarColor: Int? = null

    var fileActions: FileActions? = null
    var uploadActions: UploadActions? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFileListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        subscribeToViewModels()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            maxWidth = Int.MAX_VALUE
            queryHint = resources.getString(R.string.actionbar_search)
            setOnQueryTextListener(this@MainFileListFragment)
        }
        (menu.findItem(R.id.action_select_all)).setOnMenuItemClickListener {
            fileListAdapter.selectAll()
            updateActionModeAfterTogglingSelected()
            true
        }
        if (isPickingAFolder() || getCurrentSpace()?.isPersonal == false) {
            menu.findItem(R.id.action_share_current_folder)?.itemId?.let { menu.removeItem(it) }
        } else {
            menu.findItem(R.id.action_share_current_folder)?.setOnMenuItemClickListener {
                fileActions?.onShareFileClicked(mainFileListViewModel.getFile())
                true
            }
        }
    }

    private fun initViews() {
        setHasOptionsMenu(true)
        statusBarColorActionMode = ContextCompat.getColor(requireContext(), R.color.action_mode_status_bar_background)

        // Set view and footer correctly
        if (mainFileListViewModel.isGridModeSetAsPreferred()) {
            layoutManager =
                StaggeredGridLayoutManager(ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns(), RecyclerView.VERTICAL)
            viewType = ViewType.VIEW_TYPE_GRID
        } else {
            layoutManager = StaggeredGridLayoutManager(1, RecyclerView.VERTICAL)
            viewType = ViewType.VIEW_TYPE_LIST
        }

        binding.optionsLayout.viewTypeSelected = viewType

        // Set RecyclerView and its adapter.
        binding.recyclerViewMainFileList.layoutManager = layoutManager

        fileListAdapter = FileListAdapter(
            context = requireContext(),
            layoutManager = layoutManager,
            isPickerMode = isPickingAFolder(),
            listener = this@MainFileListFragment,
        )

        binding.recyclerViewMainFileList.adapter = fileListAdapter

        // Set Swipe to refresh and its listener
        binding.swipeRefreshMainFileList.setOnRefreshListener {
            fileOperationsViewModel.performOperation(
                FileOperation.RefreshFolderOperation(
                    folderToRefresh = mainFileListViewModel.getFile(),
                    shouldSyncContents = !isPickingAFolder(), // For picking a folder option, we just need a refresh
                )
            )
        }

        // Set SortOptions and its listeners
        binding.optionsLayout.onSortOptionsListener = this
        setViewTypeSelector(SortOptionsView.AdditionalView.CREATE_FOLDER)

        showOrHideFab(requireArguments().getParcelable(ARG_FILE_LIST_OPTION)!!, requireArguments().getParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY)!!)
    }

    private fun setViewTypeSelector(additionalView: SortOptionsView.AdditionalView) {
        if (isPickingAFolder()) {
            binding.optionsLayout.onCreateFolderListener = this
            binding.optionsLayout.selectAdditionalView(additionalView)
        }
    }

    private fun toggleSelection(position: Int) {
        fileListAdapter.toggleSelection(position)
        updateActionModeAfterTogglingSelected()
    }

    private fun subscribeToViewModels() {
        // Observe the current folder displayed
        collectLatestLifecycleFlow(mainFileListViewModel.currentFolderDisplayed) { currentFolderDisplayed: OCFile ->
            fileActions?.onCurrentFolderUpdated(currentFolderDisplayed, mainFileListViewModel.getSpace())
            val fileListOption = mainFileListViewModel.fileListOption.value
            val refreshFolderNeeded = fileListOption.isAllFiles() ||
                    (!fileListOption.isAllFiles() && currentFolderDisplayed.remotePath != ROOT_PATH)
            if (refreshFolderNeeded) {
                fileOperationsViewModel.performOperation(
                    FileOperation.RefreshFolderOperation(
                        folderToRefresh = currentFolderDisplayed,
                        shouldSyncContents = !isPickingAFolder(), // For picking a folder option, we just need a refresh
                    )
                )
            }
            showOrHideFab(fileListOption, currentFolderDisplayed)
            if (currentFolderDisplayed.hasAddSubdirectoriesPermission) {
                setViewTypeSelector(SortOptionsView.AdditionalView.CREATE_FOLDER)
            } else {
                setViewTypeSelector(SortOptionsView.AdditionalView.HIDDEN)
            }
        }
        // Observe the current space to update the toolbar.
        // We cant rely exclusively on the [currentFolderDisplayed] because sometimes retrieving the space takes more time
        collectLatestLifecycleFlow(mainFileListViewModel.space) { currentSpace: OCSpace? ->
            currentSpace?.let {
                fileActions?.onCurrentFolderUpdated(mainFileListViewModel.getFile(), currentSpace)
            }
        }

        // Observe the file list ui state
        collectLatestLifecycleFlow(mainFileListViewModel.fileListUiState) { fileListUiState ->
            if (fileListUiState !is MainFileListViewModel.FileListUiState.Success) return@collectLatestLifecycleFlow

            fileListAdapter.updateFileList(
                filesToAdd = fileListUiState.folderContent,
                fileListOption = fileListUiState.fileListOption,
            )
            showOrHideEmptyView(fileListUiState)

            fileListUiState.space?.let {
                binding.spaceHeader.root.apply {
                    if (fileListUiState.space.isProject && fileListUiState.folderToDisplay?.remotePath == ROOT_PATH) {
                        isVisible = true
                        animate().translationY(0f).duration = 100
                    } else {
                        animate().translationY(-height.toFloat()).withEndAction { isVisible = false }
                    }
                }

                val spaceSpecialImage = it.getSpaceSpecialImage()
                if (spaceSpecialImage != null) {
                    binding.spaceHeader.spaceHeaderImage.tag = spaceSpecialImage.id
                    val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(spaceSpecialImage.id)
                    if (thumbnail != null) {
                        binding.spaceHeader.spaceHeaderImage.run {
                            setImageBitmap(thumbnail)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        if (spaceSpecialImage.file.mimeType == "image/png") {
                            binding.spaceHeader.spaceHeaderImage.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_color))
                        }
                    }
                }
                binding.spaceHeader.spaceHeaderName.text = it.name
                binding.spaceHeader.spaceHeaderSubtitle.text = it.description
            }

            actionMode?.invalidate()
        }

        fileOperationsViewModel.refreshFolderLiveData.observe(viewLifecycleOwner) {
            binding.syncProgressBar.isIndeterminate = it.peekContent().isLoading
            binding.swipeRefreshMainFileList.isRefreshing = it.peekContent().isLoading
        }
    }

    fun navigateToFolderId(folderId: Long) {
        mainFileListViewModel.navigateToFolderId(folderId)
    }

    fun navigateToFolder(folder: OCFile) {
        mainFileListViewModel.updateFolderToDisplay(newFolderToDisplay = folder)
    }

    private fun showOrHideEmptyView(fileListUiState: MainFileListViewModel.FileListUiState.Success) {
        binding.recyclerViewMainFileList.isVisible = fileListUiState.folderContent.isNotEmpty()

        with(binding.emptyDataParent) {
            root.isVisible = fileListUiState.folderContent.isEmpty()

            if (fileListUiState.fileListOption.isSharedByLink() && fileListUiState.space != null) {
                // Temporary solution for shares space
                listEmptyDatasetIcon.setImageResource(R.drawable.ic_ocis_shares)
                listEmptyDatasetTitle.setText(R.string.shares_list_empty_title)
                listEmptyDatasetSubTitle.setText(R.string.shares_list_empty_subtitle)
            } else {
                listEmptyDatasetIcon.setImageResource(fileListUiState.fileListOption.toDrawableRes())
                listEmptyDatasetTitle.setText(fileListUiState.fileListOption.toTitleStringRes())
                listEmptyDatasetSubTitle.setText(fileListUiState.fileListOption.toSubtitleStringRes())
            }
        }
    }

    override fun onSortTypeListener(sortType: SortType, sortOrder: SortOrder) {
        val sortBottomSheetFragment = newInstance(sortType, sortOrder)
        sortBottomSheetFragment.sortDialogListener = this
        sortBottomSheetFragment.show(childFragmentManager, SortBottomSheetFragment.TAG)
    }

    override fun onViewTypeListener(viewType: ViewType) {
        binding.optionsLayout.viewTypeSelected = viewType

        if (viewType == ViewType.VIEW_TYPE_LIST) {
            mainFileListViewModel.setListModeAsPreferred()
            layoutManager.spanCount = 1

        } else {
            mainFileListViewModel.setGridModeAsPreferred()
            layoutManager.spanCount = ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns()
        }

        fileListAdapter.notifyItemRangeChanged(0, fileListAdapter.itemCount)
    }

    override fun onSortSelected(sortType: SortType) {
        binding.optionsLayout.sortTypeSelected = sortType

        mainFileListViewModel.updateSortTypeAndOrder(sortType, binding.optionsLayout.sortOrderSelected)
    }

    private fun isPickingAFolder(): Boolean {
        val args = arguments
        return args != null && args.getBoolean(ARG_PICKING_A_FOLDER, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun updateFileListOption(newFileListOption: FileListOption, file: OCFile) {
        mainFileListViewModel.updateFolderToDisplay(file)
        mainFileListViewModel.updateFileListOption(newFileListOption)
        showOrHideFab(newFileListOption, file)
    }

    /**
     * Check whether the fab should be shown or hidden depending on the [FileListOption] and
     * the current folder displayed permissions
     *
     * Show FAB when [FileListOption.ALL_FILES] and not picking a folder
     * Hide FAB When [FileListOption.SHARED_BY_LINK], [FileListOption.AV_OFFLINE] or picking a folder
     *
     * @param newFileListOption new file list option to enable.
     */
    private fun showOrHideFab(newFileListOption: FileListOption, currentFolder: OCFile) {
        if (!newFileListOption.isAllFiles() || isPickingAFolder() || (!currentFolder.hasAddFilePermission && !currentFolder.hasAddSubdirectoriesPermission)) {
            toggleFabVisibility(false)
        } else {
            toggleFabVisibility(true)
            if (!currentFolder.hasAddFilePermission) {
                binding.fabUpload.isVisible = false
            } else if (!currentFolder.hasAddSubdirectoriesPermission) {
                binding.fabMkdir.isVisible = false
            }
            registerFabUploadListener()
            registerFabMkDirListener()
        }
    }

    /**
     * Sets the 'visibility' state of the main FAB and its mini FABs contained in the fragment.
     *
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     * Mini FABs are automatically hidden after hiding the main one.
     *
     * @param shouldBeShown Desired visibility for the FAB.
     */
    private fun toggleFabVisibility(shouldBeShown: Boolean) {
        binding.fabMain.isVisible = shouldBeShown
        binding.fabUpload.isVisible = shouldBeShown
        binding.fabMkdir.isVisible = shouldBeShown
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'Create Dir' mini FAB for the linked action.
     */
    private fun registerFabMkDirListener() {
        binding.fabMkdir.setOnClickListener {
            val dialog = CreateFolderDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
            dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_FOLDER)
            collapseFab()
        }
    }

    /**
     * Registers [android.view.View.OnClickListener] on the 'Uplodd' mini FAB for the linked action.
     */
    private fun registerFabUploadListener() {
        binding.fabUpload.setOnClickListener {
            openBottomSheetToUploadFiles()
            collapseFab()
        }
    }

    fun collapseFab() {
        binding.fabMain.collapse()
    }

    fun isFabExpanded() = binding.fabMain.isExpanded

    private fun openBottomSheetToUploadFiles() {
        val uploadBottomSheet = layoutInflater.inflate(R.layout.upload_bottom_sheet_fragment, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(uploadBottomSheet)
        val uploadFromFilesItemView: BottomSheetFragmentItemView = uploadBottomSheet.findViewById(R.id.upload_from_files_item_view)
        val uploadFromCameraItemView: BottomSheetFragmentItemView = uploadBottomSheet.findViewById(R.id.upload_from_camera_item_view)
        val uploadToTextView = uploadBottomSheet.findViewById<TextView>(R.id.upload_to_text_view)
        uploadFromFilesItemView.setOnClickListener {
            uploadActions?.uploadFromFileSystem()
            dialog.hide()
        }
        uploadFromCameraItemView.setOnClickListener {
            uploadActions?.uploadFromCamera()
            dialog.hide()
        }
        uploadToTextView.text = String.format(
            resources.getString(R.string.upload_to),
            resources.getString(R.string.app_name)
        )
        val uploadBottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(uploadBottomSheet.parent as View)
        dialog.setOnShowListener { uploadBottomSheetBehavior.setPeekHeight(uploadBottomSheet.measuredHeight) }
        dialog.show()
    }

    override fun onFolderNameSet(newFolderName: String, parentFolder: OCFile) {
        fileOperationsViewModel.performOperation(FileOperation.CreateFolder(newFolderName, parentFolder))
        fileOperationsViewModel.createFolder.observe(viewLifecycleOwner, Event.EventObserver { uiResult: UIResult<Unit> ->
            if (uiResult is UIResult.Error) {
                val errorMessage =
                    uiResult.error?.parseError(resources.getString(R.string.create_dir_fail_msg), resources, false)
                showMessageInSnackbar(
                    message = errorMessage.toString()
                )
            }
        })
    }

    override fun onCreateFolderListener() {
        val dialog = CreateFolderDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
        dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_FOLDER)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let { mainFileListViewModel.updateSearchFilter(it) }
        return true
    }

    fun setSearchListener(searchView: SearchView) {
        searchView.setOnQueryTextListener(this)
    }

    /**
     * Call this, when the user presses the up button.
     *
     *
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     *
     *
     */
    fun onBrowseUp() {
        mainFileListViewModel.manageBrowseUp()
    }

    /**
     * Use this to query the [OCFile] that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    fun getCurrentFile(): OCFile {
        return mainFileListViewModel.getFile()
    }

    fun getCurrentSpace(): OCSpace? {
        return mainFileListViewModel.getSpace()
    }

    private fun setDrawerStatus(enabled: Boolean) {
        (activity as FileActivity).setDrawerLockMode(if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param menuId Identifier of the action menu selected by the user
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    @SuppressLint("UseRequireInsteadOfGet")
    private fun onFileActionChosen(menuId: Int?): Boolean {
        val checkedFilesWithSyncInfo = fileListAdapter.getCheckedItems() as ArrayList<OCFileWithSyncInfo>

        if (checkedFilesWithSyncInfo.isEmpty()) {
            return false
        } else if (checkedFilesWithSyncInfo.size == 1) {
            /// action only possible on a single file
            val singleFile = checkedFilesWithSyncInfo.first().file
            when (menuId) {
                R.id.action_share_file -> {
                    fileActions?.onShareFileClicked(singleFile)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }
                R.id.action_open_file_with -> {
                    fileActions?.openFile(singleFile)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }
                R.id.action_rename_file -> {
                    val dialog = RenameFileDialogFragment.newInstance(singleFile)
                    dialog.show(requireActivity().supportFragmentManager, FRAGMENT_TAG_RENAME_FILE)
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    return true
                }
                R.id.action_see_details -> {
                    fileListAdapter.clearSelection()
                    updateActionModeAfterTogglingSelected()
                    fileActions?.showDetails(singleFile)
                    return true
                }
                R.id.action_sync_file -> {
                    syncFiles(listOf(singleFile))
                    return true
                }
                R.id.action_send_file -> {
                    //Obtain the file
                    if (!singleFile.isAvailableLocally) { // Download the file
                        Timber.d("%s : File must be downloaded", singleFile.remotePath)
                        fileActions?.initDownloadForSending(singleFile)
                    } else {
                        fileActions?.sendDownloadedFile(singleFile)
                    }
                    return true
                }
                R.id.action_set_available_offline -> {
                    fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(listOf(singleFile)))
                    if (singleFile.isFolder) {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFolderOperation(singleFile, singleFile.owner))
                    } else {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(singleFile, singleFile.owner))
                    }
                    return true
                }
                R.id.action_unset_available_offline -> {
                    fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(listOf(singleFile)))
                }
            }
        }

        /// Actions possible on a batch of files
        val checkedFiles = checkedFilesWithSyncInfo.map { it.file } as ArrayList<OCFile>
        when (menuId) {
            R.id.file_action_select_all -> {
                fileListAdapter.selectAll()
                updateActionModeAfterTogglingSelected()
                return true
            }
            R.id.action_select_inverse -> {
                fileListAdapter.selectInverse()
                updateActionModeAfterTogglingSelected()
                return true
            }
            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(checkedFiles)
                dialog.show(requireActivity().supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }
            R.id.action_download_file,
            R.id.action_sync_file -> {
                syncFiles(checkedFiles)
                return true
            }
            R.id.action_cancel_sync -> {
                fileActions?.cancelFileTransference(checkedFiles)
                return true
            }
            R.id.action_set_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(checkedFiles))
                checkedFiles.forEach { ocFile ->
                    if (ocFile.isFolder) {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFolderOperation(ocFile, ocFile.owner))
                    } else {
                        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(ocFile, ocFile.owner))
                    }
                }
                return true
            }
            R.id.action_unset_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(checkedFiles))
                return true
            }
            R.id.action_send_file -> {
                requireActivity().sendDownloadedFilesByShareSheet(checkedFiles)
            }
            R.id.action_move -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.MOVE)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }
            R.id.action_copy -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_MODE, FolderPickerActivity.PickerMode.COPY)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }
        }

        return false
    }

    /**
     * Update or remove the actionMode after applying any change to the selected items.
     */
    private fun updateActionModeAfterTogglingSelected() {
        val selectedItems = fileListAdapter.selectedItemCount
        if (selectedItems == 0) {
            actionMode?.finish()
        } else {
            if (actionMode == null) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
            }
            actionMode?.apply {
                title = selectedItems.toString()
                invalidate()
            }
        }
    }

    override fun onItemClick(ocFileWithSyncInfo: OCFileWithSyncInfo, position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }

        val ocFile = ocFileWithSyncInfo.file

        if (ocFile.isFolder) {
            mainFileListViewModel.updateFolderToDisplay(ocFile)
        } else { // Click on a file
            fileActions?.onFileClicked(ocFile)
        }
    }

    override fun onLongItemClick(position: Int): Boolean {
        if (isPickingAFolder()) return false

        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
            // Notify all when enabling multiselection for the first time to show checkboxes on every single item.
            fileListAdapter.notifyDataSetChanged()
        }
        toggleSelection(position)
        return true
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            setDrawerStatus(enabled = false)
            actionMode = mode

            val inflater = requireActivity().menuInflater
            inflater.inflate(R.menu.file_actions_menu, menu)
            mode?.invalidate()

            // Set gray color
            val window = activity?.window
            statusBarColor = window?.statusBarColor ?: -1

            // Hide FAB in multi selection mode
            toggleFabVisibility(false)
            fileActions?.setBottomBarVisibility(false)

            // Hide sort options view in multi-selection mode
            binding.optionsLayout.visibility = View.GONE

            return true
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val checkedFilesWithSyncInfo = fileListAdapter.getCheckedItems()
            val checkedCount = checkedFilesWithSyncInfo.size
            val title = resources.getQuantityString(
                R.plurals.items_selected_count,
                checkedCount,
                checkedCount
            )
            mode?.title = title

            val checkedFiles = checkedFilesWithSyncInfo.map { it.file }

            val checkedFilesSync = checkedFilesWithSyncInfo.map {
                OCFileSyncInfo(
                    fileId = it.file.id!!,
                    uploadWorkerUuid = it.uploadWorkerUuid,
                    downloadWorkerUuid = it.downloadWorkerUuid,
                    isSynchronizing = it.isSynchronizing
                )
            }

            val fileMenuFilter = FileMenuFilter(
                checkedFiles,
                AccountUtils.getCurrentOwnCloudAccount(requireContext()),
                requireActivity() as FileActivity,
                activity,
                checkedFilesSync
            )

            fileMenuFilter.filter(
                menu,
                checkedCount != fileListAdapter.itemCount - 1, // -1 because one of them is the footer :S
                true,
                mainFileListViewModel.fileListOption.value.isAvailableOffline(),
                mainFileListViewModel.fileListOption.value.isSharedByLink(),
            )

            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return onFileActionChosen(item?.itemId)
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            setDrawerStatus(enabled = true)
            actionMode = null

            // reset to previous color
            requireActivity().window.statusBarColor = statusBarColor!!

            // show or hide FAB on multi selection mode exit
            showOrHideFab(mainFileListViewModel.fileListOption.value, mainFileListViewModel.currentFolderDisplayed.value)

            fileActions?.setBottomBarVisibility(true)

            // Show sort options view when multi-selection mode finish
            binding.optionsLayout.visibility = View.VISIBLE

            fileListAdapter.clearSelection()
        }
    }

    private fun syncFiles(files: List<OCFile>) {
        for (file in files) {
            if (file.isFolder) {
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFolderOperation(folderToSync = file, accountName = file.owner))
            } else {
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(fileToSync = file, accountName = file.owner))
            }
        }
    }

    fun setProgressBarAsIndeterminate(indeterminate: Boolean) {
        Timber.d("Setting progress visibility to %s", indeterminate)
        binding.shadowView.visibility = View.GONE
        binding.syncProgressBar.apply {
            visibility = View.VISIBLE
            isIndeterminate = indeterminate
            postInvalidate()
        }
    }

    interface FileActions {
        fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace? = null)
        fun onFileClicked(file: OCFile)
        fun onShareFileClicked(file: OCFile)
        fun initDownloadForSending(file: OCFile)
        fun showDetails(file: OCFile)
        fun syncFile(file: OCFile)
        fun openFile(file: OCFile)
        fun sendDownloadedFile(file: OCFile)
        fun cancelFileTransference(files: ArrayList<OCFile>)
        fun setBottomBarVisibility(isVisible: Boolean)
    }

    interface UploadActions {
        fun uploadFromCamera()
        fun uploadFromFileSystem()
    }

    companion object {
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"
        val ARG_INITIAL_FOLDER_TO_DISPLAY = "${MainFileListFragment::class.java.canonicalName}.ARG_INITIAL_FOLDER_TO_DISPLAY}"
        val ARG_FILE_LIST_OPTION = "${MainFileListFragment::class.java.canonicalName}.FILE_LIST_OPTION}"

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"

        @JvmStatic
        fun newInstance(
            initialFolderToDisplay: OCFile,
            pickingAFolder: Boolean = false,
            fileListOption: FileListOption = FileListOption.ALL_FILES,
        ): MainFileListFragment {
            val args = Bundle()
            args.putParcelable(ARG_INITIAL_FOLDER_TO_DISPLAY, initialFolderToDisplay)
            args.putBoolean(ARG_PICKING_A_FOLDER, pickingAFolder)
            args.putParcelable(ARG_FILE_LIST_OPTION, fileListOption)
            return MainFileListFragment().apply { arguments = args }
        }
    }
}

