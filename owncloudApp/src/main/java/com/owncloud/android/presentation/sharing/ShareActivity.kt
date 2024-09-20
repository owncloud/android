/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.transaction
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.utils.Event.EventObserver
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.sharees.EditPrivateShareFragment
import com.owncloud.android.presentation.sharing.sharees.SearchShareesFragment
import com.owncloud.android.presentation.sharing.sharees.UsersAndGroupsSearchProvider
import com.owncloud.android.presentation.sharing.shares.PublicShareDialogFragment
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.utils.showDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * Activity for sharing files
 */
class ShareActivity : FileActivity(), ShareFragmentListener {
    private val shareViewModel: ShareViewModel by viewModel {
        parametersOf(
            file.remotePath,
            account?.name
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.share_activity)

        setupStandardToolbar(
            title = null,
            displayHomeAsUpEnabled = true,
            homeButtonEnabled = true,
            displayShowTitleEnabled = true,
        )
        supportActionBar?.setHomeActionContentDescription(R.string.common_back)

        supportFragmentManager.transaction {
            if (savedInstanceState == null && file != null && account != null) {
                // Add Share fragment on first creation
                val fragment = ShareFileFragment.newInstance(file, account!!)
                replace(
                    R.id.share_fragment_container, fragment,
                    TAG_SHARE_FRAGMENT
                )
            }
        }

        observePrivateShareCreation()
        observePrivateShareEdition()
        observeShareDeletion()
    }

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    override fun showSearchUsersAndGroups() {
        supportFragmentManager.transaction {
            val searchFragment = SearchShareesFragment.newInstance(file, account)
            replace(
                R.id.share_fragment_container,
                searchFragment,
                TAG_SEARCH_FRAGMENT
            )
            addToBackStack(null)
        }
    }

    // Private share creation needs to be handled from here since is is carried out through intents
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_SEARCH -> {  // Verify the action and get the query
                val query = intent.getStringExtra(SearchManager.QUERY)
                Timber.w("Ignored Intent requesting to query for $query")
            }
            UsersAndGroupsSearchProvider.suggestIntentAction -> {
                val data = intent.data
                val dataString = intent.dataString
                val shareWith = dataString!!.substring(dataString.lastIndexOf('/') + 1)
                createPrivateShare(
                    shareWith,
                    data?.authority
                )
            }
            else -> Timber.e("Unexpected intent $intent")
        }
    }

    private fun createPrivateShare(shareeName: String, dataAuthority: String?) {
        val shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority)

        shareViewModel.insertPrivateShare(
            file.remotePath,
            shareType,
            shareeName,
            getAppropriatePermissions(shareType),
            account.name
        )
    }

    private fun observePrivateShareCreation() {
        shareViewModel.privateShareCreationStatus.observe(
            this,
            EventObserver { uiResult ->
                when (uiResult) {
                    is UIResult.Error -> {
                        showErrorInSnackbar(R.string.share_link_file_error, uiResult.error)
                        dismissLoadingDialog()
                    }
                    is UIResult.Loading -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    is UIResult.Success -> {
                        // Needs to trigger refresh after creation because creation request returns wrong path resulting list not getting updated.
                        shareViewModel.refreshSharesFromNetwork()
                    }
                }
            }
        )
    }

    private fun getAppropriatePermissions(shareType: ShareType?): Int {
        // check if the Share is FERERATED
        val isFederated = ShareType.FEDERATED == shareType

        val permissions = when {
            file.isSharedWithMe -> RemoteShare.READ_PERMISSION_FLAG    // minimum permissions
            isFederated ->
                if (file.isFolder) {
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FOLDER
                } else {
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FILE
                }
            else ->
                if (file.isFolder) {
                    RemoteShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
                } else {
                    RemoteShare.MAXIMUM_PERMISSIONS_FOR_FILE
                }
        }

        return if (shareViewModel.isResharingAllowed()) permissions else permissions - RemoteShare.SHARE_PERMISSION_FLAG
    }

    private fun observePrivateShareEdition() {
        shareViewModel.privateShareEditionStatus.observe(
            this,
            EventObserver { uiResult ->
                when (uiResult) {
                    is UIResult.Error -> {
                        showErrorInSnackbar(R.string.share_link_file_error, uiResult.error)
                        dismissLoadingDialog()
                    }
                    is UIResult.Loading -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    is UIResult.Success -> {}
                }
            }
        )
    }

    override fun showEditPrivateShare(share: OCShare) {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT)
        if (prev != null) {
            ft.remove(prev)    // BACK button will recover the previous fragment
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val newFragment = EditPrivateShareFragment.newInstance(share, file, account)
        newFragment.show(
            ft,
            TAG_EDIT_SHARE_FRAGMENT
        )
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
        fileOperationsHelper.copyOrSendPrivateLink(file)
    }

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    override fun showAddPublicShare(defaultLinkName: String) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.

        // Create and show the dialog
        val createPublicShareFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            account,
            defaultLinkName
        )

        showDialogFragment(
            createPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun showEditPublicShare(share: OCShare) {
        // Create and show the dialog.
        val editPublicShareFragment = PublicShareDialogFragment.newInstanceToUpdate(file, account, share)
        showDialogFragment(
            editPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    /**************************************************************************************************************
     ************************************************** COMMON ****************************************************
     **************************************************************************************************************/

    private fun observeShareDeletion() {
        shareViewModel.shareDeletionStatus.observe(
            this,
            EventObserver { uiResult ->
                when (uiResult) {
                    is UIResult.Error -> {
                        dismissLoadingDialog()
                        showErrorInSnackbar(R.string.unshare_link_file_error, uiResult.error)
                    }
                    is UIResult.Loading -> {
                        showLoading()
                    }
                    is UIResult.Success -> {}
                }
            }
        )
    }

    override fun showRemoveShare(share: OCShare) {
        val removePublicShareFragment = RemoveShareDialogFragment.newInstance(share, account)
        showDialogFragment(
            removePublicShareFragment,
            TAG_REMOVE_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun deleteShare(remoteId: String) {
        shareViewModel.deleteShare(remoteId)
    }

    override fun copyOrSendPublicLink(share: OCShare) {
        fileOperationsHelper.copyOrSendPublicLink(share)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            android.R.id.home -> if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    override fun showLoading() {
        showLoadingDialog(R.string.common_loading)
    }

    override fun dismissLoading() {
        dismissLoadingDialog()
    }

    // The main_menu won't be displayed
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (findViewById<View>(R.id.owncloud_app_bar).hasFocus()) {
                    findViewById<View>(R.id.share_fragment_container).requestFocus()
                }
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    companion object {
        const val TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT"
        const val TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT"
        const val TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT"
        const val TAG_PUBLIC_SHARE_DIALOG_FRAGMENT = "PUBLIC_SHARE_DIALOG_FRAGMENT"
        const val TAG_REMOVE_SHARE_DIALOG_FRAGMENT = "REMOVE_SHARE_DIALOG_FRAGMENT"
    }
}
