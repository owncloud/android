/* ownCloud Android Library is available under MIT license
 *   @author Fernando Sanz Velasco
 *   Copyright (C) 2021 ownCloud GmbH
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares.responses

import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.DEFAULT_PERMISSION
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_EXPIRATION_DATE_IN_MILLIS
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_SHARED_DATE
import com.owncloud.android.lib.resources.shares.ShareType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class ShareItem(
    val id: String? = null,

    @Json(name = "share_with")
    val shareWith: String? = null,

    val path: String? = null,
    val token: String? = null,

    @Json(name = "item_type")
    val itemType: String? = null,

    @Json(name = "share_with_displayname")
    val sharedWithDisplayName: String? = null,

    @Json(name = "share_with_additional_info")
    val sharedWithAdditionalInfo: String? = null,

    val name: String? = null,

    @Json(name = "url")
    val shareLink: String? = null,

    @Json(name = "share_type")
    val shareType: Int? = null,

    val permissions: Int? = null,

    @Json(name = "stime")
    val sharedDate: Long? = null,

    @Json(name = "expiration")
    val expirationDate: String? = null,
) {
    fun toRemoteShare() = RemoteShare(
        id = id ?: "0",
        shareWith = shareWith.orEmpty(),
        path = if (itemType == ItemType.FOLDER.fileValue) path.plus(File.separator) else path.orEmpty(),
        token = token.orEmpty(),
        itemType = itemType.orEmpty(),
        sharedWithDisplayName = sharedWithDisplayName.orEmpty(),
        sharedWithAdditionalInfo = sharedWithAdditionalInfo.orEmpty(),
        name = name.orEmpty(),
        shareLink = shareLink.orEmpty(),
        shareType = ShareType.values().firstOrNull { it.value == shareType } ?: ShareType.UNKNOWN,
        permissions = permissions ?: DEFAULT_PERMISSION,
        sharedDate = sharedDate ?: INIT_SHARED_DATE,
        expirationDate = expirationDate?.let {
            WebdavUtils.parseResponseDate(it)?.time
        } ?: INIT_EXPIRATION_DATE_IN_MILLIS,
        isFolder = itemType?.equals(ItemType.FOLDER.fileValue) ?: false
    )
}

enum class ItemType(val fileValue: String) { FILE("file"), FOLDER("folder") }
