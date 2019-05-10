package com.owncloud.android.ui.adapter

/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.R
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.android.synthetic.main.share_public_link_item.view.*
import java.util.ArrayList

/**
 * Adapter to show a list of public links
 */
class SharePublicLinkListAdapter(
    private val mContext: Context, resource: Int, private val mPublicLinks: ArrayList<OCShare>?,
    private val mListener: SharePublicLinkAdapterListener
) : ArrayAdapter<OCShare>(mContext, resource) {

    override fun getCount(): Int {
        return mPublicLinks!!.size
    }

    override fun getItem(position: Int): OCShare? = mPublicLinks!![position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflator.inflate(R.layout.share_public_link_item, parent, false)

        // Allow or disallow touches with other visible windows
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(mContext)

        if (mPublicLinks != null && mPublicLinks.size > position) {
            val share = mPublicLinks[position]

            // If there's no name, set the token as name
            view.publicLinkName.text = if (share.name?.isEmpty()!!) share.token else share.name

            // bind listener to get link
            view.getPublicLinkButton.setOnClickListener { mListener.copyOrSendPublicLink(mPublicLinks[position]) }

            // bind listener to delete
            view.deletePublicLinkButton.setOnClickListener { mListener.removePublicShare(mPublicLinks[position]) }

            // bind listener to edit
            view.editPublicLinkButton.setOnClickListener { mListener.editPublicShare(mPublicLinks[position]) }
        }

        return view
    }

    interface SharePublicLinkAdapterListener {
        fun copyOrSendPublicLink(share: OCShare)

        fun removePublicShare(share: OCShare)

        fun editPublicShare(share: OCShare)
    }
}
