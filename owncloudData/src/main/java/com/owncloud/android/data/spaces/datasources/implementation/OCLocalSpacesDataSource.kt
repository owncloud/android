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

package com.owncloud.android.data.spaces.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.data.spaces.db.SpaceQuotaEntity
import com.owncloud.android.data.spaces.db.SpaceRootEntity
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity
import com.owncloud.android.data.spaces.db.SpacesDao
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.data.spaces.db.SpacesWithSpecials
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.DRIVE_TYPE_PERSONAL
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.DRIVE_TYPE_PROJECT
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.SPACE_ID_SHARES
import com.owncloud.android.domain.spaces.model.SpaceDeleted
import com.owncloud.android.domain.spaces.model.SpaceFile
import com.owncloud.android.domain.spaces.model.SpaceOwner
import com.owncloud.android.domain.spaces.model.SpaceQuota
import com.owncloud.android.domain.spaces.model.SpaceRoot
import com.owncloud.android.domain.spaces.model.SpaceSpecial
import com.owncloud.android.domain.spaces.model.SpaceSpecialFolder
import com.owncloud.android.domain.spaces.model.SpaceUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

        spacesDao.insertOrDeleteSpaces(spaceEntities, spaceSpecialEntities)
    }

    override fun getPersonalSpaceForAccount(accountName: String): OCSpace? {
        return spacesDao.getSpacesByDriveTypeForAccount(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PERSONAL)
        ).map { it.toModel() }.firstOrNull()
    }

    override fun getSharesSpaceForAccount(accountName: String): OCSpace? {
        return spacesDao.getSpaceByIdForAccount(spaceId = SPACE_ID_SHARES, accountName = accountName)?.toModel()
    }

    override fun getSpacesFromEveryAccountAsStream(): Flow<List<OCSpace>> {
        return spacesDao.getSpacesByDriveTypeFromEveryAccountAsStream(
            filterDriveTypes = setOf(DRIVE_TYPE_PERSONAL, DRIVE_TYPE_PROJECT)
        ).map { spaceEntities ->
            spaceEntities.map { spaceEntity -> spaceEntity.toModel() }
        }
    }

    override fun getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
        accountName: String,
        filterDriveTypes: Set<String>,
    ): Flow<List<OCSpace>> {
        return spacesDao.getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
            accountName = accountName,
            filterDriveTypes = filterDriveTypes,
        ).map { spacesWithSpecialsEntitiesList ->
            spacesWithSpecialsEntitiesList.map { spacesWithSpecialsEntity ->
                spacesWithSpecialsEntity.toModel()
            }
        }
    }

    override fun getPersonalAndProjectSpacesForAccount(accountName: String): List<OCSpace> {
        return spacesDao.getSpacesByDriveTypeForAccount(
            accountName = accountName,
            filterDriveTypes = setOf(DRIVE_TYPE_PERSONAL, DRIVE_TYPE_PROJECT),
        ).map { it.toModel() }
    }

    override fun getSpaceWithSpecialsByIdForAccount(spaceId: String?, accountName: String): OCSpace {
        return spacesDao.getSpaceWithSpecialsByIdForAccount(spaceId, accountName).toModel()
    }

    override fun getSpaceByIdForAccount(spaceId: String?, accountName: String): OCSpace? {
        return spacesDao.getSpaceByIdForAccount(spaceId = spaceId, accountName = accountName)?.toModel()
    }

    override fun getWebDavUrlForSpace(spaceId: String?, accountName: String): String? {
        return spacesDao.getWebDavUrlForSpace(spaceId, accountName)
    }

    override fun deleteSpacesForAccount(accountName: String) {
        spacesDao.deleteSpacesForAccount(accountName)
    }

    companion object {
        @VisibleForTesting
        fun SpacesWithSpecials.toModel() =
            space.toModel(specials = specials)

        @VisibleForTesting
        fun SpaceSpecialEntity.toModel() =
            SpaceSpecial(
                eTag = eTag,
                file = SpaceFile(
                    mimeType = fileMimeType
                ),
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                size = size,
                specialFolder = SpaceSpecialFolder(
                    name = specialFolderName
                ),
                webDavUrl = webDavUrl
            )

        @VisibleForTesting
        fun SpacesEntity.toModel(specials: List<SpaceSpecialEntity>? = null) =
            OCSpace(
                accountName = accountName,
                driveAlias = driveAlias,
                driveType = driveType,
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                owner = ownerId?.let { spaceOwnerIdEntity ->
                    SpaceOwner(
                        user = SpaceUser(
                            id = spaceOwnerIdEntity
                        )
                    )
                },
                quota = quota?.let { spaceQuotaEntity ->
                    SpaceQuota(
                        remaining = spaceQuotaEntity.remaining,
                        state = spaceQuotaEntity.state,
                        total = spaceQuotaEntity.total,
                        used = spaceQuotaEntity.used,
                    )
                },
                root = root.let { spaceRootEntity ->
                    SpaceRoot(
                        eTag = spaceRootEntity.eTag,
                        id = spaceRootEntity.id,
                        webDavUrl = spaceRootEntity.webDavUrl,
                        deleted = spaceRootEntity.deleteState?.let { SpaceDeleted(state = it) },
                    )
                },
                webUrl = webUrl,
                description = description,
                special = specials?.map { it.toModel() },
            )

        @VisibleForTesting
        fun OCSpace.toEntity() =
            SpacesEntity(
                accountName = accountName,
                driveAlias = driveAlias,
                driveType = driveType,
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                ownerId = owner?.user?.id,
                quota = quota?.let { quotaModel ->
                    SpaceQuotaEntity(remaining = quotaModel.remaining, state = quotaModel.state, total = quotaModel.total, used = quotaModel.used)
                },
                root = root.let { rootModel ->
                    SpaceRootEntity(eTag = rootModel.eTag, id = rootModel.id, webDavUrl = rootModel.webDavUrl, deleteState = rootModel.deleted?.state)
                },
                webUrl = webUrl,
                description = description,
            )

        @VisibleForTesting
        fun SpaceSpecial.toEntity(accountName: String, spaceId: String): SpaceSpecialEntity =
            SpaceSpecialEntity(
                accountName = accountName,
                spaceId = spaceId,
                eTag = eTag,
                fileMimeType = file.mimeType,
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                size = size,
                specialFolderName = specialFolder.name,
                webDavUrl = webDavUrl
            )
    }
}
