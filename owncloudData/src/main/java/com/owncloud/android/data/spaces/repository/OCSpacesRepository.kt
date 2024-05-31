/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.spaces.repository

import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.data.spaces.datasources.RemoteSpacesDataSource
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.domain.spaces.SpacesRepository
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.user.model.UserQuota

class OCSpacesRepository(
    private val localSpacesDataSource: LocalSpacesDataSource,
    private val localUserDataSource: LocalUserDataSource,
    private val remoteSpacesDataSource: RemoteSpacesDataSource,
) : SpacesRepository {
    override fun refreshSpacesForAccount(accountName: String) {
        remoteSpacesDataSource.refreshSpacesForAccount(accountName).also { listOfSpaces ->
            localSpacesDataSource.saveSpacesForAccount(listOfSpaces)
            val personalSpace = listOfSpaces.find { it.isPersonal }
            personalSpace?.let {
                lateinit var userQuota: UserQuota
                if (it.quota?.total!!.toInt() == 0) {
                    userQuota = UserQuota(accountName, -3, it.quota?.used!!)
                } else {
                    userQuota = UserQuota(accountName, it.quota?.remaining!!, it.quota?.used!!)
                }
                localUserDataSource.saveQuotaForAccount(accountName, userQuota)
            }

        }
    }

    override fun getSpacesFromEveryAccountAsStream() =
        localSpacesDataSource.getSpacesFromEveryAccountAsStream()

    override fun getSpacesByDriveTypeWithSpecialsForAccountAsFlow(accountName: String, filterDriveTypes: Set<String>) =
        localSpacesDataSource.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(accountName = accountName, filterDriveTypes = filterDriveTypes)

    override fun getPersonalSpaceForAccount(accountName: String) =
        localSpacesDataSource.getPersonalSpaceForAccount(accountName)

    override fun getPersonalAndProjectSpacesForAccount(accountName: String) =
        localSpacesDataSource.getPersonalAndProjectSpacesForAccount(accountName)

    override fun getSpaceWithSpecialsByIdForAccount(spaceId: String?, accountName: String) =
        localSpacesDataSource.getSpaceWithSpecialsByIdForAccount(spaceId, accountName)

    override fun getSpaceByIdForAccount(spaceId: String?, accountName: String): OCSpace? =
        localSpacesDataSource.getSpaceByIdForAccount(spaceId = spaceId, accountName = accountName)

    override fun getWebDavUrlForSpace(accountName: String, spaceId: String?): String? =
        localSpacesDataSource.getWebDavUrlForSpace(accountName = accountName, spaceId = spaceId)

}
