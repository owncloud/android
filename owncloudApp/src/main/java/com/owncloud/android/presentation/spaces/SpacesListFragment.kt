/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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
 */

package com.owncloud.android.presentation.spaces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.SpacesListFragmentBinding
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.toDrawableRes
import com.owncloud.android.extensions.toSubtitleStringRes
import com.owncloud.android.extensions.toTitleStringRes
import org.koin.androidx.viewmodel.ext.android.viewModel

class SpacesListFragment : Fragment() {
    private var _binding: SpacesListFragmentBinding? = null
    private val binding get() = _binding!!

    private val spacesListViewModel: SpacesListViewModel by viewModel()

    private lateinit var spacesListAdapter: SpacesListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SpacesListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        subscribeToViewModels()
    }

    private fun initViews() {
        val spacesListLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerSpacesList.layoutManager = spacesListLayoutManager
        spacesListAdapter = SpacesListAdapter()
        binding.recyclerSpacesList.adapter = spacesListAdapter

        binding.swipeRefreshSpacesList.setOnRefreshListener {
            spacesListViewModel.refreshSpacesFromServer()
        }
    }

    private fun subscribeToViewModels() {
        collectLatestLifecycleFlow(spacesListViewModel.spacesList) { uiState ->
            showOrHideEmptyView(uiState.spaces)
            spacesListAdapter.setData(uiState.spaces)
            binding.swipeRefreshSpacesList.isRefreshing = uiState.refreshing
            uiState.error?.let { showErrorInSnackbar(R.string.spaces_sync_failed, it) }
        }
    }

    private fun showOrHideEmptyView(spacesList: List<OCSpace>) {
        binding.recyclerSpacesList.isVisible = spacesList.isNotEmpty()

        with(binding.emptyDataParent) {
            root.isVisible = spacesList.isEmpty()
            listEmptyDatasetIcon.setImageResource(FileListOption.SPACES_LIST.toDrawableRes())
            listEmptyDatasetTitle.setText(FileListOption.SPACES_LIST.toTitleStringRes())
            listEmptyDatasetSubTitle.setText(FileListOption.SPACES_LIST.toSubtitleStringRes())
        }
    }
}
