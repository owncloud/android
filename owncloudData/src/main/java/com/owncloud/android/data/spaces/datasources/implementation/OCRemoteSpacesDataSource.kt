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

import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.spaces.datasources.RemoteSpacesDataSource
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceDeleted
import com.owncloud.android.domain.spaces.model.SpaceFile
import com.owncloud.android.domain.spaces.model.SpaceGrantedTo
import com.owncloud.android.domain.spaces.model.SpaceOwner
import com.owncloud.android.domain.spaces.model.SpacePermission
import com.owncloud.android.domain.spaces.model.SpaceQuota
import com.owncloud.android.domain.spaces.model.SpaceRoot
import com.owncloud.android.domain.spaces.model.SpaceSpecial
import com.owncloud.android.domain.spaces.model.SpaceSpecialFolder
import com.owncloud.android.domain.spaces.model.SpaceUser
import com.owncloud.android.lib.resources.spaces.responses.SpaceResponse
import com.owncloud.android.lib.resources.spaces.services.SpacesService

class OCRemoteSpacesDataSource(
    private val spacesService: SpacesService
) : RemoteSpacesDataSource {
    override fun refreshSpacesForAccount(accountName: String): List<OCSpace> {
        val spacesResponse = executeRemoteOperation {
            spacesService.getSpaces()
        }

        return spacesResponse.map { it.toModel(accountName) }
    }

    private fun SpaceResponse.toModel(accountName: String): OCSpace =
        OCSpace(
            accountName = accountName,
            driveAlias = driveAlias,
            driveType = driveType,
            id = id,
            lastModifiedDateTime = lastModifiedDateTime,
            name = name,
            owner = SpaceOwner(
                user = SpaceUser(
                    id = owner.user.id
                )
            ),
            quota = quota?.let { quotaResponse ->
                SpaceQuota(
                    remaining = quotaResponse.remaining,
                    state = quotaResponse.state,
                    total = quotaResponse.total,
                    used = quotaResponse.used,
                )
            },
            root = SpaceRoot(
                eTag = root.eTag,
                id = root.id,
                permissions = root.permissions?.map { permissionsResponse ->
                    SpacePermission(
                        grantedTo = permissionsResponse.grantedTo.map { grantedToResponse ->
                            SpaceGrantedTo(SpaceUser(grantedToResponse.user.id))
                        },
                        roles = permissionsResponse.roles,
                    )
                },
                webDavUrl = root.webDavUrl,
                deleted = root.deleted?.let { SpaceDeleted(state = it.state) }
            ),
            webUrl = webUrl,
            description = description,
            special = special?.map { specialResponse ->
                SpaceSpecial(
                    eTag = specialResponse.eTag,
                    file = SpaceFile(mimeType = specialResponse.file.mimeType),
                    id = specialResponse.id,
                    lastModifiedDateTime = specialResponse.lastModifiedDateTime,
                    name = specialResponse.name,
                    size = specialResponse.size,
                    specialFolder = SpaceSpecialFolder(name = specialResponse.specialFolder.name),
                    webDavUrl = specialResponse.webDavUrl
                )
            }
        )
}
