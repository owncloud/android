/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.AddMemberFragmentBinding
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.spaces.members.SearchMembersAdapter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AddGraphShareFragment : Fragment(), SearchMembersAdapter.SearchMembersAdapterListener {
    private var _binding: AddMemberFragmentBinding? = null
    private val binding get() = _binding!!

    private val graphShareViewModel by activityViewModel<GraphShareViewModel> {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCFile>(ARG_FILE)
        )
    }

    private lateinit var searchMembersAdapter: SearchMembersAdapter
    private lateinit var recyclerView: RecyclerView

    private var searchMinLength = DEFAULT_SEARCH_MIN_LENGTH
    private var currentUserId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AddMemberFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchMembersAdapter = SearchMembersAdapter(this)
        recyclerView = binding.membersRecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMembersAdapter
        }

        subscribeToViewModels()

        binding.searchBar.apply {
            if (savedInstanceState == null) { requestFocus() }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean = true

                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.length >= searchMinLength) {
                        graphShareViewModel.searchMembers(newText)
                    } else {
                        graphShareViewModel.clearSearch()
                    }
                    return true
                }
            })
        }
    }

    override fun onMemberClick(member: OCMember) {

    }

    private fun showOrHideEmptyView(hasMembers: Boolean) {
        binding.membersRecyclerView.isVisible = hasMembers
        binding.emptyDataParent.apply {
            val shouldShow = !hasMembers && binding.searchBar.query.length >= searchMinLength
            root.isVisible = shouldShow
            if (shouldShow) {
                listEmptyDatasetIcon.setImageResource(R.drawable.ic_share_generic_white)
                listEmptyDatasetTitle.setText(R.string.members_search_failed)
                listEmptyDatasetSubTitle.setText(R.string.members_search_empty)
            }
        }
    }

    private fun subscribeToViewModels() {
        val currentShares = (graphShareViewModel.shares.value?.peekContent() as? UIResult.Success)?.data?.members ?: emptyList()
        searchMinLength = graphShareViewModel.capabilities?.filesSharingSearchMinLength ?: DEFAULT_SEARCH_MIN_LENGTH

        collectLatestLifecycleFlow(graphShareViewModel.userId) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let { currentUserId = it }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve user id")
                    }
                }
            }
        }

        collectLatestLifecycleFlow(graphShareViewModel.members) { uiState ->
            if (uiState.isLoading) {
                binding.indeterminateProgressBar.visibility = View.VISIBLE
                binding.emptyDataParent.root.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.GONE
            } else {
                binding.indeterminateProgressBar.visibility = View.GONE
                val sharedMemberIds = currentShares.mapTo(HashSet()) { it.memberId }
                val listOfMembersFiltered = uiState.members.filterNot { member ->
                    member.id == currentUserId || member.id in sharedMemberIds
                }
                val hasMembers = listOfMembersFiltered.isNotEmpty()
                showOrHideEmptyView(hasMembers)
                if (hasMembers) searchMembersAdapter.setMembers(listOfMembersFiltered)
                uiState.error?.let {
                    Timber.e(uiState.error, "Failed to retrieve available users and groups")
                    showErrorInSnackbar(R.string.members_search_failed, uiState.error)
                }
            }
        }
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val DEFAULT_SEARCH_MIN_LENGTH = 3

        fun newInstance(file: OCFile, accountName: String): AddGraphShareFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putString(ARG_ACCOUNT_NAME, accountName)
            }
            return AddGraphShareFragment().apply {
                arguments = args
            }
        }
    }
}
