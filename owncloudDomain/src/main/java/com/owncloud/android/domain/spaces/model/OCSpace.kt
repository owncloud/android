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

package com.owncloud.android.domain.spaces.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToLong

@Parcelize
data class OCSpace(
    val accountName: String,
    val driveAlias: String?,
    val driveType: String,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val owner: SpaceOwner?,
    val quota: SpaceQuota?,
    val root: SpaceRoot,
    val webUrl: String?,
    val description: String?,
    val special: List<SpaceSpecial>?,
) : Parcelable {
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
        const val DRIVE_TYPE_PERSONAL = "personal"
        const val DRIVE_TYPE_PROJECT = "project"
        private const val DRIVE_DISABLED = "trashed"

        // https://github.com/owncloud/web/blob/2ab7e6da432cb344cd86f3451eea1678461c5c90/packages/web-client/src/helpers/space/types.ts#L11
        const val SPACE_ID_SHARES = "a0ca6a90-a365-4782-871e-d44447bbc668\$a0ca6a90-a365-4782-871e-d44447bbc668"
    }
}

@Parcelize
data class SpaceOwner(
    val user: SpaceUser
) : Parcelable

@Parcelize
data class SpaceQuota(
    val remaining: Long?,
    val state: String?,
    val total: Long,
    val used: Long?,
) : Parcelable {

    fun getRelative(): Double =
        if (used == null) 0.0 else ((used * 100).toDouble() / total * 100).roundToLong() / 100.0

}

@Parcelize
data class SpaceRoot(
    val eTag: String?,
    val id: String,
    val webDavUrl: String,
    val deleted: SpaceDeleted?,
    val role: String?
) : Parcelable

@Parcelize
data class SpaceSpecial(
    val eTag: String,
    val file: SpaceFile,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val size: Int,
    val specialFolder: SpaceSpecialFolder,
    val webDavUrl: String
) : Parcelable

@Parcelize
data class SpaceDeleted(
    val state: String,
) : Parcelable

@Parcelize
data class SpaceUser(
    val id: String
) : Parcelable

@Parcelize
data class SpaceFile(
    val mimeType: String
) : Parcelable

@Parcelize
data class SpaceSpecialFolder(
    val name: String
) : Parcelable
