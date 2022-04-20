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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.material.snackbar.Snackbar
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
import com.owncloud.android.presentation.fold
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.Companion.newInstance
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.SortDialogListener
import com.owncloud.android.presentation.ui.files.SortOptionsView
import com.owncloud.android.presentation.ui.files.SortOrder
import com.owncloud.android.presentation.ui.files.SortType
import com.owncloud.android.presentation.ui.files.ViewType
import com.owncloud.android.presentation.ui.files.createfolder.CreateFolderDialogFragment
import com.owncloud.android.presentation.ui.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.viewmodels.files.FilesViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.ColumnQuantity
import com.owncloud.android.utils.FileStorageUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.ArrayList

class MainFileListFragment : Fragment(),
    CreateFolderDialogFragment.CreateFolderListener,
    FileListAdapter.FileListAdapterListener,
    SearchView.OnQueryTextListener,
    SortDialogListener,
    SortOptionsView.CreateFolderListener,
    SortOptionsView.SortOptionsListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel>()
    private val filesViewModel by viewModel<FilesViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private var containerActivity: FileFragment.ContainerActivity? = null
    private var files: List<OCFile> = emptyList()

    private var miniFabClicked = false
    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var viewType: ViewType

    private var fileListOption: FileListOption = FileListOption.ALL_FILES

    private var file: OCFile? = null

    var actionMode: ActionMode? = null

    var enableSelectAll = true

    private var statusBarColorActionMode: Int? = null
    private var statusBarColor: Int? = null

    var fileActions: FileActions? = null

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

        updateFab(fileListOption)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            maxWidth = Int.MAX_VALUE
            queryHint = resources.getString(R.string.actionbar_search)
            setOnQueryTextListener(this@MainFileListFragment)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.v("onAttach")
        containerActivity = try {
            context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw ClassCastException(
                context.toString() + " must implement " +
                        FileFragment.ContainerActivity::class.java.simpleName
            )
        }
    }

    override fun onDetach() {
        containerActivity = null
        super.onDetach()
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
            filesViewModel.refreshFolder(mainFileListViewModel.getFile().remotePath)
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
        fileListAdapter.selectedItemCount.also {
            if (it == 0) {
                actionMode?.finish()
            } else {
                actionMode?.apply {
                    title = it.toString()
                    invalidate()
                }
            }
        }
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

        filesViewModel.refreshFolder.observe(viewLifecycleOwner, Event.EventObserver {
            it.fold(
                onLoading = { binding.swipeRefreshMainFileList.isRefreshing = true },
                onSuccess = { binding.swipeRefreshMainFileList.isRefreshing = false },
                onFailure = { binding.swipeRefreshMainFileList.isRefreshing = false }
            )
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
        binding.emptyDataParent.root.isVisible = isFileListEmpty
        binding.recyclerViewMainFileList.isVisible = !isFileListEmpty
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
        updateFab(newFileListOption)
    }

    private fun updateFab(newFileListOption: FileListOption) {
        if (!newFileListOption.isAllFiles() || isPickingAFolder()) {
            setFabEnabled(false)
        } else {
            setFabEnabled(true)
            registerFabListeners()

            // detect if a mini FAB has ever been clicked
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            if (prefs.getLong(KEY_FAB_EVER_CLICKED, 0) > 0) {
                miniFabClicked = true
            }

            // add labels to the min FABs when none of them has ever been clicked on
            if (!miniFabClicked) {
                setFabLabels()
            } else {
                removeFabLabels()
            }
        }
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     *
     *
     * When 'false' is set, FAB visibility is set to View.GONE programatically,
     *
     * @param enabled Desired visibility for the FAB.
     */
    private fun setFabEnabled(enabled: Boolean) {
        binding.fabMain.isVisible = enabled
    }

    /**
     * Adds labels to all mini FABs.
     */
    private fun setFabLabels() {
        binding.fabUpload.title = resources.getString(R.string.actionbar_upload)
        binding.fabMkdir.title = resources.getString(R.string.actionbar_mkdir)
    }

    /**
     * Removes the labels on all known min FABs.
     */
    private fun removeFabLabels() {
        binding.fabUpload.title = null
        binding.fabMkdir.title = null
        ((binding.fabUpload.getTag(com.getbase.floatingactionbutton.R.id.fab_label)) as TextView).isVisible = false
        ((binding.fabMkdir.getTag(com.getbase.floatingactionbutton.R.id.fab_label)) as TextView).isVisible = false
    }

    /**
     * registers all listeners on all mini FABs.
     */
    private fun registerFabListeners() {
        // TODO Register upload listener
        registerFabMkDirListeners()
    }

    /**
     * Registers [android.view.View.OnClickListener] and [android.view.View.OnLongClickListener]
     * on the 'Create Dir' mini FAB for the linked action and [Snackbar] showing the underlying action.
     */
    private fun registerFabMkDirListeners() {
        binding.fabMkdir.setOnClickListener {
            val dialog = CreateFolderDialogFragment.newInstance(mainFileListViewModel.getFile(), this)
            dialog.show(requireActivity().supportFragmentManager, DIALOG_CREATE_FOLDER)
            binding.fabMain.collapse()
            recordMiniFabClick()
        }
        binding.fabMkdir.setOnLongClickListener {
            showMessageInSnackbar(
                message = getString(R.string.actionbar_mkdir)
            )
            true
        }
    }

    /**
     * records a click on a mini FAB and thus:
     *
     *  1. persists the click fact
     *  1. removes the mini FAB labels
     *
     */
    private fun recordMiniFabClick() {
        // only record if it hasn't been done already at some other time
        if (!miniFabClicked) {
            val sp = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
            sp.edit().putLong(KEY_FAB_EVER_CLICKED, 1).apply()
            miniFabClicked = true
        }
    }

    override fun onFolderNameSet(newFolderName: String, parentFolder: OCFile) {
        filesViewModel.createFolder(parentFolder, newFolderName)
        filesViewModel.createFolder.observe(viewLifecycleOwner, Event.EventObserver { uiResult: UIResult<Unit> ->
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

    fun getFabMain(): FloatingActionsMenu {
        return binding.fabMain
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
                    containerActivity?.fileOperationsHelper?.showShareFile(singleFile)
                    enableSelectAll = false
                    return true
                }
                R.id.action_open_file_with -> {
                    containerActivity?.fileOperationsHelper?.openFile(singleFile)
                    return true
                }
                R.id.action_rename_file -> {
                    val dialog = RenameFileDialogFragment.newInstance(singleFile)
                    dialog.show(requireActivity().supportFragmentManager, FileDetailFragment.FTAG_RENAME_FILE)
                    return true
                }
                R.id.action_see_details -> {
                    if (actionMode != null) {
                        actionMode!!.finish()
                    }
                    containerActivity?.showDetails(singleFile)
                    return true
                }
                R.id.action_send_file -> {
                    //Obtain the file
                    if (!singleFile.isAvailableLocally) { // Download the file
                        Timber.d("%s : File must be downloaded", singleFile.remotePath)
                        fileActions?.initDownloadForSending(singleFile)
                    } else {
                        containerActivity?.fileOperationsHelper?.sendDownloadedFile(singleFile)
                    }
                    return true
                }
            }
        }

        /// actions possible on a batch of files
        when (menuId) {
            R.id.file_action_select_all -> {
                // Last item on list is the footer, so that element must be excluded from selection
                for (i in 0 until fileListAdapter.itemCount - 1) {
                    if (!fileListAdapter.isSelected(i)) {
                        toggleSelection(i)
                    }
                }
                return true
            }
            R.id.action_select_inverse -> {
                // Last item on list is the footer, so that element must be excluded from selection
                for (i in 0 until fileListAdapter.itemCount - 1) {
                    toggleSelection(i)
                }
                return true
            }
            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(checkedFiles)
                dialog.show(requireActivity().supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
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
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES)
                return true
            }
            R.id.action_copy -> {
                val action = Intent(activity, FolderPickerActivity::class.java)
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, checkedFiles)
                requireActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES)
                return true
            }
        }

        return false
    }

    override fun onItemClick(ocFile: OCFile, position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }

        if (ocFile.isFolder) {
            mainFileListViewModel.updateFolderToDisplay(ocFile)
            filesViewModel.refreshFolder(ocFile.remotePath)
        } else { // Click on a file
            fileActions?.onFileClicked(ocFile)
        }
    }

    override fun onLongItemClick(ocFile: OCFile, position: Int): Boolean {
        if (isPickingAFolder()) return false

        if (actionMode == null) {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
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

            //set gray color
            val window = activity?.window
            statusBarColor = window?.statusBarColor ?: -1

            //hide FAB in multi selection mode
            binding.fabMain.visibility = View.GONE
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
                containerActivity,
                activity
            )

            fileMenuFilter.filter(
                menu,
                enableSelectAll,
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
            setFabEnabled(true)

            fileActions?.setBottomBarVisibility(true)

            // Show sort options view when multi-selection mode finish
            binding.optionsLayout.visibility = View.VISIBLE

            fileListAdapter.clearSelection()
        }
    }

    fun syncFiles(files: Collection<OCFile?>) {
        for (file in files) {
            file?.let { syncFile(file) }
        }
    }

    fun syncFile(file: OCFile) {
        if (!file.isFolder) {
            // TODO Sync file
        } else {
            filesViewModel.refreshFolder(file.remotePath)
        }
    }

    fun getProgressBar(): ProgressBar {
        return binding.syncProgressBar
    }

    fun getShadowView(): View {
        return binding.shadowView
    }

    fun setMessageForEmptyList(message: String) {
        binding.emptyDataParent.root.visibility = View.VISIBLE
        binding.emptyDataParent.apply {
            listEmptyDatasetIcon.visibility = View.GONE
            listEmptyDatasetTitle.visibility = View.GONE
            listEmptyDatasetSubTitle.text = message
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
        fun initDownloadForSending(file: OCFile)
        fun cancelFileTransference(file: ArrayList<OCFile>)
        fun setBottomBarVisibility(isVisible: Boolean)
    }

    companion object {
        private val MY_PACKAGE = MainFileListFragment::class.java.`package`.name ?: "com.owncloud.android.ui.fragment"
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"
        val ARG_LIST_FILE_OPTION = "${MainFileListFragment::class.java.canonicalName}.LIST_FILE_OPTION}"
        val KEY_FILE = "$MY_PACKAGE.extra.FILE"

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"
        private const val KEY_FAB_EVER_CLICKED = "FAB_EVER_CLICKED"

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

