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


package com.owncloud.android.testing

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.ui.fragment.ShareFragmentListener

class TestShareFileActivity : SingleFragmentActivity(), ShareFragmentListener {
    override fun copyOrSendPrivateLink(file: OCFile?) {
    }

    override fun showSearchUsersAndGroups() {
    }

    override fun showEditPrivateShare(share: OCShare?) {
    }

    override fun refreshSharesFromServer() {
    }

    override fun removeShare(share: OCShare?) {
    }

    override fun showAddPublicShare(defaultLinkName: String?) {
    }

    override fun showEditPublicShare(share: OCShare?) {
    }

    override fun copyOrSendPublicLink(share: OCShare?) {
    }

    override fun getStorageManager(): FileDataStorageManager? {
        return null;
    }
}
