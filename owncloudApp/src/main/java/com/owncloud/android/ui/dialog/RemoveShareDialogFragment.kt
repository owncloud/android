/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
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

package com.owncloud.android.ui.dialog

/**
 * Dialog requiring confirmation before removing a share.
 * Triggers the removal according to the user response.
 */

import android.app.Dialog
import android.os.Bundle
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.fragment.ShareFragmentListener

class RemoveShareDialogFragment : ConfirmationDialogFragment(), ConfirmationDialogFragmentListener {

    private var targetShare: RemoteShare? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        targetShare = arguments!!.getParcelable(ARG_TARGET_SHARE)

        setOnConfirmationListener(this)

        return dialog
    }

    /**
     * Performs the removal of the target share, both locally and in the server.
     */
    override fun onConfirmation(callerTag: String) {
        val listener = activity as ShareFragmentListener?
        Log_OC.d(TAG, "Removing public share " + targetShare!!.name)

    }

    override fun onCancel(callerTag: String) {
        // nothing to do here
    }

    override fun onNeutral(callerTag: String) {
        // nothing to do here
    }

    companion object {

        private val TAG = RemoveShareDialogFragment::class.java.name

        private val ARG_TARGET_SHARE = "TARGET_SHARE"

        /**
         * Public factory method to create new RemoveFilesDialogFragment instances.
         *
         * @param share           [OCShare] to remove.
         * @return                Dialog ready to show.
         */
        fun newInstance(share: OCShare): RemoveShareDialogFragment {
            val frag = RemoveShareDialogFragment()
            val args = Bundle()

            args.putInt(
                ARG_MESSAGE_RESOURCE_ID,
                R.string.confirmation_remove_public_share_message
            )
            args.putStringArray(
                ARG_MESSAGE_ARGUMENTS,
                arrayOf(if (share.name!!.isNotEmpty()) share.name!! else share.token!!)
            )
            args.putInt(ARG_TITLE_ID, R.string.confirmation_remove_public_share_title)
            args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes)
            args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no)
            args.putInt(ARG_NEGATIVE_BTN_RES, -1)
            frag.arguments = args

            return frag
        }
    }
}
