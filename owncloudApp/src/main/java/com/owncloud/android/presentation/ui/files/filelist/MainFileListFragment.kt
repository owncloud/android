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
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.adapters.filelist.FileListAdapter
import com.owncloud.android.presentation.onSuccess
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFileListFragment : Fragment() {

    private val mainFileListViewModel by viewModel<MainFileListViewModel>()

    private var _binding: MainFileListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var fileListAdapter: FileListAdapter

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
        fileListAdapter = FileListAdapter(context = requireContext())
        binding.recyclerViewMainFileList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileListAdapter

        }
    }

    private fun subscribeToViewModels() {
        //Observe the action of retrieving the list of files from BBDD.
        mainFileListViewModel.getFilesListStatusLiveData.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                val files = data ?: emptyList()
                fileListAdapter.updateFileList(filesToAdd = files)
                mainFileListViewModel.manageListOfFiles(files)
            }
        })

        mainFileListViewModel.numberOfFilesPerType.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                if (!isShowingJustFolders()) {
                    mainFileListViewModel.generateFooterText(data!!.first, data.second)
                }
            }
        })

        mainFileListViewModel.footerText.observe(viewLifecycleOwner, Event.EventObserver {
            it.onSuccess { data ->
                setFooterText(data)
            }
        })
    }

    fun listDirectory(directory: OCFile) {
        mainFileListViewModel.listDirectory(directory = directory)
    }

    private fun setFooterText(text: String?) {
        if (text?.isNotEmpty() == true) {
            binding.footerMainFileList.footerText.text = text
            // TODO Manage footer enable/disable options
            //setFooterEnabled(true)
        } else {
            //setFooterEnabled(false)
        }
    }

    private fun isShowingJustFolders(): Boolean {
        val args = arguments
        return args != null && args.getBoolean(ARG_JUST_FOLDERS, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        val ARG_JUST_FOLDERS = MainFileListFragment::class.java.canonicalName + ".JUST_FOLDERS"

        fun newInstance(): MainFileListFragment {
            val args = Bundle()
            return MainFileListFragment().apply { arguments = args }
        }
    }
}

