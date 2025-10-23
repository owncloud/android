/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.domain.spaces

import com.owncloud.android.domain.spaces.model.OCSpace
import kotlinx.coroutines.flow.Flow

interface SpacesRepository {
    fun refreshSpacesForAccount(accountName: String, userId: String, userGroups: List<String>)
    fun getSpacesFromEveryAccountAsStream(): Flow<List<OCSpace>>
    fun getSpacesByDriveTypeWithSpecialsForAccountAsFlow(accountName: String, filterDriveTypes: Set<String>): Flow<List<OCSpace>>
    fun getPersonalSpaceForAccount(accountName: String): OCSpace?
    fun getPersonalAndProjectSpacesForAccount(accountName: String): List<OCSpace>
    fun getSpaceWithSpecialsByIdForAccount(spaceId: String?, accountName: String): OCSpace
    fun getSpaceByIdForAccount(spaceId: String?, accountName: String): OCSpace?
    fun getSpacePermissions(accountName: String, spaceId: String): List<String>
    fun getWebDavUrlForSpace(accountName: String, spaceId: String?): String?
    fun createSpace(accountName: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long)
    fun editSpace(accountName: String, spaceId: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long?)
}
