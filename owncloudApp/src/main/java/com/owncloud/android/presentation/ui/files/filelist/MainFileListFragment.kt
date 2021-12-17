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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.databinding.MainFileListFragmentBinding
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.cancel
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
import com.owncloud.android.ui.activity.FileListOption
import com.owncloud.android.utils.FileStorageUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFileListFragment : Fragment(), SortDialogListener, SortOptionsView.SortOptionsListener, SortOptionsView.CreateFolderListener {

    private val mainFileListViewModel by viewModel<MainFileListViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var files: List<OCFile>

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

    private fun initViews() {
        //Set RecyclerView and its adapter.
        fileListAdapter = FileListAdapter(context = requireContext(), isShowingJustFolders = isShowingJustFolders(), listener = object :
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
        binding.recyclerViewMainFileList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileListAdapter
        }

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
        //TODO("Not yet implemented")
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

        // TODO Manage FAB button
    }

    companion object {
        val ARG_JUST_FOLDERS = "${MainFileListFragment::class.java.canonicalName}.JUST_FOLDERS"
        val ARG_PICKING_A_FOLDER = "${MainFileListFragment::class.java.canonicalName}.ARG_PICKING_A_FOLDER}"

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

