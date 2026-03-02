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

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.MembersFragmentBinding
import com.owncloud.android.domain.links.model.OCLink
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.roles.model.OCRoleType
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.extensions.avoidScreenshotsIfNeeded
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.spaces.links.SpaceLinksAdapter
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SpaceMembersFragment : Fragment(), SpaceMembersAdapter.SpaceMembersAdapterListener {
    private var _binding: MembersFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by activityViewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var spaceMembersAdapter: SpaceMembersAdapter
    private lateinit var spaceLinksAdapter: SpaceLinksAdapter
    private lateinit var currentSpace: OCSpace

    private var roles: List<OCRole> = emptyList()
    private var addMemberRoles: List<OCRole> = emptyList()
    private var spaceMembers: List<SpaceMember> = emptyList()
    private var listener: SpaceMemberFragmentListener? = null
    private var canRemoveMembers = false
    private var canEditMembers = false
    private var numberOfManagers = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val accountId = requireArguments().getString(ARG_ACCOUNT_ID)
        spaceMembersAdapter = SpaceMembersAdapter(this, accountId)
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = spaceMembersAdapter
        }

        spaceLinksAdapter = SpaceLinksAdapter()
        binding.publicLinksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = spaceLinksAdapter
        }

        currentSpace = requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE) ?: return
        savedInstanceState?.let {
            canRemoveMembers = it.getBoolean(CAN_REMOVE_MEMBERS, false)
            canEditMembers = it.getBoolean(CAN_EDIT_MEMBERS, false)
        }

        subscribeToViewModels()

        binding.addMemberButton.setOnClickListener {
            spaceMembersViewModel.resetViewModel()
            listener?.addMember(
                space = currentSpace,
                spaceMembers = spaceMembers,
                roles = addMemberRoles,
                editMode = false,
                selectedMember = null
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(R.string.space_members_label)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SpaceMemberFragmentListener?
        } catch (e: ClassCastException) {
            Timber.e(e, "The activity attached does not implement SpaceMemberFragmentListener")
            throw ClassCastException(activity.toString() + " must implement SpaceMemberFragmentListener")
        }
    }

    override fun onResume() {
        super.onResume()
        spaceMembersViewModel.getSpacePermissions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CAN_REMOVE_MEMBERS, canRemoveMembers)
        outState.putBoolean(CAN_EDIT_MEMBERS, canEditMembers)
    }

    override fun onRemoveMember(spaceMember: SpaceMember) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.members_remove_dialog_message, spaceMember.displayName))
            .setPositiveButton(getString(R.string.common_yes)) { _, _ -> spaceMembersViewModel.removeMember(spaceMember.id) }
            .setNegativeButton(getString(R.string.common_no)) { dialog, _ -> dialog.dismiss() }
            .show()
            .avoidScreenshotsIfNeeded()
    }

    override fun onEditMember(spaceMember: SpaceMember) {
        spaceMembersViewModel.resetViewModel()
        val currentSpace = requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE) ?: return
        listener?.addMember(
            space = currentSpace,
            spaceMembers = spaceMembers,
            roles = addMemberRoles,
            editMode = true,
            selectedMember = spaceMember
        )
    }

    private fun subscribeToViewModels() {
        collectLatestLifecycleFlow(spaceMembersViewModel.roles) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            roles = it
                            spaceMembersViewModel.getSpaceMembers()
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve platform roles")
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.spaceMembers) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let {
                            if (roles.isNotEmpty()) {
                                numberOfManagers = it.members.count { spaceMember ->
                                    spaceMember.roles.contains(OCRoleType.toString(OCRoleType.CAN_MANAGE)) }
                                spaceMembers = it.members
                                addMemberRoles = it.roles
                                spaceMembersAdapter.setSpaceMembers(spaceMembers, roles, canRemoveMembers, canEditMembers, numberOfManagers)
                                val hasLinks = it.links.isNotEmpty()
                                showOrHideEmptyView(hasLinks)
                                if (hasLinks) { showSpaceLinks(it.links) }
                            }
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        requireActivity().finish()
                        Timber.e(uiResult.error, "Failed to retrieve space members for space: ${currentSpace.id} (${currentSpace.id})")
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.spacePermissions) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let { spacePermissions ->
                            binding.addMemberButton.isVisible = DRIVES_CREATE_PERMISSION in spacePermissions
                            canRemoveMembers = DRIVES_DELETE_PERMISSION in spacePermissions
                            canEditMembers = DRIVES_UPDATE_PERMISSION in spacePermissions
                            spaceMembersAdapter.setSpaceMembers(spaceMembers, roles, canRemoveMembers, canEditMembers, numberOfManagers)
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve space permissions for space: ${currentSpace.id} (${currentSpace?.id})")
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.addMemberResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> {
                        showMessageInSnackbar(getString(R.string.members_add_correctly))
                        spaceMembersViewModel.resetViewModel()
                    }
                    is UIResult.Error -> { }
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.removeMemberResultFlow) { uiResult ->
            when (uiResult) {
                is UIResult.Loading -> { }
                is UIResult.Success -> {
                    showMessageInSnackbar(getString(R.string.members_remove_correctly))
                    spaceMembersViewModel.getSpaceMembers()
                }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "Failed to remove a member from space: ${currentSpace.id}")
                    showErrorInSnackbar(R.string.members_remove_failed, uiResult.error)
                }
            }
        }

        collectLatestLifecycleFlow(spaceMembersViewModel.editMemberResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> {
                        showMessageInSnackbar(getString(R.string.members_edit_correctly))
                        spaceMembersViewModel.resetViewModel()
                    }
                    is UIResult.Error -> { }
                }
            }
        }
    }

    private fun showOrHideEmptyView(hasLinks: Boolean) {
        binding.apply {
            publicLinksRecyclerView.isVisible = hasLinks
            noPublicLinksMessage.isVisible = !hasLinks
        }
    }

    private fun showSpaceLinks(spaceLinks: List<OCLink>) {
        val formatter = SimpleDateFormat(DisplayUtils.DATE_FORMAT_ISO, Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        spaceLinksAdapter.setSpaceLinks(spaceLinks.sortedByDescending { spaceLink ->
            formatter.parse(spaceLink.createdDateTime)
        })
    }

    interface SpaceMemberFragmentListener {
        fun addMember(space: OCSpace, spaceMembers: List<SpaceMember>, roles: List<OCRole>, editMode: Boolean, selectedMember: SpaceMember?)
    }

    companion object {
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val ARG_ACCOUNT_ID = "ACCOUNT_ID"
        private const val DRIVES_CREATE_PERMISSION = "libre.graph/driveItem/permissions/create"
        private const val DRIVES_DELETE_PERMISSION = "libre.graph/driveItem/permissions/delete"
        private const val DRIVES_UPDATE_PERMISSION = "libre.graph/driveItem/permissions/update"
        private const val CAN_REMOVE_MEMBERS = "CAN_REMOVE_MEMBERS"
        private const val CAN_EDIT_MEMBERS = "CAN_EDIT_MEMBERS"

        fun newInstance(
            accountName: String,
            accountId: String,
            currentSpace: OCSpace,
        ): SpaceMembersFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putString(ARG_ACCOUNT_ID, accountId)
                putParcelable(ARG_CURRENT_SPACE, currentSpace)
            }
            return SpaceMembersFragment().apply {
                arguments = args
            }
        }
    }
}
