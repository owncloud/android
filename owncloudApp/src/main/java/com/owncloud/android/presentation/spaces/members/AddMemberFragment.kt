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

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.AddMemberFragmentBinding
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.members.model.OCMemberType
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AddMemberFragment: Fragment(), SearchMembersAdapter.SearchMembersAdapterListener {
    private var _binding: AddMemberFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by viewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var searchMembersAdapter: SearchMembersAdapter
    private lateinit var rolesAdapter: SpaceRolesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var roles: List<OCRole>

    private var editMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AddMemberFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchMembersAdapter = SearchMembersAdapter(this)
        recyclerView = binding.membersRecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMembersAdapter
        }

        val spaceMembers = requireArguments().getParcelableArrayList<SpaceMember>(ARG_SPACE_MEMBERS) ?: arrayListOf()
        editMode = requireArguments().getBoolean(ARG_EDIT_MODE, false)
        roles = requireArguments().getParcelableArrayList<OCRole>(ARG_ROLES) ?: arrayListOf()

        if (editMode) {
            val selectedMember = requireArguments().getParcelable<SpaceMember>(ARG_SELECTED_MEMBER)
            selectedMember?.let {
                bindEditMode(it, roles)
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.members) { uiState ->
            if (uiState.isLoading) {
                binding.indeterminateProgressBar.visibility = View.VISIBLE
                binding.emptyDataParent.root.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.GONE
            } else {
                binding.indeterminateProgressBar.visibility = View.GONE
                val listOfMembersFiltered = uiState.members.filter { member ->
                    !spaceMembers.any { spaceMember ->
                        spaceMember.id == "u:${member.id}" || spaceMember.id == "g:${member.id}" }
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

        collectLatestLifecycleFlow(spaceMembersViewModel.addMemberUIState) { uiState ->
            uiState?.let {
                binding.apply {
                    searchMemberLayout.visibility = View.GONE
                    addMemberLayout.visibility = View.VISIBLE
                    inviteMemberButton.visibility = View.VISIBLE
                }
                it.selectedMember?.let { member ->
                    bindSelectedMember(member)
                }
                it.selectedExpirationDate?.let { expirationDate ->
                    binding.expirationDateLayout.expirationDateValue.apply {
                        visibility = View.VISIBLE
                        text = DisplayUtils.displayDateToHumanReadable(expirationDate)
                    }
                }
                bindRoles(uiState.selectedRole?.id)
                bindDatePickerDialog()

                binding.expirationDateLayout.apply {
                    expirationDateLayout.setOnClickListener {
                        if (uiState.selectedExpirationDate != null) {
                            openDatePickerDialog()
                        } else {
                            expirationDateSwitch.isChecked = true
                        }
                    }
                }
                binding.inviteMemberButton.setOnClickListener {
                    uiState.selectedMember?.let { selectedMember ->
                        uiState.selectedRole?.let { selectedRole ->
                            spaceMembersViewModel.addMember(selectedMember, selectedRole.id, uiState.selectedExpirationDate)
                        }
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.addMemberResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> requireActivity().onBackPressed()
                    is UIResult.Error -> showErrorInSnackbar(R.string.members_add_failed, uiResult.error)
                }
            }
        }

        binding.searchBar.apply {
            if (savedInstanceState == null && !editMode) {
                requestFocus()
            }
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean = true

                override fun onQueryTextChange(newText: String): Boolean {
                    if (newText.length > 2) { spaceMembersViewModel.searchMembers(newText) } else { spaceMembersViewModel.clearSearch() }
                    return true
                }
            })
        }
    }

    private fun showOrHideEmptyView(hasMembers: Boolean) {
        binding.membersRecyclerView.isVisible = hasMembers
        binding.emptyDataParent.apply {
            val shouldShow = !hasMembers && binding.searchBar.query.length > 2
            root.isVisible = shouldShow
            if (shouldShow) {
                listEmptyDatasetIcon.setImageResource(R.drawable.ic_share_generic_white)
                listEmptyDatasetTitle.setText(R.string.members_search_failed)
                listEmptyDatasetSubTitle.setText(R.string.members_search_empty)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(if (editMode) R.string.members_edit else R.string.members_add)
    }

    override fun onMemberClick(member: OCMember) {
        spaceMembersViewModel.onMemberSelected(member)
    }

    private fun bindSelectedMember(member: OCMember) {
        binding.selectedMemberLayout.apply {
            memberIcon.setImageResource(if (member.type == OCMemberType.GROUP) R.drawable.ic_group else R.drawable.ic_user)
            memberName.text = member.displayName
            memberRole.text = member.surname
        }
    }

    private fun bindRoles(selectedRoleId: String?) {
        rolesAdapter = SpaceRolesAdapter(onRoleSelected = {
            binding.inviteMemberButton.isEnabled = true
            spaceMembersViewModel.onRoleSelected(it)
        })
        binding.rolesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rolesAdapter
        }
        rolesAdapter.setRoles(roles)
        selectedRoleId?.let {
            binding.inviteMemberButton.isEnabled = true
            rolesAdapter.setSelectedRole(it)
        }
    }

    private fun bindDatePickerDialog() {
        binding.expirationDateLayout.expirationDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openDatePickerDialog()
            } else {
                binding.expirationDateLayout.expirationDateValue.visibility = View.GONE
                spaceMembersViewModel.onExpirationDateSelected(null)
            }
        }
    }

    private fun openDatePickerDialog() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val formatter = SimpleDateFormat(DisplayUtils.DATE_FORMAT_ISO, Locale.ROOT)
                formatter.timeZone = TimeZone.getTimeZone("UTC")
                val isoExpirationDate = formatter.format(calendar.time)
                spaceMembersViewModel.onExpirationDateSelected(isoExpirationDate)
                binding.expirationDateLayout.expirationDateValue.apply {
                    visibility = View.VISIBLE
                    text = DisplayUtils.displayDateToHumanReadable(isoExpirationDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = calendar.timeInMillis
            show()
            setOnCancelListener {
                binding.expirationDateLayout.apply {
                    expirationDateSwitch.isChecked = false
                    expirationDateValue.visibility = View.GONE
                }
            }
        }
    }

    private fun bindEditMode(member: SpaceMember, roles: List<OCRole>) {
        spaceMembersViewModel.onMemberSelected(member)

        val selectedRole = roles.first { it.id == member.roles[0] }
        spaceMembersViewModel.onRoleSelected(selectedRole)

        member.expirationDateTime?.let { expirationDate ->
            spaceMembersViewModel.onExpirationDateSelected(expirationDate)
            binding.expirationDateLayout.expirationDateSwitch.isChecked = true
        }
        binding.inviteMemberButton.text = getString(R.string.share_confirm_public_link_button)
    }

    companion object {
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"
        private const val ARG_SPACE_MEMBERS = "SPACE_MEMBERS"
        private const val ARG_ROLES = "ROLES"
        private const val ARG_EDIT_MODE = "EDIT_MODE"
        private const val ARG_SELECTED_MEMBER = "SELECTED_MEMBER"

        fun newInstance(
            accountName: String,
            currentSpace: OCSpace,
            spaceMembers: List<SpaceMember>,
            roles: List<OCRole>,
            editMode: Boolean,
            selectedMember: SpaceMember?
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
