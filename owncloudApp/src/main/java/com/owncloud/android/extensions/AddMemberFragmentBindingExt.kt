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

package com.owncloud.android.extensions

import androidx.core.view.isVisible
import com.owncloud.android.R
import com.owncloud.android.databinding.AddMemberFragmentBinding
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.members.model.OCMemberType
import com.owncloud.android.presentation.spaces.members.SpaceRolesAdapter

fun AddMemberFragmentBinding.showOrHideEmptyView(hasMembers: Boolean, searchMinLength: Int) {
    membersRecyclerView.isVisible = hasMembers
    emptyDataParent.apply {
        val shouldShow = !hasMembers && searchBar.query.length >= searchMinLength
        root.isVisible = shouldShow
        if (shouldShow) {
            listEmptyDatasetIcon.setImageResource(R.drawable.ic_share_generic_white)
            listEmptyDatasetTitle.setText(R.string.members_search_failed)
            listEmptyDatasetSubTitle.setText(R.string.members_search_empty)
        }
    }
}

fun AddMemberFragmentBinding.bindSelectedMember(member: OCMember) {
    selectedMemberLayout.apply {
        memberIcon.setImageResource(if (member.type == OCMemberType.GROUP) R.drawable.ic_group else R.drawable.ic_user)
        memberName.text = member.displayName
        memberRole.text = member.surname
    }
}

fun AddMemberFragmentBinding.bindRoles(rolesAdapter: SpaceRolesAdapter, selectedRoleId: String?) {
    selectedRoleId?.let {
        inviteMemberButton.isEnabled = true
        rolesAdapter.setSelectedRole(it)
    }
}
