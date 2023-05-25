package com.owncloud.android.presentation.sharing.shares

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.owncloud.android.databinding.SharePublicLinkItemBinding
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.utils.PreferenceUtils

/**
 * Adapter to show a list of public links
 */
class SharePublicLinkListAdapter(
    private val mContext: Context,
    resource: Int,
    private var publicLinks: List<OCShare>,
    private val listener: SharePublicLinkAdapterListener
) : ArrayAdapter<OCShare>(mContext, resource) {

    private lateinit var binding: SharePublicLinkItemBinding

    init {
        publicLinks = publicLinks.sortedBy { it.name }
    }

    override fun getCount(): Int = publicLinks.size

    override fun getItem(position: Int): OCShare = publicLinks[position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = SharePublicLinkItemBinding.inflate(inflator).apply {
            root.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(mContext)
        }

        if (publicLinks.size > position) {
            val share = publicLinks[position]

            // If there's no name, set the token as name
            binding.publicLinkName.text = if (share.name.isNullOrEmpty()) share.token else share.name

            // bind listener to get link
            binding.getPublicLinkButton.setOnClickListener { listener.copyOrSendPublicLink(publicLinks[position]) }

            // bind listener to delete
            binding.deletePublicLinkButton.setOnClickListener { listener.removeShare(publicLinks[position]) }

            // bind listener to edit
            binding.editPublicLinkButton.setOnClickListener { listener.editPublicShare(publicLinks[position]) }
        }

        return binding.root
    }

    interface SharePublicLinkAdapterListener {
        fun copyOrSendPublicLink(share: OCShare)

        fun removeShare(share: OCShare)

        fun editPublicShare(share: OCShare)
    }
}
