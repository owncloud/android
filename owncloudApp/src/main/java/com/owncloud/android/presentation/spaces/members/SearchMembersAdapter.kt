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
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MemberItemBinding
import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.utils.PreferenceUtils

class SearchMembersAdapter: RecyclerView.Adapter<SearchMembersAdapter.SearchMembersViewHolder>() {

    private var members: List<OCMember> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchMembersViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.member_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return SearchMembersViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchMembersViewHolder, position: Int) {
        val member = members[position]

        holder.binding.apply {
            memberIcon.setImageResource(R.drawable.ic_user)
            memberName.text = member.displayName
            memberName.contentDescription = holder.itemView.context.getString(R.string.content_description_member_user, member.displayName)
            memberRole.text = if (member.surname == USER_SURNAME) holder.itemView.context.getString(R.string.member_user) else member.surname
        }
    }

    override fun getItemCount(): Int = members.size

    fun addUserMembers(members: List<OCMember>) {
        this.members = members
        notifyDataSetChanged()
    }

    class SearchMembersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MemberItemBinding.bind(itemView)
    }

    companion object {
        private const val USER_SURNAME = "User"
    }

}
