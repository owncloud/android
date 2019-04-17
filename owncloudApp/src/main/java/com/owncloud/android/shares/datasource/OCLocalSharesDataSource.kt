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

package com.owncloud.android.shares.datasource

import androidx.lifecycle.LiveData
import com.owncloud.android.MainApp
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.db.OCShareDao

class OCLocalSharesDataSource(
    private val ocShareDao: OCShareDao = OwncloudDatabase.getDatabase(MainApp.getAppContext()).shareDao()
) : LocalSharesDataSource {

    override fun getSharesForFileAsLiveData(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>
    ): LiveData<List<OCShare>> =
        ocShareDao.getSharesForFileAsLiveData(filePath, accountName, shareTypes.map { it.value })

    override fun insert(ocShares: List<OCShare>) = ocShareDao.replace(ocShares)

    override fun update(ocShares: List<OCShare>) {
        ocShareDao.update(ocShares)
    }

    override fun delete(filePath: String, accountName: String) = ocShareDao.delete(filePath, accountName)
}
