/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.shares.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.owncloud.android.lib.resources.shares.RemoteShare

@Entity(tableName = "shares_table")
data class OCShare(
    @PrimaryKey val id: Long,
    val fileSource: Long,
    val itemSource: Long,
    val shareType: Int,
    val shareWith: String,
    val path: String,
    val permissions: Int,
    val sharedDate: Long,
    val expirationDate: Long,
    val token: String,
    val sharedWithDisplayName: String,
    val name: String,
    val isFolder: Boolean,
    val userId: Long,
    val remoteId: Long,
    val shareLink: String,
    var accountOwner: String
) {
    constructor(remoteShare: RemoteShare) : this(
        remoteShare.id,
        remoteShare.fileSource,
        remoteShare.itemSource,
        remoteShare.shareType.value,
        remoteShare.shareWith,
        remoteShare.path,
        remoteShare.permissions,
        remoteShare.sharedDate,
        remoteShare.expirationDate,
        remoteShare.token,
        remoteShare.sharedWithDisplayName,
        remoteShare.name,
        remoteShare.isFolder,
        remoteShare.userId,
        remoteShare.remoteId,
        remoteShare.shareLink,
        ""
    )

    companion object {
        /**
         * Generated - should be refreshed every time the class changes!!
         */
        private const val serialVersionUID = 4124975224281327921L

        private val TAG = OCShare::class.java.simpleName

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
        const val FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9 = READ_PERMISSION_FLAG + UPDATE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9 =
            FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 + SHARE_PERMISSION_FLAG
    }
}
