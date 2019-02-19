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
