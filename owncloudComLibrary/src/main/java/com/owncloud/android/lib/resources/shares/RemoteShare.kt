/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares

import com.owncloud.android.lib.resources.shares.responses.ItemType

/**
 * Contains the data of a Share from the Share API
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
data class RemoteShare(
    var id: String = "0",
    var shareWith: String = "",
    var path: String = "",
    var token: String = "",
    var itemType: String = "",
    var sharedWithDisplayName: String = "",
    var sharedWithAdditionalInfo: String = "",
    var name: String = "",
    var shareLink: String = "",
    var shareType: ShareType? = ShareType.UNKNOWN,
    var permissions: Int = DEFAULT_PERMISSION,
    var sharedDate: Long = INIT_SHARED_DATE,
    var expirationDate: Long = INIT_EXPIRATION_DATE_IN_MILLIS,
    var isFolder: Boolean = (itemType == ItemType.FOLDER.fileValue)
) {

    companion object {
        const val DEFAULT_PERMISSION = -1
        const val READ_PERMISSION_FLAG = 1
        const val UPDATE_PERMISSION_FLAG = 2
        const val CREATE_PERMISSION_FLAG = 4
        const val DELETE_PERMISSION_FLAG = 8
        const val SHARE_PERMISSION_FLAG = 16
        const val MAXIMUM_PERMISSIONS_FOR_FILE = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val MAXIMUM_PERMISSIONS_FOR_FOLDER = MAXIMUM_PERMISSIONS_FOR_FILE +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG

        const val INIT_EXPIRATION_DATE_IN_MILLIS: Long = 0
        const val INIT_SHARED_DATE: Long = 0
    }
}

/**
 * // TODO This type is already included in the domain but we still need it here since the parsing takes place in this library for the moment
 *
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
        fun fromValue(value: Int) = values().firstOrNull { it.value == value }
    }
}
