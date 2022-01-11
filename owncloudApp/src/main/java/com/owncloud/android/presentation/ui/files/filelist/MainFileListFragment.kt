/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.databinding.MainFileListFragmentBinding
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.cancel
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.adapters.filelist.FileListAdapter
import com.owncloud.android.presentation.observers.EmptyDataObserver
import com.owncloud.android.presentation.onSuccess
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.Companion.newInstance
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment.SortDialogListener
import com.owncloud.android.presentation.ui.files.SortOptionsView
import com.owncloud.android.presentation.ui.files.SortOrder
import com.owncloud.android.presentation.ui.files.SortType
import com.owncloud.android.presentation.ui.files.ViewType
import com.owncloud.android.presentation.ui.files.createfolder.CreateFolderDialogFragment
import com.owncloud.android.presentation.viewmodels.files.FilesViewModel
import com.owncloud.android.ui.activity.FileListOption
import com.owncloud.android.utils.ColumnQuantity
import com.owncloud.android.utils.FileStorageUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.get

class MainFileListFragment : Fragment(), SortDialogListener, SortOptionsView.SortOptionsListener, SortOptionsView.CreateFolderListener,
    CreateFolderDialogFragment.CreateFolderListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel>()

    private val KEY_FAB_EVER_CLICKED = "FAB_EVER_CLICKED"
    private val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var files: List<OCFile>

    private var miniFabClicked = false
    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var viewType: ViewType

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

        var fileListOption: FileListOption? =
            if (requireArguments().getParcelable<Parcelable?>(ARG_LIST_FILE_OPTION) != null)
                requireArguments().getParcelable(ARG_LIST_FILE_OPTION)
            else
                null

        if (fileListOption == null) {
            fileListOption = FileListOption.ALL_FILES
        }
        updateFab(fileListOption)
    }

    /**
     * Operation to calculate the free space to apply to the footer and manage its visibility.
     */
    private fun setFooter() {
        view?.doOnPreDraw {
            val footerHeight = binding.footerText.height
            binding.recyclerViewMainFileList.updatePadding(bottom = footerHeight)
            binding.recyclerViewMainFileList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val range = recyclerView.computeVerticalScrollRange()
                    val extent = recyclerView.computeVerticalScrollExtent()
                    val offset = recyclerView.computeVerticalScrollOffset()
                    val threshHold = range - footerHeight
                    val currentScroll = extent + offset
                    val excess = currentScroll - threshHold
                    binding.footerText.isVisible = !isShowingJustFolders() && excess >= 0
                }
            })
        }
    }

    private fun initViews() {
        setFooter()

        //Set RecyclerView and its adapter.
        if (mainFileListViewModel.isGridModeSetAsPreferred()) {
            layoutManager = StaggeredGridLayoutManager(
                ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns(),
                StaggeredGridLayoutManager.VERTICAL
            )
            viewType = ViewType.VIEW_TYPE_GRID
        } else {
            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            viewType = ViewType.VIEW_TYPE_LIST
        }

        binding.recyclerViewMainFileList.layoutManager = layoutManager

        fileListAdapter = FileListAdapter(
            context = requireContext(),
            layoutManager = layoutManager,
            listener = object :
                FileListAdapter.FileListAdapterListener {
                override fun clickItem(ocFile: OCFile) {
                    if (ocFile.isFolder) {
                        mainFileListViewModel.listDirectory(ocFile)
                        // TODO Manage animation listDirectoryWithAnimationDown
                    } else { /// Click on a file
                        // TODO Click on a file
                    }
                }
            })
        binding.recyclerViewMainFileList.adapter = fileListAdapter

        // Set Swipe to refresh and its listener
        binding.swipeRefreshMainFileList.setOnRefreshListener { mainFileListViewModel.refreshDirectory() }

        //Set SortOptions and its listeners
        binding.optionsLayout.let {
            it.onSortOptionsListener = this
            if (isPickingAFolder()) {
                it.onCreateFolderListener = this
                it.selectAdditionalView(SortOptionsView.AdditionalView.CREATE_FOLDER)
            }
        }
    }

    private fun subscribeToViewModels() {
        // Observe the action of retrieving the list of files from DB.
        mainFileListViewModel.getFilesListStatusLiveData.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                updateFileListData(files = data ?: emptyList())
                files = data ?: emptyList()
                val sortedFiles = mainFileListViewModel.sortList(files)
                fileListAdapter.updateFileList(filesToAdd = sortedFiles)
                registerListAdapterDataObserver()
                binding.footerText.text = mainFileListViewModel.manageListOfFilesAndGenerateText(files)
                binding.swipeRefreshMainFileList.cancel()
            }
        })

        // Observe the action of retrieving the list of shared by link files from DB.
        mainFileListViewModel.getFilesSharedByLinkData.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                updateFileListData(files = data ?: emptyList())
            }
        })

        // Observe the action of retrieving the list of available offline files from DB.
        mainFileListViewModel.getFilesAvailableOfflineData.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                updateFileListData(files = data ?: emptyList())
            }
        })
    }

    fun listDirectory(directory: OCFile) {
        mainFileListViewModel.listDirectory(directory = directory)
    }

    private fun isShowingJustFolders(): Boolean {
        val args = arguments
        return args != null && args.getBoolean(ARG_JUST_FOLDERS, false)
    }

    private fun registerListAdapterDataObserver() {
        val emptyDataObserver = EmptyDataObserver(binding.recyclerViewMainFileList, binding.emptyDataParent.root)
        fileListAdapter.registerAdapterDataObserver(emptyDataObserver)
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
            layoutManager?.spanCount = 1
        } else {
            mainFileListViewModel.setGridModeAsPreferred()
            layoutManager?.spanCount = ColumnQuantity(requireContext(), R.layout.grid_item).calculateNoOfColumns()
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

    private fun updateFileListData(files: List<OCFile>) {
        fileListAdapter.updateFileList(filesToAdd = files)
        registerListAdapterDataObserver()
        binding.swipeRefreshMainFileList.cancel()
    }

    fun updateFileListOption(newFileListOption: FileListOption) {
        when (newFileListOption) {
            FileListOption.ALL_FILES -> mainFileListViewModel.listCurrentDirectory()
            FileListOption.AV_OFFLINE -> mainFileListViewModel.getAvailableOfflineFilesList()
            FileListOption.SHARED_BY_LINK -> mainFileListViewModel.getSharedByLinkFilesList()
        }

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
        val filesViewModel = get(FilesViewModel::class.java)

        filesViewModel.createFolder(parentFolder, newFolderName)
        filesViewModel.createFolder.observe(this, { uiResultEvent: Event<UIResult<Unit>> ->
            val uiResult = uiResultEvent.peekContent()
            if (!uiResult.isSuccess) {
                val throwable = uiResult.getThrowableOrNull()
                val errorMessage =
                    throwable!!.parseError(resources.getString(R.string.create_dir_fail_msg), resources, false)
                showMessageInSnackbar(
                    message = errorMessage.toString()
                )
            }
        })
    }

    companion object {
        val ARG_JUST_FOLDERS = "${MainFileListFragment::class.java.canonicalName}.JUST_FOLDERS"
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"
        val ARG_LIST_FILE_OPTION = "${MainFileListFragment::class.java.canonicalName}.LIST_FILE_OPTION}"

        fun newInstance(
            justFolders: Boolean = false,
            pickingAFolder: Boolean = false
        ): MainFileListFragment {
            val args = Bundle()
            args.putBoolean(ARG_JUST_FOLDERS, justFolders)
            args.putBoolean(ARG_PICKING_A_FOLDER, pickingAFolder)
            return MainFileListFragment().apply { arguments = args }
        }
    }

    override fun onCreateFolderListener() {
        //TODO("Not yet implemented")
    }
}

