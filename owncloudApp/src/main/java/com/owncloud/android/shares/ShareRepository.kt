/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.shares

import android.arch.lifecycle.LiveData
import com.owncloud.android.shares.datasources.LocalSharesDataSource
import com.owncloud.android.shares.datasources.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare

interface ShareRepository {
    fun getSharesForFile(
        filePath: String,
        accountName: String,
        shareTypes: List<Int>
    ): LiveData<List<OCShare>>

    fun fetchSharesForFileFromServer(path: String, reshares: Boolean, subfiles: Boolean)
}

class ShareRepositoryImpl(
    private val localSharesDataSource: LocalSharesDataSource,
    private val remoteShareSharesDataSource: RemoteSharesDataSource
) : ShareRepository {

    override fun getSharesForFile(filePath: String, accountName: String, shareTypes: List<Int>)
            : LiveData<List<OCShare>> {
        return localSharesDataSource.sharesForAFile(filePath, accountName, shareTypes)
    }

    override fun fetchSharesForFileFromServer(path: String, reshares: Boolean, subfiles: Boolean) {
        val remoteShares = remoteShareSharesDataSource.getSharesForAFile(path, reshares, subfiles)

        for (remoteShare in remoteShares) {
            localSharesDataSource.insert(OCShare(remoteShare))
        }
    }
}