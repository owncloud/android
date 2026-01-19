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

package com.owncloud.android.presentation.spaces.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.AddMemberFragmentBinding
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.common.UIResult
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AddMemberFragment: Fragment() {
    private var _binding: AddMemberFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by viewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var searchMembersAdapter: SearchMembersAdapter
    private lateinit var recyclerView: RecyclerView

    private var listOfMembers = emptyList<OCMember>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AddMemberFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchMembersAdapter = SearchMembersAdapter()
        recyclerView = binding.membersRecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMembersAdapter
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.members) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            listOfMembers = it
                            spaceMembersViewModel.getSpaceMembers()
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve available users and groups")
                        showErrorInSnackbar(R.string.search_members_failed, uiResult.error)
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.spaceMembers) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            val listOfMembersFiltered = listOfMembers.filter { member -> !it.members.any {
                                spaceMember -> spaceMember.id == "u:${member.id}" || spaceMember.id == "g:${member.id}" }
                            }
                            searchMembersAdapter.setMembers(listOfMembersFiltered)
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        val currentSpace = requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
                        Timber.e(uiResult.error, "Failed to retrieve space members for space: ${currentSpace?.name} (${currentSpace?.id})")
                    }
                }
            }
        }


        binding.searchBar.apply {
            requestFocus()
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean = true

                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.length > 2) { spaceMembersViewModel.searchMembers(newText) } else { spaceMembersViewModel.clearSearch() }
                    return true
                }
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(R.string.add_member)
    }

    companion object {
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"

        fun newInstance(
            accountName: String,
            currentSpace: OCSpace
        ): AddMemberFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putParcelable(ARG_CURRENT_SPACE, currentSpace)
            }
            return AddMemberFragment().apply {
                arguments = args
            }
        }
    }
}
