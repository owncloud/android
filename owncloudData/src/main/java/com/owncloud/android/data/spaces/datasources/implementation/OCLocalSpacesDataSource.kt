/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.data.spaces.datasources.implementation

import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.data.spaces.db.SpaceQuotaEntity
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity
import com.owncloud.android.data.spaces.db.SpacesDao
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceSpecial

class OCLocalSpacesDataSource(
    private val spacesDao: SpacesDao,
) : LocalSpacesDataSource {

    override fun saveSpacesForAccount(listOfSpaces: List<OCSpace>) {
        val spaceEntities = mutableListOf<SpacesEntity>()
        val spaceSpecialEntities = mutableListOf<SpaceSpecialEntity>()

        listOfSpaces.forEach { spaceModel ->
            spaceEntities.add(spaceModel.toEntity())
            spaceModel.special?.let { listOfSpacesSpecials ->
                spaceSpecialEntities.addAll(listOfSpacesSpecials.map { it.toEntity(spaceModel.accountName, spaceModel.id) })
            }
        }

        spacesDao.upsertOrDeleteSpaces(spaceEntities, spaceSpecialEntities)
    }

    private fun OCSpace.toEntity() =
        SpacesEntity(
            accountName = accountName,
            driveAlias = driveAlias,
            driveType = driveType,
            id = id,
            lastModifiedDateTime = lastModifiedDateTime,
            name = name,
            ownerId = owner.user.id,
            quota = quota?.let { quotaModel ->
                SpaceQuotaEntity(remaining = quotaModel.remaining, state = quotaModel.state, total = quotaModel.total, used = quotaModel.used)
            },
            rootETag = root.eTag,
            rootId = root.id,
            rootWebDavUrl = root.webDavUrl,
            webUrl = webUrl,
            description = description,
        )

    private fun SpaceSpecial.toEntity(accountName: String, spaceId: String): SpaceSpecialEntity =
        SpaceSpecialEntity(
            accountName = accountName,
            spaceId = spaceId,
            eTag = eTag,
            fileMymeType = file.mimeType,
            id = id,
            lastModifiedDateTime = lastModifiedDateTime,
            name = name,
            size = size,
            specialFolderName = specialFolder.name,
            webDavUrl = webDavUrl
        )
}
