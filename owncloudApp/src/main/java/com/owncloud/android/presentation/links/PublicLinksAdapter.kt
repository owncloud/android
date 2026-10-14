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

package com.owncloud.android.presentation.links

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

class PublicLinksAdapter(
    private val listener: PublicLinksAdapterListener
): RecyclerView.Adapter<PublicLinksAdapter.PublicLinksViewHolder>() {

    private var publicLinks: List<OCLink> = emptyList()
    private var canRemoveLinks = false
    private var canEditLinks = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicLinksViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.public_link_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return PublicLinksViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicLinksViewHolder, position: Int) {
        val publicLink = publicLinks[position]
        holder.binding.apply {
            publicLinkDisplayName.text = publicLink.displayName
            publicLinkType.text = holder.itemView.context.getString(publicLink.type.toStringResId())

            val hasExpirationDate = publicLink.expirationDateTime != null
            expirationCalendarIcon.isVisible = hasExpirationDate
            expirationDate.isVisible = hasExpirationDate
            if (hasExpirationDate) {
                expirationDate.apply {
                    text = DisplayUtils.displayDateToHumanReadable(publicLink.expirationDateTime)
                    contentDescription = holder.itemView.context.getString(R.string.content_description_member_expiration_date, expirationDate.text)
                }
            }
            copyPublicLinkButton.apply {
                contentDescription = holder.itemView.context.getString(R.string.content_description_get_public_link, publicLink.displayName)
                setOnClickListener {
                    listener.onCopyOrSendPublicLink(publicLink.webUrl)
                }
            }

            removePublicLinkButton.apply {
                contentDescription = holder.itemView.context.getString(R.string.content_description_delete_public_link, publicLink.displayName)
                isVisible = canRemoveLinks
                setOnClickListener {
                    listener.onRemovePublicLink(publicLink.id, publicLink.displayName)
                }
            }

            editPublicLinkButton.apply {
                contentDescription = holder.itemView.context.getString(R.string.content_description_edit_public_link, publicLink.displayName)
                isVisible = canEditLinks
                setOnClickListener {
                    listener.onEditPublicLink(publicLink)
                }
            }
        }
    }

    override fun getItemCount(): Int = publicLinks.size

    fun setPublicLinks(publicLinks: List<OCLink>, canRemoveLinks: Boolean, canEditLinks: Boolean) {
        val hasUserPermissionsChanged = this.canEditLinks != canEditLinks || this.canRemoveLinks != canRemoveLinks
        this.canRemoveLinks = canRemoveLinks
        this.canEditLinks = canEditLinks
        val diffCallback = PublicLinksDiffUtil(this.publicLinks, publicLinks, hasUserPermissionsChanged)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.publicLinks = publicLinks
        diffResult.dispatchUpdatesTo(this)
    }

    class PublicLinksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = PublicLinkItemBinding.bind(itemView)
    }

    interface PublicLinksAdapterListener {
        fun onCopyOrSendPublicLink(publicLinkUrl: String)
        fun onRemovePublicLink(publicLinkId: String, publicLinkDisplayName: String)
        fun onEditPublicLink(publicLink: OCLink)
    }
}
