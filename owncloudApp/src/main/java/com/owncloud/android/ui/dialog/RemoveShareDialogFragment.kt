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

import android.accounts.Account
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import com.owncloud.android.R
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFragmentListener
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RemoveShareDialogFragment : ConfirmationDialogFragment(), ConfirmationDialogFragmentListener {
    private var targetShare: OCShareEntity? = null
    private var account: Account? = null

    /**
     * Reference to parent listener
     */
    private var listener: ShareFragmentListener? = null

    private val ocShareViewModel: OCShareViewModel by viewModel {
        parametersOf(
            targetShare?.path,
            account
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        targetShare = arguments!!.getParcelable(ARG_TARGET_SHARE)
        account = arguments!!.getParcelable(ARG_ACCOUNT)

        setOnConfirmationListener(this)

        return dialog
    }

    /**
     * Performs the removal of the target share, both locally and in the server.
     */
    override fun onConfirmation(callerTag: String) {
        Log_OC.d(TAG, "Removing share " + targetShare!!.name)
        ocShareViewModel.deleteShare(targetShare?.remoteId!!)
    }

    override fun onCancel(callerTag: String) {
        // nothing to do here
    }

    override fun onNeutral(callerTag: String) {
        // nothing to do here
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeShareDeletion()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = activity as ShareFragmentListener?
        } catch (e: IllegalStateException) {
            throw IllegalStateException(activity!!.toString() + " must implement OnShareFragmentInteractionListener")
        }
    }

    private fun observeShareDeletion() {
        ocShareViewModel.shareDeletionStatus.observe(
            this,
            Observer { uiResult ->
                when (uiResult) {
                    is UIResult.Success -> {
                        dismiss()
                    }
                    is UIResult.Loading -> {
                        listener?.showLoading()
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when removing share"
                        )
                    }
                }
            }
        )
    }

    companion object {

        private val TAG = RemoveShareDialogFragment::class.java.name

        private const val ARG_TARGET_SHARE = "TARGET_SHARE"
        private const val ARG_ACCOUNT = "ACCOUNT"

        /**
         * Public factory method to create new RemoveFilesDialogFragment instances.
         *
         * @param share           [OCShareEntity] to remove.
         * @param account         [Account] which the share belongs to
         * @return                Dialog ready to show.
         */
        fun newInstance(share: OCShareEntity, account: Account): RemoveShareDialogFragment {
            val frag = RemoveShareDialogFragment()
            val args = Bundle()

            args.putInt(
                ARG_MESSAGE_RESOURCE_ID,
                R.string.confirmation_remove_share_message
            )

            val privateShareName = if (share.sharedWithAdditionalInfo!!.isEmpty()) {
                share.sharedWithDisplayName
            } else {
                share.sharedWithDisplayName + " (" + share.sharedWithAdditionalInfo + ")"
            }

            args.putStringArray(
                ARG_MESSAGE_ARGUMENTS,
                arrayOf(if (share.name!!.isNotEmpty()) share.name!! else privateShareName)
            )
            args.putInt(
                ARG_TITLE_ID,
                if (share.shareType == ShareType.PUBLIC_LINK.value)
                    R.string.confirmation_remove_public_share_title else
                    R.string.confirmation_remove_private_share_title
            )
            args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes)
            args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no)
            args.putInt(ARG_NEGATIVE_BTN_RES, -1)
            args.putParcelable(ARG_TARGET_SHARE, share)
            args.putParcelable(ARG_ACCOUNT, account)
            frag.arguments = args

            return frag
        }
    }
}
