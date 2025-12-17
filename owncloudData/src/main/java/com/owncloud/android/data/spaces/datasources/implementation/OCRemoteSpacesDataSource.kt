/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.spaces.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.spaces.datasources.RemoteSpacesDataSource
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceDeleted
import com.owncloud.android.domain.spaces.model.SpaceFile
import com.owncloud.android.domain.spaces.model.SpaceMember
import com.owncloud.android.domain.spaces.model.SpaceMembers
import com.owncloud.android.domain.spaces.model.SpaceOwner
import com.owncloud.android.domain.spaces.model.SpaceQuota
import com.owncloud.android.domain.spaces.model.SpaceRoot
import com.owncloud.android.domain.spaces.model.SpaceSpecial
import com.owncloud.android.domain.spaces.model.SpaceSpecialFolder
import com.owncloud.android.domain.spaces.model.SpaceUser
import com.owncloud.android.lib.resources.spaces.responses.RootResponse
import com.owncloud.android.lib.resources.spaces.responses.SpacePermissionsResponse
import com.owncloud.android.lib.resources.spaces.responses.SpaceResponse

class OCRemoteSpacesDataSource(
    private val clientManager: ClientManager,
) : RemoteSpacesDataSource {
    override fun refreshSpacesForAccount(accountName: String, userId: String, userGroups: List<String>): List<OCSpace> {
        val spacesResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).getSpaces()
        }

        return spacesResponse.map { it.toModel(accountName, userId, userGroups) }
    }

    override fun createSpace(accountName: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long): OCSpace {
        val spaceResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).createSpace(spaceName, spaceSubtitle, spaceQuota)
        }
        return spaceResponse.toModel(accountName)
    }

    override fun getSpaceMembers(accountName: String, spaceId: String): SpaceMembers {
        val spacePermissionsResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).getSpacePermissions(spaceId)
        }
        return spacePermissionsResponse.toModel()
    }

    override fun getSpacePermissions(accountName: String, spaceId: String): List<String> {
        val spacePermissionsResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).getSpacePermissions(spaceId)
        }
        return spacePermissionsResponse.actions
    }

    override fun editSpace(accountName: String, spaceId: String, spaceName: String, spaceSubtitle: String, spaceQuota: Long?): OCSpace {
        val spaceResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).editSpace(spaceId, spaceName, spaceSubtitle, spaceQuota)
        }
        return spaceResponse.toModel(accountName)
    }

    override fun editSpaceImage(accountName: String, spaceId: String, imageId: String): OCSpace {
        val spaceResponse = executeRemoteOperation {
            clientManager.getSpacesService(accountName).editSpaceImage(spaceId, imageId)
        }
        return spaceResponse.toModel(accountName)
    }

    override fun disableSpace(accountName: String, spaceId: String, deleteMode: Boolean) {
        executeRemoteOperation { clientManager.getSpacesService(accountName).disableSpace(spaceId, deleteMode) }
    }

    override fun enableSpace(accountName: String, spaceId: String) {
        executeRemoteOperation { clientManager.getSpacesService(accountName).enableSpace(spaceId) }
    }

    companion object {
        private const val MANAGER_ROLE = "manager"
        private const val EDITOR_ROLE = "editor"
        private const val VIEWER_ROLE = "viewer"

        @VisibleForTesting
        fun SpaceResponse.toModel(accountName: String): OCSpace =
            OCSpace(
                accountName = accountName,
                driveAlias = driveAlias,
                driveType = driveType,
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                owner = owner?.let { ownerResponse ->
                    SpaceOwner(
                        user = SpaceUser(
                            id = ownerResponse.user.id
                        )
                    )
                },
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
                    webDavUrl = root.webDavUrl,
                    deleted = root.deleted?.let { SpaceDeleted(state = it.state) },
                    role = null
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

        @VisibleForTesting
        fun SpaceResponse.toModel(accountName: String, userId: String, userGroups: List<String>): OCSpace =
            OCSpace(
                accountName = accountName,
                driveAlias = driveAlias,
                driveType = driveType,
                id = id,
                lastModifiedDateTime = lastModifiedDateTime,
                name = name,
                owner = owner?.let { ownerResponse ->
                    SpaceOwner(
                        user = SpaceUser(
                            id = ownerResponse.user.id
                        )
                    )
                },
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
                    webDavUrl = root.webDavUrl,
                    deleted = root.deleted?.let { SpaceDeleted(state = it.state) },
                    role = getRoleForUser(root, userId, userGroups)
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

        @VisibleForTesting
        fun SpacePermissionsResponse.toModel(): SpaceMembers =
            SpaceMembers(
                roles = roles.map { spaceRoleResponse ->
                    OCRole(
                        id = spaceRoleResponse.id,
                        displayName = spaceRoleResponse.displayName
                    )
                },
                members = members.map { spaceMemberResponse ->
                    SpaceMember (
                        id = spaceMemberResponse.id ?: "",
                        expirationDateTime = spaceMemberResponse.expirationDateTime,
                        displayName = spaceMemberResponse.grantedToV2.user?.displayName ?: spaceMemberResponse.grantedToV2.group?.displayName ?: "",
                        roles = spaceMemberResponse.roles
                    )
                }
            )


        private fun getRoleForUser(root: RootResponse, userId: String, userGroups: List<String>): String? {
            val priorityOrder = listOf(MANAGER_ROLE, EDITOR_ROLE, VIEWER_ROLE)
            val matchingPermissions = root.permissions.orEmpty().filter { permission ->
                permission.grantedToV2.user?.id == userId || permission.grantedToV2.group?.id in userGroups
            }
            return matchingPermissions.flatMap { it.roles }.minByOrNull { priorityOrder.indexOf(it) }
        }
    }
}
