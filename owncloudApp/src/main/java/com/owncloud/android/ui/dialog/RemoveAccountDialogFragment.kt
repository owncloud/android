/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import org.koin.java.KoinJavaComponent.inject

/**
 * Dialog requiring confirmation before removing an OC Account.
 *
 * Removes the account if the user confirms.
 *
 * Container Activity needs to implement AccountManagerCallback<Boolean>.
</Boolean> */
class RemoveAccountDialogFragment : ConfirmationDialogFragment(), ConfirmationDialogFragmentListener {
    val viewModel: RemoveAccountDialogViewModel by inject(RemoveAccountDialogViewModel::class.java)

    private var targetAccount: Account? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // checked here to fail soon in case of wrong usage
        try {
            activity as AccountManagerCallback<Boolean>?
        } catch (c: ClassCastException) {
            throw IllegalStateException(
                "Container Activity needs to implement (AccountManagerCallback<Boolean>)", c
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        targetAccount = requireArguments().getParcelable(ARG_TARGET_ACCOUNT)
        setOnConfirmationListener(this)
        return dialog
    }

    /**
     * Performs the removal of the target account.
     */
    override fun onConfirmation(callerTag: String) {
        val parentActivity: Activity? = activity
        val am = AccountManager.get(parentActivity)
        val callback = parentActivity as AccountManagerCallback<Boolean>?
        am.removeAccount(targetAccount, callback, Handler())

        // Reset camera uploads if they were enabled for this account
        viewModel.resetCameraUploadsForAccount(targetAccount!!.name)

        // Notify removal to Document Provider
        val authority = resources.getString(R.string.document_provider_authority)
        val rootsUri = DocumentsContract.buildRootsUri(authority)
        requireContext().contentResolver.notifyChange(rootsUri, null)
    }

    override fun onCancel(callerTag: String) {
        // nothing to do here
    }

    override fun onNeutral(callerTag: String) {
        // nothing to do here
    }

    companion object {
        private const val ARG_TARGET_ACCOUNT = "TARGET_ACCOUNT"

        /**
         * Public factory method to create new RemoveAccountDialogFragment instances.
         *
         * @param account Account to remove.
         * @param accountAttachedToCameraUploads true if camera uploads are enabled for this account. Removing the account will disable camera uploads.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(account: Account, accountAttachedToCameraUploads: Boolean): RemoveAccountDialogFragment {
            val dialogMessage = if (accountAttachedToCameraUploads) {
                R.string.confirmation_remove_account_alert_camera_uploads
            } else {
                R.string.confirmation_remove_account_alert
            }
            val args = Bundle().apply {
                putInt(ARG_MESSAGE_RESOURCE_ID, dialogMessage)
                putStringArray(ARG_MESSAGE_ARGUMENTS, arrayOf(account.name))
                putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes)
                putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no)
                putInt(ARG_NEGATIVE_BTN_RES, -1)
                putParcelable(ARG_TARGET_ACCOUNT, account)
            }
            return RemoveAccountDialogFragment().apply { arguments = args }
        }
    }
}
