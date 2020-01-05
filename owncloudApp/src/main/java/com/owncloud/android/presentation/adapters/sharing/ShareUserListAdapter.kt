/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.presentation.adapters.sharing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.owncloud.android.R
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.utils.PreferenceUtils
import java.util.ArrayList

/**
 * Adapter to show a user/group in Share With List
 */
class ShareUserListAdapter(
    private val mContext: Context, resource: Int,
    private var shares: List<OCShare>?,
    private val listener: ShareUserAdapterListener
) : ArrayAdapter<OCShare>(mContext, resource) {

    init {
        shares = ArrayList(shares?.sortedWith(compareBy { it.sharedWithDisplayName }))
    }

    override fun getCount(): Int = shares!!.size

    override fun getItem(position: Int): OCShare? = shares!![position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflator = mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflator.inflate(R.layout.share_user_item, parent, false)

        // Allow or disallow touches with other visible windows
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(mContext)

        if (shares != null && shares?.size!! > position) {
            val share = shares!![position]

            val userName = view.findViewById<TextView>(R.id.userOrGroupName)
            val iconView = view.findViewById<ImageView>(R.id.icon)
            var name = share.sharedWithDisplayName
            name = if (share.sharedWithAdditionalInfo!!.isEmpty())
                name
            else
                name + " (" + share.sharedWithAdditionalInfo + ")"
            var icon = context.resources.getDrawable(R.drawable.ic_user)
            iconView.tag = R.drawable.ic_user
            if (share.shareType == ShareType.GROUP) {
                name = context.getString(R.string.share_group_clarification, name)
                icon = context.resources.getDrawable(R.drawable.ic_group)
                iconView.tag = R.drawable.ic_group
            }
            userName.text = name
            iconView.setImageDrawable(icon)

            /// bind listener to edit privileges
            val editShareButton = view.findViewById<ImageView>(R.id.editShareButton)
            editShareButton.setOnClickListener { listener.editShare(shares!![position]) }

            /// bind listener to unshare
            val unshareButton = view.findViewById<ImageView>(R.id.unshareButton)
            unshareButton.setOnClickListener { listener.unshareButtonPressed(shares!![position]) }

        }
        return view
    }

    interface ShareUserAdapterListener {
        fun unshareButtonPressed(share: OCShare)
        fun editShare(share: OCShare)
    }
}
