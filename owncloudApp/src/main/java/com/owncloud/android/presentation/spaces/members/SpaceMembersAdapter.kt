/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MemberItemBinding
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.domain.spaces.model.SpaceMembers
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils

class SpaceMembersAdapter: RecyclerView.Adapter<SpaceMembersAdapter.SpaceMembersViewHolder>() {

    private var members: List<SpaceMember> = emptyList()
    private var rolesMap: Map<String, String> = emptyMap()

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
            memberIcon.setImageResource(if (member.id.startsWith(GROUP_PREFIX)) R.drawable.ic_group else R.drawable.ic_user)
            memberName.text = member.displayName
            memberRole.text = roleNames.joinToString(", ")

            member.expirationDateTime?.let {
                expirationCalendarIcon.visibility = View.VISIBLE
                expirationDate.visibility = View.VISIBLE
                expirationDate.text = DisplayUtils.displayDateToHumanReadable(it)
            }
        }
    }

    override fun getItemCount(): Int = members.size

    fun setSpaceMembers(spaceMembers: SpaceMembers, roles: List<OCRole>) {
        this.rolesMap = roles.associate { it.id to it.displayName }
        this.members = spaceMembers.members.sortedByDescending { member -> roles.indexOfFirst { it.id in member.roles } }
        notifyDataSetChanged()
    }

    class SpaceMembersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MemberItemBinding.bind(itemView)
    }

    companion object {
        const val GROUP_PREFIX = "g:"
    }
}
