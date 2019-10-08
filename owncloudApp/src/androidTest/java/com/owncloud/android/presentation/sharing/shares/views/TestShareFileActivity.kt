/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.presentation.sharing.shares.views

import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.presentation.ui.sharing.fragments.SearchShareesFragment
import com.owncloud.android.presentation.ui.sharing.fragments.PublicShareDialogFragment
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFileFragment
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFragmentListener
import com.owncloud.android.testing.SingleFragmentActivity

class TestShareFileActivity : SingleFragmentActivity(),
    ShareFragmentListener {
    lateinit var capabilities: OCCapabilityEntity
    lateinit var privateShares: ArrayList<OCShareEntity>
    lateinit var publicShares: ArrayList<OCShareEntity>
    lateinit var errorMessage: String

    override fun startObserving() {
        val shareFileFragment: ShareFileFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as ShareFileFragment
        shareFileFragment.updateCapabilities(capabilities)
        shareFileFragment.updatePrivateShares(privateShares)
        shareFileFragment.updatePublicShares(publicShares)
    }

    override fun refreshPrivateShares() {
        val searchShareesFragment: SearchShareesFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as SearchShareesFragment
        searchShareesFragment.updatePrivateShares(privateShares)
    }

    override fun refreshPrivateShare(remoteId: Long) {
        val editPrivateShareFragment: EditPrivateShareFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as EditPrivateShareFragment
        editPrivateShareFragment.updateShare(privateShares[0])
    }

    override fun updatePrivateShare(remoteId: Long, permissions: Int) {
        return
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    override fun showAddPublicShare(defaultLinkName: String) {
    }

    override fun showEditPublicShare(share: OCShare) {
    }

    override fun showRemovePublicShare(share: OCShare) {
    }

    override fun copyOrSendPublicLink(share: OCShare) {
    }

    override fun createPublicShare(
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ) {
        val publicShareDialogFragment: PublicShareDialogFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as PublicShareDialogFragment
        publicShareDialogFragment.showError(errorMessage)
    }

    override fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ) {
    }

    override fun removePublicShare(share: OCShareEntity) {
    }

    override fun observeCapabilities(shouldFetchFromNetwork: Boolean) {
        val publicShareDialogFragment: PublicShareDialogFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as PublicShareDialogFragment
        publicShareDialogFragment.updateCapabilities(capabilities)
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
    }

    override fun showSearchUsersAndGroups() {
    }

    override fun showEditPrivateShare(share: OCShareEntity) {
    }

    override fun refreshAllShares() {
        val shareFileFragment: ShareFileFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as ShareFileFragment
        shareFileFragment.updateCapabilities(capabilities)
        shareFileFragment.updatePrivateShares(privateShares)
        shareFileFragment.updatePublicShares(publicShares)
    }

    override fun deleteShare(shareRemoteId: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun copyOrSendPrivateLink(file: OCFile) {
    }
}
