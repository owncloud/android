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

package com.owncloud.android.capabilities.datasource

import androidx.lifecycle.LiveData
import com.owncloud.android.MainApp
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.db.OCCapabilityDao
import com.owncloud.android.db.OwncloudDatabase

class OCLocalCapabilitiesDataSource(
    private val ocCapabilityDao: OCCapabilityDao = OwncloudDatabase.getDatabase(MainApp.appContext!!).capabilityDao()
) : LocalCapabilitiesDataSource {

    override fun getCapabilityForAccountAsLiveData(accountName: String): LiveData<OCCapability> =
        ocCapabilityDao.getCapabilityForAccount(accountName)

    override fun insert(ocCapabilities: List<OCCapability>) {
        ocCapabilityDao.replace(ocCapabilities)
    }
}
