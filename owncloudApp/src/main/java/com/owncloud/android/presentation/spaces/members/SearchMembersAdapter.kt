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
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MemberItemBinding
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.utils.PreferenceUtils

class SearchMembersAdapter(
    private val listener: SearchMembersAdapterListener
) : RecyclerView.Adapter<SearchMembersAdapter.SearchMembersViewHolder>() {

    private var members = mutableListOf<OCMember>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchMembersViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.member_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return SearchMembersViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchMembersViewHolder, position: Int) {
        val member = members[position]

        holder.binding.apply {
            val isGroup = member.surname == GROUP_SURNAME
            memberIcon.setImageResource(if (isGroup) R.drawable.ic_group else R.drawable.ic_user)
            memberName.text = member.displayName
            memberName.contentDescription = holder.itemView.context.getString(
                if (isGroup) R.string.content_description_member_group else R.string.content_description_member_user, member.displayName
            )
            memberRole.text = if (isGroup) {
                holder.itemView.context.getString(R.string.member_type_group)
            } else {
                if (member.surname == USER_SURNAME) holder.itemView.context.getString(R.string.member_type_user) else member.surname
            }

            memberItemLayout.setOnClickListener {
                listener.onMemberClick(member)
            }
        }
    }

    override fun getItemCount(): Int = members.size

    fun setMembers(members: List<OCMember>) {
        val diffCallback = SearchMembersDiffUtil(this.members, members)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.members.clear()
        this.members.addAll(members)
        diffResult.dispatchUpdatesTo(this)
    }

    interface SearchMembersAdapterListener {
        fun onMemberClick(member: OCMember)
    }

    class SearchMembersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MemberItemBinding.bind(itemView)
    }

    companion object {
        private const val USER_SURNAME = "User"
        private const val GROUP_SURNAME = "Group"
    }
}
