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

package com.owncloud.android.domain.sharing.shares.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable

@SuppressLint("ParcelCreator")
data class OCShare(
    val id: Int? = null,
    val fileSource: String,
    val itemSource: String,
    val shareType: ShareType,
    val shareWith: String?,
    val path: String,
    val permissions: Int,
    val sharedDate: Long,
    val expirationDate: Long,
    val token: String?,
    val sharedWithDisplayName: String?,
    val sharedWithAdditionalInfo: String?,
    val isFolder: Boolean,
    val userId: Long,
    val remoteId: Long,
    var accountOwner: String = "",
    val name: String?,
    val shareLink: String?
) : Parcelable {

    val isPasswordProtected: Boolean
        get() = shareType == ShareType.PUBLIC_LINK && shareWith?.isNotEmpty()!!

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id!!)
        dest?.writeString(shareWith)
        dest?.writeString(path)
        dest?.writeString(token)
        dest?.writeString(sharedWithDisplayName)
        dest?.writeString(sharedWithAdditionalInfo)
        dest?.writeString(name)
        dest?.writeString(shareLink)
        dest?.writeString(fileSource)
        dest?.writeString(itemSource)
        dest?.writeInt(shareType.value)
        dest?.writeInt(permissions)
        dest?.writeLong(sharedDate)
        dest?.writeLong(expirationDate)
        dest?.writeInt(if (isFolder) 1 else 0)
        dest?.writeLong(userId)
    }

    override fun describeContents(): Int = this.hashCode()
}

/**
 * Enum for Share Type, with values:
 * -1 - Unknown
 * 0 - Shared by user
 * 1 - Shared by group
 * 3 - Shared by public link
 * 4 - Shared by e-mail
 * 5 - Shared by contact
 * 6 - Federated
 *
 * @author masensio
 */

enum class ShareType constructor(val value: Int) {
    UNKNOWN(-1),
    USER(0),
    GROUP(1),
    PUBLIC_LINK(3),
    EMAIL(4),
    CONTACT(5),
    FEDERATED(6);

    companion object {
        fun fromValue(value: Int): ShareType? {
            return when (value) {
                -1 -> UNKNOWN
                0 -> USER
                1 -> GROUP
                3 -> PUBLIC_LINK
                4 -> EMAIL
                5 -> CONTACT
                6 -> FEDERATED
                else -> null
            }
        }
    }
}
