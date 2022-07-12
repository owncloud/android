/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * @author Jose Antonio Barros Ramos
 * Copyright (C) 2022 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.ui.files.filelist

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.databinding.MainFileListFragmentBinding
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.adapters.filelist.FileListAdapter
import com.owncloud.android.presentation.ui.common.BottomSheetFragmentItemView
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.Companion.newInstance
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.SortDialogListener
import com.owncloud.android.presentation.ui.files.SortOptionsView
import com.owncloud.android.presentation.ui.files.SortOrder
import com.owncloud.android.presentation.ui.files.SortType
import com.owncloud.android.presentation.ui.files.ViewType
import com.owncloud.android.presentation.ui.files.createfolder.CreateFolderDialogFragment
import com.owncloud.android.presentation.ui.files.operations.FileOperation
import com.owncloud.android.presentation.ui.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.ui.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.utils.ColumnQuantity
import com.owncloud.android.utils.FileStorageUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainFileListFragment : Fragment(),
    CreateFolderDialogFragment.CreateFolderListener,
    FileListAdapter.FileListAdapterListener,
    SearchView.OnQueryTextListener,
    SortDialogListener,
    SortOptionsView.CreateFolderListener,
    SortOptionsView.SortOptionsListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel>()
    private val fileOperationsViewModel by viewModel<FileOperationsViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private var files: List<OCFile> = emptyList()

    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var viewType: ViewType

    private var fileListOption: FileListOption = FileListOption.ALL_FILES

    private var file: OCFile? = null

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

        fileListOption = requireArguments().getParcelable(ARG_LIST_FILE_OPTION) ?: FileListOption.ALL_FILES

        showOrHideFab(fileListOption)
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
        if (isPickingAFolder()) {
            menu.removeItem(menu.findItem(R.id.action_share_current_folder).itemId)
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
            listener = this@MainFileListFragment
        )

        binding.recyclerViewMainFileList.adapter = fileListAdapter

        // Set Swipe to refresh and its listener
        binding.swipeRefreshMainFileList.setOnRefreshListener {
            mainFileListViewModel.syncFolder(
                ocFolder = mainFileListViewModel.getFile(),
                syncJustAlreadyDownloadedFiles = true,
                syncFoldersRecursively = false,
            )
        }

        // Set SortOptions and its listeners
        binding.optionsLayout.let {
            it.onSortOptionsListener = this
            if (isPickingAFolder()) {
                it.onCreateFolderListener = this
                it.selectAdditionalView(SortOptionsView.AdditionalView.CREATE_FOLDER)
            }
        }
    }

    private fun toggleSelection(position: Int) {
        fileListAdapter.toggleSelection(position)
        updateActionModeAfterTogglingSelected()
    }

    private fun subscribeToViewModels() {
        // Observe the current folder displayed and notify the listener
        mainFileListViewModel.currentFileLiveData.observe(viewLifecycleOwner) {
            fileActions?.onCurrentFolderUpdated(it)
        }
        mainFileListViewModel.folderContentLiveData.observe(viewLifecycleOwner) {}

        // Observe the file list ui state.
        mainFileListViewModel.fileListUiStateLiveData.observe(viewLifecycleOwner) { fileListUiState ->
            val fileListPreFilters = fileListUiState.folderContent
            val searchFilter = fileListUiState.searchFilter
            val fileListOption = fileListUiState.fileListOption

            val fileListPostFilters = fileListPreFilters
                .filter { fileToFilter ->
                    fileToFilter.fileName.contains(searchFilter, ignoreCase = true)
                }
                .filter {
                    when (fileListOption) {
                        FileListOption.AV_OFFLINE -> it.keepInSync == 1
                        FileListOption.SHARED_BY_LINK -> it.sharedByLink || it.sharedWithSharee == true
                        else -> true
                    }
                }

            updateFileListData(fileListPostFilters)
        }

        mainFileListViewModel.refreshFolder.observe(viewLifecycleOwner, Event.EventObserver {
            binding.syncProgressBar.isIndeterminate = it.isLoading
            binding.swipeRefreshMainFileList.isRefreshing = it.isLoading
        })
    }

    private fun updateFileListData(filesList: List<OCFile>) {
        files = filesList
        val sortedFiles = mainFileListViewModel.sortList(files)
        fileListAdapter.updateFileList(filesToAdd = sortedFiles)
        showOrHideEmptyView(sortedFiles)
    }

    fun navigateToFolderId(folderId: Long) {
        mainFileListViewModel.navigateTo(folderId)
    }

    fun listDirectory(directory: OCFile) {
        mainFileListViewModel.updateFolderToDisplay(newFolderToDisplay = directory)
    }

    private fun showOrHideEmptyView(filesList: List<OCFile>) {
        val isFileListEmpty = filesList.isEmpty()
        binding.recyclerViewMainFileList.isVisible = !isFileListEmpty

        with(binding.emptyDataParent) {
            root.isVisible = isFileListEmpty
            listEmptyDatasetSubTitle.text = mainFileListViewModel.getMessageForEmptyList(isPickingAFolder())
            // We should use a different icon and title for each [FileListOption]
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

        val isAscending = binding.optionsLayout.sortOrderSelected == SortOrder.SORT_ORDER_ASCENDING

        when (sortType) {
            SortType.SORT_TYPE_BY_NAME -> sortAdapterBy(FileStorageUtils.SORT_NAME, isAscending)
            SortType.SORT_TYPE_BY_DATE -> sortAdapterBy(FileStorageUtils.SORT_DATE, isAscending)
            SortType.SORT_TYPE_BY_SIZE -> sortAdapterBy(FileStorageUtils.SORT_SIZE, isAscending)
        }
    }

    private fun sortAdapterBy(sortType: Int, isDescending: Boolean) {
        PreferenceManager.setSortOrder(sortType, requireContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        PreferenceManager.setSortAscending(isDescending, requireContext(), FileStorageUtils.FILE_DISPLAY_SORT)

        val sortedFiles = mainFileListViewModel.sortList(files)
        fileListAdapter.updateFileList(filesToAdd = sortedFiles)
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
        fileListOption = newFileListOption
        mainFileListViewModel.updateFolderToDisplay(file)
        mainFileListViewModel.updateFileListOption(newFileListOption)
        showOrHideFab(newFileListOption)
    }

    /**
     * Check whether the fab should be shown or hidden depending on the [FileListOption]
     *
     * Show FAB when [FileListOption.ALL_FILES] and not picking a folder
     * Hide FAB When [FileListOption.SHARED_BY_LINK], [FileListOption.AV_OFFLINE] or picking a folder
     *
     * @param newFileListOption new file list option to enable.
     */
    private fun showOrHideFab(newFileListOption: FileListOption) {
        if (!newFileListOption.isAllFiles() || isPickingAFolder()) {
            toggleFabVisibility(false)
        } else {
            toggleFabVisibility(true)

            registerFabUploadListener()
            registerFabMkDirListener()
        }
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     *
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     * Mini FABs are automatically hidden after hiding the main one.
     *
     * @param shouldBeShown Desired visibility for the FAB.
     */
    private fun toggleFabVisibility(shouldBeShown: Boolean) {
        binding.fabMain.isVisible = shouldBeShown
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
        mainFileListViewModel.manageBrowseUp(fileListOption)
    }

    /**
     * Use this to query the [OCFile] that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    fun getCurrentFile(): OCFile? {
        return mainFileListViewModel.getFile()
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
        val checkedFiles = fileListAdapter.getCheckedItems() as ArrayList<OCFile>

        if (checkedFiles.isEmpty()) {
            return false
        } else if (checkedFiles.size == 1) {
            /// action only possible on a single file
            val singleFile = checkedFiles.first()
            when (menuId) {
                R.id.action_share_file -> {
                    fileActions?.onShareFileClicked(singleFile)
                    return true
                }
                R.id.action_open_file_with -> {
                    fileActions?.openFile(singleFile)
                    return true
                }
                R.id.action_rename_file -> {
                    val dialog = RenameFileDialogFragment.newInstance(singleFile)
                    dialog.show(requireActivity().supportFragmentManager, FileDetailFragment.FTAG_RENAME_FILE)
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
            }
        }

        /// Actions possible on a batch of files
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
                // TODO Waiting to be implemented
                //containerActivity?.fileOperationsHelper?.toggleAvailableOffline(checkedFiles, true)
                //getListView().invalidateViews()
                return true
            }
            R.id.action_unset_available_offline -> {
                // TODO Waiting to be implemented
                //containerActivity?.fileOperationsHelper?.toggleAvailableOffline(checkedFiles, false)
                //getListView().invalidateViews()
                //invalidateActionMode()
                /*if (fileListOption?.isAvailableOffline() == true) {
                    onRefresh()
                }*/
                return true
            }
            R.id.action_move -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_OPTION, FolderPickerActivity.PickerMode.MOVE)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES)
                fileListAdapter.clearSelection()
                updateActionModeAfterTogglingSelected()
                return true
            }
            R.id.action_copy -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                action.putExtra(FolderPickerActivity.EXTRA_PICKER_OPTION, FolderPickerActivity.PickerMode.COPY)
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

    override fun onItemClick(ocFile: OCFile, position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }

        if (ocFile.isFolder) {
            mainFileListViewModel.updateFolderToDisplay(ocFile)
            mainFileListViewModel.syncFolder(
                ocFolder = ocFile,
                syncJustAlreadyDownloadedFiles = true,
                syncFoldersRecursively = false
            )
        } else { // Click on a file
            fileActions?.onFileClicked(ocFile)
        }
    }

    override fun onLongItemClick(ocFile: OCFile, position: Int): Boolean {
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
            val checkedFiles = fileListAdapter.getCheckedItems()
            val checkedCount = checkedFiles.size
            val title = resources.getQuantityString(
                R.plurals.items_selected_count,
                checkedCount,
                checkedCount
            )
            mode?.title = title
            val fileMenuFilter = FileMenuFilter(
                checkedFiles,
                AccountUtils.getCurrentOwnCloudAccount(requireContext()),
                requireActivity() as FileActivity,
                activity
            )

            fileMenuFilter.filter(
                menu,
                checkedCount != fileListAdapter.itemCount - 1, // -1 because one of them is the footer :S
                true,
                fileListOption.isAvailableOffline(),
                fileListOption.isSharedByLink()
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

            // show FAB on multi selection mode exit
            toggleFabVisibility(true)

            fileActions?.setBottomBarVisibility(true)

            // Show sort options view when multi-selection mode finish
            binding.optionsLayout.visibility = View.VISIBLE

            fileListAdapter.clearSelection()
        }
    }

    private fun syncFiles(files: List<OCFile>) {
        for (file in files) {
            if (file.isFolder) {
                mainFileListViewModel.syncFolder(
                    ocFolder = file,
                    syncFoldersRecursively = true,
                    syncJustAlreadyDownloadedFiles = false
                )
            } else {
                fileActions?.syncFile(file)
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
        fun onCurrentFolderUpdated(newCurrentFolder: OCFile)
        fun onFileClicked(file: OCFile)
        fun onShareFileClicked(file: OCFile)
        fun initDownloadForSending(file: OCFile)
        fun showDetails(file: OCFile)
        fun syncFile(file: OCFile)
        fun openFile(file: OCFile)
        fun sendDownloadedFile(file: OCFile)
        fun cancelFileTransference(file: ArrayList<OCFile>)
        fun setBottomBarVisibility(isVisible: Boolean)
    }

    interface UploadActions {
        fun uploadFromCamera()
        fun uploadFromFileSystem()
    }

    companion object {
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"
        val ARG_LIST_FILE_OPTION = "${MainFileListFragment::class.java.canonicalName}.LIST_FILE_OPTION}"

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"

        @JvmStatic
        fun newInstance(
            pickingAFolder: Boolean = false
        ): MainFileListFragment {
            val args = Bundle()
            args.putBoolean(ARG_PICKING_A_FOLDER, pickingAFolder)
            return MainFileListFragment().apply { arguments = args }
        }
    }
}

