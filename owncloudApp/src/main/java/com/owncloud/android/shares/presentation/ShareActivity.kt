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

package com.owncloud.android.shares.presentation

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.RemoveShareOperation
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.sharees.presentation.SearchShareesFragment
import com.owncloud.android.sharees.presentation.UsersAndGroupsSearchProvider
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.presentation.fragment.EditPrivateShareFragment
import com.owncloud.android.shares.presentation.fragment.PublicShareDialogFragment
import com.owncloud.android.shares.presentation.fragment.ShareFileFragment
import com.owncloud.android.shares.presentation.fragment.ShareFragmentListener
import com.owncloud.android.testing.OpenForTesting
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.RemoveShareDialogFragment
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.utils.showDialogFragment
import com.owncloud.android.vo.Status
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Activity for sharing files
 */
@OpenForTesting
class ShareActivity : FileActivity(), ShareFragmentListener {
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
    private val searchShareesFragment: SearchShareesFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_SEARCH_FRAGMENT) as SearchShareesFragment?

    /**
     * Shortcut to get access to the [PublicShareDialogFragment] instance, if any
     *
     * @return A [PublicShareDialogFragment] instance, or null
     */
    private val publicShareFragment: PublicShareDialogFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_PUBLIC_SHARE_DIALOG_FRAGMENT) as PublicShareDialogFragment?

    /**
     * Shortcut to get access to the [EditPrivateShareFragment] instance, if any
     *
     * @return A [EditPrivateShareFragment] instance, or null
     */
    private val editPrivateShareFragment: EditPrivateShareFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT) as EditPrivateShareFragment?

    private val ocShareViewModel: OCShareViewModel by viewModel {
        parametersOf(
            account!!
        )
    }

    private val ocCapabilityViewModel: OCCapabilityViewModel by viewModel {
        parametersOf(
            account!!
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.share_activity)

        // Set back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val ft = supportFragmentManager.beginTransaction()

        if (savedInstanceState == null && file != null && account != null) {
            // Add Share fragment on first creation
            val fragment = ShareFileFragment.newInstance(file, account!!)
            ft.replace(
                R.id.share_fragment_container, fragment,
                TAG_SHARE_FRAGMENT
            )
            ft.commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEARCH -> {  // Verify the action and get the query
                val query = intent.getStringExtra(SearchManager.QUERY)
                Log_OC.w(TAG, "Ignored Intent requesting to query for $query")
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
            else -> Log_OC.e(TAG, "Unexpected intent $intent")
        }
    }

    public override fun onStop() {
        super.onStop()
    }

    override fun refreshAllShares() {
        refreshCapabilities()
        refreshPrivateShares()
        refreshPublicShares()
    }

    override fun refreshCapabilities(shouldFetchFromNetwork: Boolean) {
        ocCapabilityViewModel.getCapabilityForAccountAsLiveData(shouldFetchFromNetwork).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        if (publicShareFragment != null) {
                            publicShareFragment?.updateCapabilities(resource.data)
                        } else {
                            shareFileFragment?.updateCapabilities(resource.data)
                        }
                        dismissLoadingDialog()
                    }
                    Status.ERROR -> {
                        val errorMessage = ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.GET_CAPABILITIES,
                            resources
                        )
                        if (publicShareFragment != null) {
                            publicShareFragment?.showError(errorMessage)
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_SHORT)
                                .show()
                            shareFileFragment?.updateCapabilities(resource.data)
                        }
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                        if (publicShareFragment != null) {
                            publicShareFragment?.updateCapabilities(resource.data)
                        } else {
                            shareFileFragment?.updateCapabilities(resource.data)
                        }
                    }
                    else -> {
                        Log.d(TAG, "Unknown status when loading capabilities in account ${account?.name}")
                    }
                }
            }
        )
    }

    /**************************************************************************************************************
     *********************************************** PRIVATE SHARES ***********************************************
     **************************************************************************************************************/

    override fun refreshPrivateShares() {
        ocShareViewModel.getPrivateShares(file?.remotePath!!).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        if (shareFileFragment != null && shareFileFragment!!.isAdded) {
                            shareFileFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                        if (searchShareesFragment != null && searchShareesFragment!!.isAdded) {
                            searchShareesFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                        dismissLoadingDialog()
                        if (resource.data.isNullOrEmpty()) {
                            updateFileSharedWithSharee(false)
                        }
                    }
                    Status.ERROR -> {
                        val errorMessage = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.GET_SHARES,
                            resources
                        )
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            errorMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        if (shareFileFragment != null && shareFileFragment!!.isAdded) {
                            shareFileFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                        if (searchShareesFragment != null && searchShareesFragment!!.isAdded) {
                            searchShareesFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                        if (shareFileFragment != null && shareFileFragment!!.isAdded) {
                            shareFileFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                        if (searchShareesFragment != null && searchShareesFragment!!.isAdded) {
                            searchShareesFragment?.updatePrivateShares(resource.data as ArrayList<OCShare>)
                        }
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when loading private shares for file ${file?.fileName} in " +
                                    "account ${account?.name}"
                        )
                    }
                }
            }
        )
    }

    override fun refreshPrivateShare(remoteId: Long) {
        ocShareViewModel.getPrivateShare(remoteId).observe(
            this,
            Observer { updatedShare ->
                editPrivateShareFragment?.updateShare(updatedShare)
            }
        )
    }

    override fun showSearchUsersAndGroups() {
        val searchFragment = SearchShareesFragment.newInstance(file, account)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.share_fragment_container, searchFragment,
            TAG_SEARCH_FRAGMENT
        )
        ft.addToBackStack(null)    // BACK button will recover the ShareFragment
        ft.commit()
    }

    private fun createPrivateShare(shareeName: String, dataAuthority: String?) {
        val shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority)

        ocShareViewModel.insertPrivateShare(
            file.remotePath,
            shareType,
            shareeName,
            getAppropiatePermissions(shareType)
        ).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        updateFileSharedWithSharee(true)
                    }
                    Status.ERROR -> {
                        val errorMessage = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.CREATE_SHARE_WITH_SHAREES,
                            resources
                        )
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            errorMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when creating private share for file ${file?.fileName} in " +
                                    "account ${account?.name}"
                        )
                    }
                }
            }
        )
    }

    private fun getAppropiatePermissions(shareType: ShareType?): Int {
        // check if the Share is FERERATED
        val isFederated = ShareType.FEDERATED == shareType

        when {
            file.isSharedWithMe -> return RemoteShare.READ_PERMISSION_FLAG    // minimum permissions
            isFederated -> {
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
            }
            else -> return if (file.isFolder)
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            else
                RemoteShare.MAXIMUM_PERMISSIONS_FOR_FILE
        }
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
        newFragment.show(ft, TAG_EDIT_SHARE_FRAGMENT)
    }

    override fun updatePrivateShare(remoteId: Long, permissions: Int) {
        ocShareViewModel.updatePrivateShare(
            remoteId,
            permissions
        ).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        updateFileSharedWithSharee(true)
                    }
                    Status.ERROR -> {
                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.UPDATE_SHARE,
                            resources
                        )
                        editPrivateShareFragment?.refreshUiFromState()
                        editPrivateShareFragment?.showError(errorMessage)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(TAG, "Unknown status when updating private share with remote id $remoteId")
                    }
                }
            }
        )
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
        fileOperationsHelper.copyOrSendPrivateLink(file)
    }

    private fun updateFileSharedWithSharee(isSharedWithSharee: Boolean) {
        storageManager.getFileByPath(file.remotePath)?.let { file ->
            file.isSharedWithSharee = isSharedWithSharee
            storageManager.saveFile(file)
        }
    }

    /**************************************************************************************************************
     *********************************************** PUBLIC SHARES ************************************************
     **************************************************************************************************************/

    private fun refreshPublicShares() {
        ocShareViewModel.getPublicShares(file?.remotePath!!).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        shareFileFragment?.updatePublicShares(resource.data as ArrayList<OCShare>)
                        if (resource.data.isNullOrEmpty()) {
                            updatePublicShareFile(false)
                        }
                        dismissLoadingDialog()
                    }
                    Status.ERROR -> {
                        val errorMessage = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.GET_SHARES,
                            resources
                        )
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            errorMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        shareFileFragment?.updatePublicShares(resource.data as ArrayList<OCShare>)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                        shareFileFragment?.updatePublicShares(resource.data as ArrayList<OCShare>)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when loading shares for file ${file?.fileName} in account" +
                                    "${account?.name}"
                        )
                    }
                }
            }
        )
    }

    override fun showAddPublicShare(defaultLinkName: String) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.

        // Create and show the dialog
        val createPublicShareFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            defaultLinkName
        )

        showDialogFragment(
            createPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun createPublicShare(
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ) {
        ocShareViewModel.insertPublicShare(
            file.remotePath,
            permissions,
            name,
            password,
            expirationTimeInMillis,
            publicUpload
        ).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        publicShareFragment?.dismiss()
                        updatePublicShareFile(true)
                        Log_OC.d("TESTS", "Closing share creation dialog")
                    }
                    Status.ERROR -> {
                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.CREATE_PUBLIC_SHARE,
                            resources
                        )
                        publicShareFragment?.showError(errorMessage)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when creating public share with name $name \" +" +
                                    "from account ${account?.name}"
                        )
                    }
                }
            }
        )
    }

    override fun showEditPublicShare(share: OCShare) {
        // Create and show the dialog.
        val editPublicShareFragment = PublicShareDialogFragment.newInstanceToUpdate(file, share)
        showDialogFragment(
            editPublicShareFragment,
            TAG_PUBLIC_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ) {
        ocShareViewModel.updatePublicShare(
            remoteId,
            name,
            password,
            expirationDateInMillis,
            permissions,
            publicUpload
        ).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        updatePublicShareFile(true)
                        publicShareFragment?.dismiss()
                    }
                    Status.ERROR -> {
                        val errorMessage: String = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.UPDATE_SHARE,
                            resources
                        )
                        publicShareFragment?.showError(errorMessage)
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when updating public share with name $name " +
                                    "from account ${account?.name}"
                        )
                    }
                }
            }
        )
    }

    override fun showRemovePublicShare(share: OCShare) {
        val removePublicShareFragment = RemoveShareDialogFragment.newInstance(share)
        showDialogFragment(
            removePublicShareFragment,
            TAG_REMOVE_SHARE_DIALOG_FRAGMENT
        )
    }

    override fun copyOrSendPublicLink(share: OCShare) {
        fileOperationsHelper.copyOrSendPublicLink(share)
    }

    private fun updatePublicShareFile(isSharedViaLink: Boolean) {
        storageManager.getFileByPath(file.remotePath)?.let { file ->
            file.isSharedViaLink = isSharedViaLink
            storageManager.saveFile(file)
        }
    }

    /**************************************************************************************************************
     *************************************************** COMMON ***************************************************
     **************************************************************************************************************/

    override fun removeShare(shareRemoteId: Long) {
        ocShareViewModel.deleteShare(shareRemoteId).observe(
            this,
            Observer { resource ->
                when (resource?.status) {
                    Status.SUCCESS -> {
                        dismissLoadingDialog()
                    }
                    Status.ERROR -> {
                        val errorMessage = resource.msg ?: ErrorMessageAdapter.getResultMessage(
                            resource.code,
                            resource.exception,
                            OperationType.REMOVE_SHARE,
                            resources
                        )
                        Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_SHORT).show()
                        dismissLoadingDialog()
                    }
                    Status.LOADING -> {
                        showLoadingDialog(R.string.common_loading)
                    }
                    else -> {
                        Log.d(
                            TAG, "Unknown status when removing share with id $shareRemoteId " +
                                    "from account ${account?.name}"
                        )
                    }
                }
            }
        )
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

        if (operation is RemoveShareOperation && result.isSuccess && editPrivateShareFragment != null) {
            supportFragmentManager.popBackStack()
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

        const val TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT"
        const val TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT"
        const val TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT"
        const val TAG_PUBLIC_SHARE_DIALOG_FRAGMENT = "PUBLIC_SHARE_DIALOG_FRAGMENT"
        const val TAG_REMOVE_SHARE_DIALOG_FRAGMENT = "REMOVE_SHARE_DIALOG_FRAGMENT"
    }
}
