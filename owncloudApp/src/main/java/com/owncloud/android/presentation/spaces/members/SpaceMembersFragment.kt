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
import android.content.DialogInterface
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
import com.owncloud.android.extensions.showAlertDialog
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.spaces.links.SpaceLinksAdapter
import com.owncloud.android.presentation.spaces.links.SpaceLinksViewModel
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SpaceMembersFragment : Fragment(), SpaceMembersAdapter.SpaceMembersAdapterListener, SpaceLinksAdapter.SpaceLinksAdapterListener {
    private var _binding: MembersFragmentBinding? = null
    private val binding get() = _binding!!

    private val spaceMembersViewModel: SpaceMembersViewModel by activityViewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE)
        )
    }
    private val spaceLinksViewModel: SpaceLinksViewModel by activityViewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME),
            requireArguments().getParcelable(ARG_CURRENT_SPACE)
        )
    }

    private lateinit var spaceMembersAdapter: SpaceMembersAdapter
    private lateinit var spaceLinksAdapter: SpaceLinksAdapter
    private lateinit var currentSpace: OCSpace

    private var roles: List<OCRole> = emptyList()
    private var addMemberRoles: List<OCRole> = emptyList()
    private var spaceMembers: List<SpaceMember> = emptyList()
    private var listener: SpaceMemberFragmentListener? = null
    private var canRemoveMembersAndLinks = false
    private var canEditMembersAndLinks = false
    private var canReadMembers = false
    private var numberOfManagers = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MembersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val accountId = requireArguments().getString(ARG_ACCOUNT_ID)
        spaceMembersAdapter = SpaceMembersAdapter(this, accountId)
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = spaceMembersAdapter
        }

        spaceLinksAdapter = SpaceLinksAdapter(this)
        binding.publicLinksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = spaceLinksAdapter
        }

        currentSpace = requireArguments().getParcelable<OCSpace>(ARG_CURRENT_SPACE) ?: return
        savedInstanceState?.let {
            canRemoveMembersAndLinks = it.getBoolean(CAN_REMOVE_MEMBERS, false)
            canEditMembersAndLinks = it.getBoolean(CAN_EDIT_MEMBERS, false)
            canReadMembers = it.getBoolean(CAN_READ_MEMBERS, false)
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

        binding.addPublicLinkButton.setOnClickListener {
            spaceLinksViewModel.resetViewModel()
            listener?.addPublicLink(
                space = currentSpace,
                editMode = false,
                selectedPublicLink = null
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
        outState.putBoolean(CAN_REMOVE_MEMBERS, canRemoveMembersAndLinks)
        outState.putBoolean(CAN_EDIT_MEMBERS, canEditMembersAndLinks)
        outState.putBoolean(CAN_READ_MEMBERS, canReadMembers)
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

    override fun onCopyOrSendPublicLink(publicLinkUrl: String) {
        listener?.copyOrSendPublicLink(publicLinkUrl, currentSpace.name)
    }

    override fun onRemovePublicLink(publicLinkId: String, publicLinkDisplayName: String) {
        showAlertDialog(
            title = getString(R.string.public_link_remove_dialog_title, publicLinkDisplayName),
            message = getString(R.string.public_link_remove_dialog_message),
            positiveButtonText = getString(R.string.common_yes),
            positiveButtonListener = { _: DialogInterface?, _: Int -> spaceLinksViewModel.removePublicLink(publicLinkId) },
            negativeButtonText = getString(R.string.common_no)
        )
    }

    override fun onEditPublicLink(publicLink: OCLink) {
        spaceLinksViewModel.resetViewModel()
        listener?.addPublicLink(
            space = currentSpace,
            editMode = true,
            selectedPublicLink = publicLink
        )
    }

    private fun subscribeToViewModels() {
        observeRoles()
        observeSpaceMembers()
        observeSpacePermissions()
        observeAddMemberResult()
        observeRemoveMemberResult()
        observeEditMemberResult()
        observeAddLinkResult()
        observeRemoveLinkResult()
        observeEditLinkResult()
        observeEditPasswordLinkResult()
    }

    private fun observeRoles() {
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
    }

    private fun observeSpaceMembers() {
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
                                if (canReadMembers) {
                                    showSpaceMembers()
                                    val hasLinks = it.links.isNotEmpty()
                                    showOrHideEmptyView(hasLinks)
                                    if (hasLinks) { showSpaceLinks(it.links) }
                                }
                                binding.indeterminateProgressBar.isVisible = false
                            }
                        }
                    }
                    is UIResult.Loading -> { binding.indeterminateProgressBar.isVisible = true }
                    is UIResult.Error -> {
                        requireActivity().finish()
                        Timber.e(uiResult.error, "Failed to retrieve space members for space: ${currentSpace.id} (${currentSpace.id})")
                    }
                }
            }
        }
    }

    private fun observeSpacePermissions() {
        collectLatestLifecycleFlow(spaceMembersViewModel.spacePermissions) { event ->
            event?.let {
                when (val uiResult = event.peekContent()) {
                    is UIResult.Success -> {
                        uiResult.data?.let { spacePermissions ->
                            checkPermissions(spacePermissions)
                            if (canReadMembers) {
                                showSpaceMembers()
                            }
                        }
                    }
                    is UIResult.Loading -> { }
                    is UIResult.Error -> {
                        Timber.e(uiResult.error, "Failed to retrieve space permissions for space: ${currentSpace.id} (${currentSpace?.id})")
                    }
                }
            }
        }
    }

    private fun observeAddMemberResult() {
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
    }

    private fun observeRemoveMemberResult() {
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
    }

    private fun observeEditMemberResult() {
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

    private fun observeAddLinkResult() {
        collectLatestLifecycleFlow(spaceLinksViewModel.addLinkResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> {
                        showMessageInSnackbar(getString(R.string.public_link_add_correctly))
                        spaceLinksViewModel.resetViewModel()
                    }
                    is UIResult.Error -> { }
                }
            }
        }
    }

    private fun observeRemoveLinkResult() {
        collectLatestLifecycleFlow(spaceLinksViewModel.removeLinkResultFlow) { uiResult ->
            when (uiResult) {
                is UIResult.Loading -> { }
                is UIResult.Success -> {
                    showMessageInSnackbar(getString(R.string.public_link_remove_correctly))
                    spaceMembersViewModel.getSpaceMembers()
                }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "Failed to remove a public link from space: ${currentSpace.id}")
                    showErrorInSnackbar(R.string.public_link_remove_failed, uiResult.error)
                }
            }
        }
    }

    private fun observeEditLinkResult() {
        collectLatestLifecycleFlow(spaceLinksViewModel.editLinkResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> {
                        if (spaceLinksViewModel.addPublicLinkUIState.value?.wasPasswordChanged == false) {
                            showMessageInSnackbar(getString(R.string.public_link_edit_correctly))
                        }
                        spaceLinksViewModel.resetViewModel()
                    }
                    is UIResult.Error -> { }
                }
            }
        }
    }

    private fun observeEditPasswordLinkResult() {
        collectLatestLifecycleFlow(spaceLinksViewModel.editPasswordLinkResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> showMessageInSnackbar(getString(R.string.public_link_edit_correctly))
                    is UIResult.Error -> { }
                }
            }
        }
    }

    private fun checkPermissions(spacePermissions: List<String>) {
        val hasCreatePermission = DRIVES_CREATE_PERMISSION in spacePermissions
        canRemoveMembersAndLinks = DRIVES_DELETE_PERMISSION in spacePermissions
        canEditMembersAndLinks = DRIVES_UPDATE_PERMISSION in spacePermissions
        canReadMembers = DRIVES_READ_PERMISSION in spacePermissions
        binding.apply {
            addMemberButton.isVisible = hasCreatePermission
            addPublicLinkButton.isVisible = hasCreatePermission
            membersListSection.isVisible = canReadMembers
            publicLinksSection.isVisible = canReadMembers
        }
    }

    private fun showOrHideEmptyView(hasLinks: Boolean) {
        binding.apply {
            publicLinksRecyclerView.isVisible = hasLinks
            noPublicLinksMessage.isVisible = !hasLinks
        }
    }

    private fun showSpaceMembers() {
        spaceMembersAdapter.setSpaceMembers(
            spaceMembers = spaceMembers,
            roles = roles,
            canRemoveMembers = canRemoveMembersAndLinks,
            canEditMembers = canEditMembersAndLinks,
            numberOfManagers = numberOfManagers
        )
    }

    private fun showSpaceLinks(spaceLinks: List<OCLink>) {
        val formatter = SimpleDateFormat(DisplayUtils.DATE_FORMAT_ISO, Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        spaceLinksAdapter.setSpaceLinks(
            spaceLinks = spaceLinks.sortedByDescending { spaceLink ->
                if (spaceLink.createdDateTime.isNotEmpty()) {
                    formatter.parse(spaceLink.createdDateTime)
                } else {
                    Date(0)
                }
            },
            canRemoveLinks = canRemoveMembersAndLinks,
            canEditLinks = canEditMembersAndLinks
        )
    }

    interface SpaceMemberFragmentListener {
        fun addMember(space: OCSpace, spaceMembers: List<SpaceMember>, roles: List<OCRole>, editMode: Boolean, selectedMember: SpaceMember?)
        fun addPublicLink(space: OCSpace, editMode: Boolean, selectedPublicLink: OCLink?)
        fun copyOrSendPublicLink(publicLinkUrl: String, spaceName: String)
    }

    companion object {
        private const val ARG_CURRENT_SPACE = "CURRENT_SPACE"
        private const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        private const val ARG_ACCOUNT_ID = "ACCOUNT_ID"
        private const val DRIVES_CREATE_PERMISSION = "libre.graph/driveItem/permissions/create"
        private const val DRIVES_DELETE_PERMISSION = "libre.graph/driveItem/permissions/delete"
        private const val DRIVES_UPDATE_PERMISSION = "libre.graph/driveItem/permissions/update"
        private const val DRIVES_READ_PERMISSION = "libre.graph/driveItem/permissions/read"
        private const val CAN_REMOVE_MEMBERS = "CAN_REMOVE_MEMBERS"
        private const val CAN_EDIT_MEMBERS = "CAN_EDIT_MEMBERS"
        private const val CAN_READ_MEMBERS = "CAN_READ_MEMBERS"

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
