/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.sharing.shares.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFragmentListener
import com.owncloud.android.testing.SingleFragmentActivity

class TestShareFileActivity : SingleFragmentActivity(), ShareFragmentListener {
    fun startFragment(fragment: Fragment) {
        supportFragmentManager.transaction(allowStateLoss = true) {
            add(R.id.container, fragment, TEST_FRAGMENT_TAG)
        }
    }

    fun getTestFragment(): Fragment? = supportFragmentManager.findFragmentByTag(TEST_FRAGMENT_TAG)

    override fun copyOrSendPrivateLink(file: OCFile) {
    }

    override fun deleteShare(remoteId: String) {
    }

    override fun showLoading() {
    }

    override fun dismissLoading() {
    }

    override fun showAddPublicShare(defaultLinkName: String) {
    }

    override fun showEditPublicShare(share: OCShare) {
    }

    override fun showRemoveShare(share: OCShare) {
    }

    override fun copyOrSendPublicLink(share: OCShare) {
    }

    override fun showSearchUsersAndGroups() {
    }

    override fun showEditPrivateShare(share: OCShare) {
    }

    companion object {
        private const val TEST_FRAGMENT_TAG = "TEST FRAGMENT"
    }
}
