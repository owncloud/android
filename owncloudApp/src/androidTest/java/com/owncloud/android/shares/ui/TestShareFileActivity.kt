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

package com.owncloud.android.shares.ui

import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.PublicShareDialogFragment
import com.owncloud.android.shares.ui.fragment.ShareFileFragment
import com.owncloud.android.shares.ui.fragment.ShareFragmentListener
import com.owncloud.android.testing.SingleFragmentActivity

class TestShareFileActivity : SingleFragmentActivity(), ShareFragmentListener {
    lateinit var capabilities: OCCapability
    lateinit var publicShares: ArrayList<OCShare>
    lateinit var errorMessage: String

    override fun refreshShares() {
        val shareFileFragment: ShareFileFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as ShareFileFragment
        shareFileFragment.updateCapabilities(capabilities)
        shareFileFragment.updatePublicShares(publicShares)
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

    override fun removePublicShare(share: OCShare) {
    }

    override fun refreshCapabilities(shouldFetchFromNetwork: Boolean) {
        val publicShareDialogFragment: PublicShareDialogFragment =
            supportFragmentManager.findFragmentByTag("TEST FRAGMENT") as PublicShareDialogFragment
        publicShareDialogFragment.updateCapabilities(capabilities)
    }

    override fun copyOrSendPrivateLink(file: OCFile) {
    }

    override fun showSearchUsersAndGroups() {
    }

    override fun showEditPrivateShare(share: OCShare) {
    }

    override fun refreshSharesFromServer() {
    }

    override fun showAddPublicShare(defaultLinkName: String) {
    }

    override fun showEditPublicShare(share: OCShare) {
    }

    override fun showRemovePublicShare(share: OCShare) {
    }

    override fun copyOrSendPublicLink(share: OCShare) {
    }
}
