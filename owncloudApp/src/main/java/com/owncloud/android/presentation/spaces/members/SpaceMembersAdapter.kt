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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MemberItemBinding
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.roles.model.OCRoleType
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils

class SpaceMembersAdapter(
    private val listener: SpaceMembersAdapterListener
): RecyclerView.Adapter<SpaceMembersAdapter.SpaceMembersViewHolder>() {

    private var members: List<SpaceMember> = emptyList()
    private var rolesMap: Map<String, String> = emptyMap()
    private var canRemoveMembers = false
    private var canEditMembers = false
    private var numberOfManagers = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceMembersViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.member_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return SpaceMembersViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpaceMembersViewHolder, position: Int) {
        val member = members[position]
        val roleNames = member.roles.mapNotNull { rolesMap[it] }

        holder.binding.apply {
            val isGroup = member.id.startsWith(GROUP_PREFIX)
            memberIcon.setImageResource(if (isGroup) R.drawable.ic_group else R.drawable.ic_user)
            memberName.text = member.displayName
            memberName.contentDescription = holder.itemView.context.getString(
                if (isGroup) R.string.content_description_member_group else R.string.content_description_member_user, member.displayName
            )
            memberRole.text = roleNames.joinToString(", ")

            val memberRole = OCRoleType.parseFromId(member.roles.first())
            val numberOfManagers = members.count { it.roles.contains(OCRoleType.toString(OCRoleType.CAN_MANAGE)) }
            val isTheLastManager = memberRole == OCRoleType.CAN_MANAGE && numberOfManagers == 1
            removeMemberButton.apply {
                contentDescription = holder.itemView.context.getString(R.string.content_description_remove_member_button, member.displayName)
                isVisible = canRemoveMembers && !isTheLastManager
                setOnClickListener {
                    listener.onRemoveMember(member)
                }
            }
            editMemberButton.apply {
                contentDescription = holder.itemView.context.getString(R.string.content_description_edit_member_button, member.displayName)
                isVisible = canEditMembers && !isTheLastManager
                setOnClickListener {
                    listener.onEditMember(member)
                }
            }

            member.expirationDateTime?.let {
                expirationCalendarIcon.visibility = View.VISIBLE
                expirationDate.visibility = View.VISIBLE
                expirationDate.text = DisplayUtils.displayDateToHumanReadable(it)
                expirationDate.contentDescription =
                    holder.itemView.context.getString(R.string.content_description_member_expiration_date, expirationDate.text)
            }
        }
    }

    override fun getItemCount(): Int = members.size

    fun setSpaceMembers(
        spaceMembers: List<SpaceMember>,
        roles: List<OCRole>,
        canRemoveMembers: Boolean,
        canEditMembers: Boolean,
        numberOfManagers: Int
    ) {

        val userPermissionsChanged = this.canEditMembers != canEditMembers
        val numberOfManagersChanged = this.numberOfManagers != numberOfManagers

        this.canRemoveMembers = canRemoveMembers
        this.canEditMembers = canEditMembers
        this.rolesMap = roles.associate { it.id to it.displayName }
        val listOfMembersFiltered = spaceMembers.sortedWith(compareByDescending<SpaceMember> {
                member -> roles.indexOfFirst { it.id in member.roles } }.thenBy { member -> member.displayName })
        val diffCallback = SpaceMembersDiffUtil(this.members, listOfMembersFiltered, numberOfManagersChanged, userPermissionsChanged)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.members = listOfMembersFiltered
        this.numberOfManagers = numberOfManagers
        diffResult.dispatchUpdatesTo(this)
    }

    class SpaceMembersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MemberItemBinding.bind(itemView)
    }

    interface SpaceMembersAdapterListener {
        fun onRemoveMember(spaceMember: SpaceMember)
        fun onEditMember(spaceMember: SpaceMember)
    }

    companion object {
        const val GROUP_PREFIX = "g:"
    }
}
