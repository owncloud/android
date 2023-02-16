/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
import com.owncloud.android.domain.spaces.SpacesRepository

class OCSpacesRepository(
    private val localSpacesDataSource: LocalSpacesDataSource,
    private val remoteSpacesDataSource: RemoteSpacesDataSource,
) : SpacesRepository {
    override fun refreshSpacesForAccount(accountName: String) {
        remoteSpacesDataSource.refreshSpacesForAccount(accountName).also { listOfSpaces ->
            localSpacesDataSource.saveSpacesForAccount(listOfSpaces)
        }
    }

    override fun getAllSpaces() =
        localSpacesDataSource.getAllSpaces()

    override fun getProjectSpacesWithSpecialsForAccountAsFlow(accountName: String) =
        localSpacesDataSource.getProjectSpacesWithSpecialsForAccountAsFlow(accountName)

    override fun getPersonalAndProjectSpacesWithSpecialsForAccountAsFlow(accountName: String) =
        localSpacesDataSource.getPersonalAndProjectSpacesWithSpecialsForAccountAsFlow(accountName)

    override fun getPersonalAndProjectSpacesForAccount(accountName: String) =
        localSpacesDataSource.getPersonalAndProjectSpacesForAccount(accountName)

    override fun getSpaceWithSpecialsByIdForAccount(spaceId: String?, accountName: String) =
        localSpacesDataSource.getSpaceWithSpecialsByIdForAccount(spaceId, accountName)

    override fun getWebDavUrlForSpace(accountName: String, spaceId: String?): String? =
        localSpacesDataSource.getWebDavUrlForSpace(accountName = accountName, spaceId = spaceId)
}
