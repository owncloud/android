/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
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

package com.owncloud.android.domain.spaces.model

data class OCSpace(
    val accountName: String,
    val driveAlias: String,
    val driveType: String,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val owner: SpaceOwner?,
    val quota: SpaceQuota?,
    val root: SpaceRoot,
    val webUrl: String,
    val description: String?,
    val special: List<SpaceSpecial>?,
) {
    val isPersonal get() = driveType == DRIVE_TYPE_PERSONAL
    val isProject get() = driveType == DRIVE_TYPE_PROJECT

    val isDisabled get() = root.deleted?.state == DRIVE_DISABLED

    fun getSpaceSpecialImage(): SpaceSpecial? {
        val imageSpecial = special?.filter {
            it.specialFolder.name == "image"
        }
        return imageSpecial?.firstOrNull()
    }

    companion object {
        private const val DRIVE_TYPE_PERSONAL = "personal"
        private const val DRIVE_TYPE_PROJECT = "project"
        private const val DRIVE_DISABLED = "trashed"
    }
}

data class SpaceOwner(
    val user: SpaceUser
)

data class SpaceQuota(
    val remaining: Long?,
    val state: String?,
    val total: Long,
    val used: Long?,
)

data class SpaceRoot(
    val eTag: String?,
    val id: String,
    val permissions: List<SpacePermission>?,
    val webDavUrl: String,
    val deleted: SpaceDeleted?,
)

data class SpaceSpecial(
    val eTag: String,
    val file: SpaceFile,
    val id: String,
    val lastModifiedDateTime: String,
    val name: String,
    val size: Int,
    val specialFolder: SpaceSpecialFolder,
    val webDavUrl: String
)

data class SpaceDeleted(
    val state: String,
)

data class SpaceUser(
    val id: String
)

data class SpaceFile(
    val mimeType: String
)

data class SpacePermission(
    val grantedToIdentities: List<SpaceGrantedToIdentities>,
    val roles: List<String>
)

data class SpaceGrantedToIdentities(
    val user: SpacePermissionIdentity?,
    val group: SpacePermissionIdentity?,
)

data class SpacePermissionIdentity(
    val id: String,
    val displayName: String?,
)

data class SpaceSpecialFolder(
    val name: String
)