/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
 *
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

package com.owncloud.android.ui.activity

import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.MenuItem
import com.owncloud.android.MainApp

import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.operations.CreateShareViaLinkOperation
import com.owncloud.android.operations.GetSharesForFileOperation
import com.owncloud.android.operations.RemoveShareOperation
import com.owncloud.android.operations.UpdateSharePermissionsOperation
import com.owncloud.android.operations.UpdateShareViaLinkOperation
import com.owncloud.android.providers.UsersAndGroupsSearchProvider
import com.owncloud.android.shares.viewmodel.ShareViewModel
import com.owncloud.android.ui.asynctasks.GetSharesForFileAsyncTask
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.fragment.EditShareFragment
import com.owncloud.android.ui.fragment.PublicShareDialogFragment
import com.owncloud.android.ui.fragment.SearchShareesFragment
import com.owncloud.android.ui.fragment.ShareFileFragment
import com.owncloud.android.ui.fragment.ShareFragmentListener


/**
 * Activity for sharing files
 */

class ShareActivity : FileActivity(), ShareFragmentListener {

    internal var mGetSharesForFileAsyncTask: GetSharesForFileAsyncTask? = null

    /**
     * Shortcut to get access to the [ShareFileFragment] instance, if any
     *
     * @return A [ShareFileFragment] instance, or null
     */
    private val shareFileFragment: ShareFileFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SHARE_FRAGMENT) as ShareFileFragment

    /**
     * Shortcut to get access to the [SearchShareesFragment] instance, if any
     *
     * @return A [SearchShareesFragment] instance, or null
     */
    private val searchFragment: SearchShareesFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SEARCH_FRAGMENT) as SearchShareesFragment

    /**
     * Shortcut to get access to the [PublicShareDialogFragment] instance, if any
     *
     * @return A [PublicShareDialogFragment] instance, or null
     */
    private val publicShareFragment: PublicShareDialogFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT) as PublicShareDialogFragment

    /**
     * Shortcut to get access to the [EditShareFragment] instance, if any
     *
     * @return A [EditShareFragment] instance, or null
     */
    private val editShareFragment: EditShareFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT) as EditShareFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGetSharesForFileAsyncTask = null

        setContentView(R.layout.share_activity)

        // Set back button
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val ft = supportFragmentManager.beginTransaction()

        if (savedInstanceState == null) {
            // Add Share fragment on first creation
            val fragment = ShareFileFragment.newInstance(file, account)
            ft.replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT)
            ft.commit()
        }

    }

    protected inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
        }

    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)

        val shareViewModel: ShareViewModel = ViewModelProviders.of(this, viewModelFactory {
            ShareViewModel(
                application,
                this,
                OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                    OwnCloudAccount(account, this),
                    this
                ),
                file.remotePath,
                listOf(ShareType.PUBLIC_LINK.value)
            )
        }).get(ShareViewModel::class.java)

        shareViewModel.sharesForFile.observe(
            this,
            Observer { sharesForFile ->
                Log_OC.d(TAG, sharesForFile.toString())
            }
        )
        //        // Load data into the list
        //        Log_OC.d(TAG, "Refreshing lists on account set");
        //        refreshSharesFromStorageManager();
        //
        //        // Request for a refresh of the data through the server (starts an Async Task)
        //        refreshSharesFromServer();
    }


    override fun onNewIntent(intent: Intent) {
        // Verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            Log_OC.w(TAG, "Ignored Intent requesting to query for $query")

        } else if (UsersAndGroupsSearchProvider.getSuggestIntentAction() == intent.action) {
            val data = intent.data
            val dataString = intent.dataString
            val shareWith = dataString!!.substring(dataString.lastIndexOf('/') + 1)
            doShareWith(
                shareWith,
                data!!.authority
            )

        } else {
            Log_OC.e(TAG, "Unexpected intent " + intent.toString())
        }
    }

    public override fun onStop() {
        super.onStop()
        if (mGetSharesForFileAsyncTask != null) {
            mGetSharesForFileAsyncTask!!.cancel(true)
            mGetSharesForFileAsyncTask = null
        }
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
        fileOperationsHelper.copyOrSendPrivateLink(file)
    }

    private fun doShareWith(shareeName: String, dataAuthority: String?) {

        val shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority)

        fileOperationsHelper.shareFileWithSharee(
            file,
            shareeName,
            shareType,
            getAppropiatePermissions(shareType)
        )
    }

    private fun getAppropiatePermissions(shareType: ShareType): Int {

        // check if the Share is FERERATED
        val isFederated = ShareType.FEDERATED == shareType

        if (file.isSharedWithMe) {
            return RemoteShare.READ_PERMISSION_FLAG    // minimum permissions

        } else if (isFederated) {
            val serverVersion = com.owncloud.android.authentication.AccountUtils.getServerVersion(account)
            return if (serverVersion != null && serverVersion.isNotReshareableFederatedSupported) {
                if (file.isFolder)
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9
                else
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9
            } else {
                if (file.isFolder)
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9
                else
                    RemoteShare.FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9
            }
        } else {
            return if (file.isFolder)
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            else
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FILE
        }
    }

    override fun showSearchUsersAndGroups() {
        val searchFragment = SearchShareesFragment.newInstance(file, account)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.share_fragment_container, searchFragment, TAG_SEARCH_FRAGMENT)
        ft.addToBackStack(null)    // BACK button will recover the ShareFragment
        ft.commit()
    }

    override fun showEditPrivateShare(share: RemoteShare) {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT)
        if (prev != null) {
            ft.remove(prev)    // BACK button will recover the previous fragment
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val newFragment = EditShareFragment.newInstance(share, file, account)
        newFragment.show(ft, TAG_EDIT_SHARE_FRAGMENT)

    }

    override// Call to Remove share operation
    fun removeShare(share: RemoteShare) {
        fileOperationsHelper.removeShare(share)
    }

    /**
     * Get users and groups from the server to fill in the "share with" list
     */
    override fun refreshSharesFromServer() {
        // Show loading
        showLoadingDialog(R.string.common_loading)
        // Get Users and Groups
        mGetSharesForFileAsyncTask = GetSharesForFileAsyncTask(this)
        val params = arrayOf(file, account, storageManager)
        mGetSharesForFileAsyncTask!!.execute(*params)
    }

    override fun showAddPublicShare(defaultLinkName: String) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        // Create and show the dialog
        val newFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            account,
            defaultLinkName
        )
        newFragment.show(ft, TAG_PUBLIC_SHARE_DIALOG_FRAGMENT)
    }

    override fun showEditPublicShare(share: RemoteShare) {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val newFragment = PublicShareDialogFragment.newInstanceToUpdate(
            file, share,
            account
        )
        newFragment.show(ft, TAG_PUBLIC_SHARE_DIALOG_FRAGMENT)
    }

    override fun copyOrSendPublicLink(share: RemoteShare) {
        fileOperationsHelper.copyOrSendPublicLink(share)
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)

        if (result.isSuccess || operation is GetSharesForFileOperation && result.code == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh")
            refreshSharesFromStorageManager()
            if (operation is GetSharesForFileOperation) {
                mGetSharesForFileAsyncTask = null
            }
        }

        if (operation is CreateShareViaLinkOperation) {
            onCreateShareViaLinkOperationFinish(operation, result as RemoteOperationResult<ShareParserResult>)
        }

        if (operation is UpdateShareViaLinkOperation) {
            onUpdateShareViaLinkOperationFinish(operation, result as RemoteOperationResult<ShareParserResult>)
        }

        if (operation is RemoveShareOperation && result.isSuccess && editShareFragment != null) {
            supportFragmentManager.popBackStack()
        }

        if (operation is UpdateSharePermissionsOperation
            && editShareFragment != null && editShareFragment!!.isAdded
        ) {
            editShareFragment!!.onUpdateSharePermissionsFinished(result as RemoteOperationResult<ShareParserResult>?)
        }
    }

    private fun onCreateShareViaLinkOperationFinish(
        operation: CreateShareViaLinkOperation,
        result: RemoteOperationResult<ShareParserResult>
    ) {
        if (result.isSuccess) {
            updateFileFromDB()

            publicShareFragment!!.dismiss()

            fileOperationsHelper.copyOrSendPublicLink(result.data.shares[0])

        } else {
            publicShareFragment!!.showError(
                ErrorMessageAdapter.getResultMessage(result, operation, resources)
            )
        }
    }

    private fun onUpdateShareViaLinkOperationFinish(
        operation: UpdateShareViaLinkOperation,
        result: RemoteOperationResult<ShareParserResult>
    ) {
        if (result.isSuccess) {
            updateFileFromDB()

            publicShareFragment!!.dismiss()

            fileOperationsHelper.copyOrSendPublicLink(result.data.shares[0])

        } else {
            publicShareFragment!!.showError(
                ErrorMessageAdapter.getResultMessage(result, operation, resources)
            )
        }
    }

    /**
     * Updates the view, reading data from [com.owncloud.android.datamodel.FileDataStorageManager]
     */
    private fun refreshSharesFromStorageManager() {

        val shareFileFragment = shareFileFragment
        if (shareFileFragment != null && shareFileFragment.isAdded) {   // only if added to the view hierarchy!!
            shareFileFragment.refreshCapabilitiesFromDB()
            shareFileFragment.refreshUsersOrGroupsListFromDB()
            shareFileFragment.refreshPublicSharesListFromDB()
        }

        val searchShareesFragment = searchFragment
        if (searchShareesFragment != null && searchShareesFragment.isAdded) {  // only if added to the view hierarchy!!
            searchShareesFragment.refreshUsersOrGroupsListFromDB()
        }

        val publicShareDialogFragment = publicShareFragment
        if (publicShareDialogFragment != null && publicShareDialogFragment.isAdded) {  // only if added to the view hierarchy!!
            publicShareDialogFragment.refreshModelFromStorageManager()
        }

        val editShareFragment = editShareFragment
        if (editShareFragment != null && editShareFragment.isAdded) {
            editShareFragment.refreshUiFromDB()
        }
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

    companion object {

        private val TAG = ShareActivity::class.java.simpleName

        private val TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT"
        private val TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT"
        private val TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT"
        private val TAG_PUBLIC_SHARE_DIALOG_FRAGMENT = "PUBLIC_SHARE_DIALOG_FRAGMENT"
        val TAG_REMOVE_SHARE_DIALOG_FRAGMENT = "REMOVE_SHARE_DIALOG_FRAGMENT"
    }
}
