/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 *
 *
 *
 */

package com.owncloud.android.lib.resources.shares.responses

import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.DEFAULT_PERMISSION
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_EXPIRATION_DATE_IN_MILLIS
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_SHARED_DATE
import com.owncloud.android.lib.resources.shares.ShareType
import com.squareup.moshi.JsonClass
import java.io.File


@JsonClass(generateAdapter = true)
data class ShareResponse(
    val shares: List<ShareItem>
) {
    fun toRemoteShare() = this.shares.map { shareItem ->
        RemoteShare(
            id = shareItem.id ?: "0",
            shareWith = shareItem.shareWith.orEmpty(),
            path = shareItem.path.orEmpty(),
            token = shareItem.token.orEmpty(),
            sharedWithDisplayName = shareItem.sharedWithDisplayName.orEmpty(),
            sharedWithAdditionalInfo = shareItem.sharedWithAdditionalInfo.orEmpty(),
            name = shareItem.name.orEmpty(),
            shareLink = shareItem.shareLink.orEmpty(),
            shareType = ShareType.values().firstOrNull { it.value == shareItem.shareType } ?: ShareType.UNKNOWN,
            permissions = shareItem.permissions ?: DEFAULT_PERMISSION,
            sharedDate = shareItem.sharedDate ?: INIT_SHARED_DATE,
            expirationDate = shareItem.expirationDate ?: INIT_EXPIRATION_DATE_IN_MILLIS,
            isFolder = shareItem.path?.endsWith(File.separator) ?: false
        )
    }
}

@JsonClass(generateAdapter = true)
data class ShareItem(
    val id: String? = null,
    val shareWith: String? = null,
    val path: String? = null,
    val token: String? = null,
    val sharedWithDisplayName: String? = null,
    val sharedWithAdditionalInfo: String? = null,
    val name: String? = null,
    val shareLink: String? = null,
    val shareType: Int? = null,
    val permissions: Int? = null,
    val sharedDate: Long? = null,
    val expirationDate: Long? = null,
)
