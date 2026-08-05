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

package com.owncloud.android.presentation.sharing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.MemberItemBinding
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.sharing.shares.model.MemberPermission
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils

class GraphSharesAdapter : RecyclerView.Adapter<GraphSharesAdapter.GraphShareViewHolder>() {

    private var shares: List<MemberPermission> = emptyList()
    private var rolesMap: Map<String, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphShareViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.member_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)
        return GraphShareViewHolder(view)
    }

    override fun onBindViewHolder(holder: GraphShareViewHolder, position: Int) {
        val share = shares[position]
        val roleNames = share.roles.mapNotNull { rolesMap[it] }

        holder.binding.apply {
            memberIcon.setImageResource(if (share.isGroup) R.drawable.ic_group else R.drawable.ic_user)
            memberName.text = share.displayName
            memberName.contentDescription = holder.itemView.context.getString(
                if (share.isGroup) R.string.content_description_member_group else R.string.content_description_member_user,
                share.displayName
            )
            memberRole.text = roleNames.joinToString(", ")

            val hasExpirationDate = share.expirationDateTime != null
            expirationCalendarIcon.isVisible = hasExpirationDate
            expirationDate.isVisible = hasExpirationDate
            if (hasExpirationDate) {
                expirationDate.text = DisplayUtils.displayDateToHumanReadable(share.expirationDateTime)
                expirationDate.contentDescription =
                    holder.itemView.context.getString(R.string.content_description_member_expiration_date, expirationDate.text)
            }
        }
    }

    override fun getItemCount(): Int = shares.size

    fun setShares(shares: List<MemberPermission>, roles: List<OCRole>) {
        this.rolesMap = roles.associate { it.id to it.displayName }
        val sortedShares = shares.sortedWith(
            compareByDescending<MemberPermission> { share -> roles.indexOfFirst { it.id in share.roles } }
                .thenBy { it.displayName }
        )
        val diffResult = DiffUtil.calculateDiff(GraphSharesDiffUtil(this.shares, sortedShares))
        this.shares = sortedShares
        diffResult.dispatchUpdatesTo(this)
    }

    class GraphShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = MemberItemBinding.bind(itemView)
    }
}
