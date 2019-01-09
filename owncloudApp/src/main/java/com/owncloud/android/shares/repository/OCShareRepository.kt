package com.owncloud.android.shares.repository

import android.arch.lifecycle.LiveData
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.LocalSharesDataSource
import com.owncloud.android.shares.datasources.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare

class OCShareRepository(
private val localSharesDataSource: LocalSharesDataSource,
private val remoteShareSharesDataSource: RemoteSharesDataSource
) : ShareRepository {

    override fun getSharesForFile(filePath: String, accountName: String, shareTypes: List<ShareType>)
            : LiveData<List<OCShare>> {
        return localSharesDataSource.sharesForAFile(
            filePath,
            accountName,
            shareTypes.map { shareType ->  shareType.value}
        )
    }

    override fun fetchSharesForFileFromServer(
        filePath: String,
        accountName: String,
        reshares: Boolean,
        subfiles: Boolean
    ) {
        // Get shares from server
        val remoteShares = remoteShareSharesDataSource.getSharesForAFile(filePath, reshares, subfiles)

        // Save shares into database
        val localShares = remoteShares.map { remoteShare -> OCShare(remoteShare) }
        for (localShare in localShares) {
            localShare.accountOwner = accountName
            localSharesDataSource.insert(localShare)
        }
    }
}
