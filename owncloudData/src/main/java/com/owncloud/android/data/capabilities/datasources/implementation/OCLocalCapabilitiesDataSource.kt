/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.data.capabilities.datasources.implementation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.OCCapabilityMapper
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.domain.capabilities.model.OCCapability

class OCLocalCapabilitiesDataSource(
    private val ocCapabilityDao: OCCapabilityDao,
    private val ocCapabilityMapper: OCCapabilityMapper
) : LocalCapabilitiesDataSource {

    override fun getCapabilitiesForAccountAsLiveData(accountName: String): LiveData<OCCapability?> =
        Transformations.map(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(accountName)) { ocCapabilityEntity ->
            ocCapabilityMapper.toModel(ocCapabilityEntity)
        }

    override fun getCapabilityForAccount(accountName: String): OCCapability? =
        ocCapabilityMapper.toModel(ocCapabilityDao.getCapabilitiesForAccount(accountName))

    override fun insert(ocCapabilities: List<OCCapability>) {
        ocCapabilityDao.replace(
            ocCapabilities.map { ocCapability -> ocCapabilityMapper.toEntity(ocCapability)!! }
        )
    }
}
