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

package com.owncloud.android.presentation.spaces.links

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.PublicLinkItemBinding
import com.owncloud.android.domain.links.model.OCLink
import com.owncloud.android.extensions.toStringResId
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils

class SpaceLinksAdapter: RecyclerView.Adapter<SpaceLinksAdapter.SpaceLinksViewHolder>() {

    private var spaceLinks: List<OCLink> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceLinksViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.public_link_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return SpaceLinksViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpaceLinksViewHolder, position: Int) {
        val spaceLink = spaceLinks[position]
        holder.binding.apply {
            publicLinkDisplayName.text = spaceLink.displayName
            publicLinkType.text = holder.itemView.context.getString(spaceLink.type.toStringResId())

            val hasExpirationDate = spaceLink.expirationDateTime != null
            expirationCalendarIcon.isVisible = hasExpirationDate
            expirationDate.isVisible = hasExpirationDate
            if (hasExpirationDate) {
                expirationDate.apply {
                    text = DisplayUtils.displayDateToHumanReadable(spaceLink.expirationDateTime)
                    contentDescription = holder.itemView.context.getString(R.string.content_description_member_expiration_date, expirationDate.text)
                }
            }
        }
    }

    override fun getItemCount(): Int = spaceLinks.size

    fun setSpaceLinks(spaceLinks: List<OCLink>) {
        val diffCallback = SpaceLinksDiffUtil(this.spaceLinks, spaceLinks)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.spaceLinks = spaceLinks
        diffResult.dispatchUpdatesTo(this)
    }

    class SpaceLinksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = PublicLinkItemBinding.bind(itemView)
    }
}
