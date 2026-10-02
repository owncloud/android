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
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.sharing.shares.model.MemberPermission
import com.owncloud.android.extensions.bindDatePickerDialog
import com.owncloud.android.extensions.bindRoles
import com.owncloud.android.extensions.bindSelectedMember
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.openDatePickerDialog
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showOrHideEmptyView
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.members.SearchMembersAdapter
import com.owncloud.android.presentation.roles.RolesAdapter
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class AddMemberFragment: Fragment(), SearchMembersAdapter.SearchMembersAdapterListener {
    private var _binding: AddMemberFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by activityViewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var searchMembersAdapter: SearchMembersAdapter
    private lateinit var rolesAdapter: RolesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var roles: List<OCRole>

    private var editMode = false
    private var selectedMemberId = ""
    private var searchMinLength = DEFAULT_SEARCH_MIN_LENGTH

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

        editMode = requireArguments().getBoolean(ARG_EDIT_MODE, false)
        roles = requireArguments().getParcelableArrayList<OCRole>(ARG_ROLES) ?: arrayListOf()

        searchMembersAdapter = SearchMembersAdapter(this)
        recyclerView = binding.membersRecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMembersAdapter
        }

        rolesAdapter = RolesAdapter(onRoleSelected = {
            binding.confirmActionButton.isEnabled = true
            spaceMembersViewModel.onRoleSelected(it)
        })
        binding.rolesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rolesAdapter
        }
        rolesAdapter.setRoles(roles)

        if (editMode) {
            val selectedMember = requireArguments().getParcelable<MemberPermission>(ARG_SELECTED_MEMBER)
            selectedMember?.let {
                bindEditMode(it, roles)
            }
        }

        subscribeToViewModels()

        binding.searchBar.apply {
            if (savedInstanceState == null && !editMode) {
                requestFocus()
            }
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean = true

                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.length >= searchMinLength) {
                        spaceMembersViewModel.searchMembers(newText)
                    } else {
                        spaceMembersViewModel.clearSearch()
                    }
                    return true
                }
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(if (editMode) R.string.members_edit else R.string.members_add)
    }

    override fun onMemberClick(member: OCMember) {
        spaceMembersViewModel.onMemberSelected(member)
    }

    private fun subscribeToViewModels() {
        val spaceMembers = requireArguments().getParcelableArrayList<MemberPermission>(ARG_SPACE_MEMBERS) ?: arrayListOf()
        searchMinLength = spaceMembersViewModel.capabilities?.filesSharingSearchMinLength ?: DEFAULT_SEARCH_MIN_LENGTH

        collectLatestLifecycleFlow(spaceMembersViewModel.members) { uiState ->
            if (uiState.isLoading) {
                binding.indeterminateProgressBar.visibility = View.VISIBLE
                binding.emptyDataParent.root.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.GONE
            } else {
                binding.indeterminateProgressBar.visibility = View.GONE
                val spaceMemberIds = spaceMembers.mapTo(HashSet()) { it.id }
                val listOfMembersFiltered = uiState.members.filterNot { member ->
                    "u:${member.id}" in spaceMemberIds || "g:${member.id}" in spaceMemberIds
                }
                val hasMembers = listOfMembersFiltered.isNotEmpty()
                binding.showOrHideEmptyView(hasMembers, searchMinLength)
                if (hasMembers) searchMembersAdapter.setMembers(listOfMembersFiltered)
                uiState.error?.let {
                    Timber.e(uiState.error, "Failed to retrieve available users and groups")
                    showErrorInSnackbar(R.string.members_search_failed, uiState.error)
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.addMemberUIState) { uiState ->
            uiState?.let {
                binding.apply {
                    searchMemberLayout.visibility = View.GONE
                    addMemberLayout.visibility = View.VISIBLE
                    confirmActionButton.visibility = View.VISIBLE
                }
                it.selectedMember?.let { member ->
                    binding.bindSelectedMember(member)
                }
                it.selectedExpirationDate?.let { expirationDate ->
                    binding.expirationDateLayout.expirationDateValue.apply {
                        visibility = View.VISIBLE
                        text = DisplayUtils.displayDateToHumanReadable(expirationDate)
                    }
                }
                binding.bindRoles(rolesAdapter, uiState.selectedRole?.id)
                bindDatePickerDialog(binding, uiState.selectedExpirationDate, spaceMembersViewModel::onExpirationDateSelected)

                binding.expirationDateLayout.apply {
                    expirationDateLayout.setOnClickListener {
                        if (uiState.selectedExpirationDate != null) {
                            openDatePickerDialog(binding, uiState.selectedExpirationDate, spaceMembersViewModel::onExpirationDateSelected)
                        } else {
                            expirationDateSwitch.isChecked = true
                        }
                    }
                }
                binding.confirmActionButton.setOnClickListener {
                    uiState.selectedMember?.let { selectedMember ->
                        uiState.selectedRole?.let { selectedRole ->
                            if (editMode) {
                                spaceMembersViewModel.editMember(selectedMemberId, selectedRole.id, uiState.selectedExpirationDate)
                            } else {
                                spaceMembersViewModel.addMember(selectedMember, selectedRole.id, uiState.selectedExpirationDate)
                            }
                        }
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.addMemberResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> parentFragmentManager.popBackStack()
                    is UIResult.Error -> showErrorInSnackbar(R.string.members_add_failed, uiResult.error)
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.editMemberResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> parentFragmentManager.popBackStack()
                    is UIResult.Error -> showErrorInSnackbar(R.string.members_edit_failed, uiResult.error)
                }
            }
        }
    }

    private fun bindEditMode(member: MemberPermission, roles: List<OCRole>) {
        selectedMemberId = member.id
        spaceMembersViewModel.onMemberSelected(member)

        val selectedRole = roles.first { it.id == member.roles[0] }
        spaceMembersViewModel.onRoleSelected(selectedRole)

        member.expirationDateTime?.let { expirationDate ->
            spaceMembersViewModel.onExpirationDateSelected(expirationDate)
            binding.expirationDateLayout.expirationDateSwitch.isChecked = true
        }
        binding.confirmActionButton.text = getString(R.string.share_confirm_public_link_button)
    }

    companion object {
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"
        private const val ARG_SPACE_MEMBERS = "SPACE_MEMBERS"
        private const val ARG_ROLES = "ROLES"
        private const val ARG_EDIT_MODE = "EDIT_MODE"
        private const val ARG_SELECTED_MEMBER = "SELECTED_MEMBER"
        private const val DEFAULT_SEARCH_MIN_LENGTH = 3

        fun newInstance(
            accountName: String,
            currentSpace: OCSpace,
            spaceMembers: List<MemberPermission>,
            roles: List<OCRole>,
            editMode: Boolean,
            selectedMember: MemberPermission?
        ): AddMemberFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putParcelable(ARG_CURRENT_SPACE, currentSpace)
                putParcelableArrayList(ARG_SPACE_MEMBERS, ArrayList(spaceMembers))
                putParcelableArrayList(ARG_ROLES, ArrayList(roles))
                putBoolean(ARG_EDIT_MODE, editMode)
                putParcelable(ARG_SELECTED_MEMBER, selectedMember)
            }
            return AddMemberFragment().apply {
                arguments = args
            }
        }
    }
}
