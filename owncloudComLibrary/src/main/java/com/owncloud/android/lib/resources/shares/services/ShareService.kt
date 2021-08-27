/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 *
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

package com.owncloud.android.lib.resources.shares.services

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.Service
import com.owncloud.android.lib.resources.shares.ShareResponse
import com.owncloud.android.lib.resources.shares.ShareType

interface ShareService : Service {
    fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean
    ): RemoteOperationResult<ShareResponse>

    fun insertShare(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        publicUpload: Boolean
    ): RemoteOperationResult<ShareResponse>

    fun updateShare(
        remoteId: String,
        name: String,
        password: String?,
        expirationDate: Long,
        permissions: Int,
        publicUpload: Boolean
    ): RemoteOperationResult<ShareResponse>

    fun deleteShare(remoteId: String): RemoteOperationResult<ShareResponse>
}
