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

package com.owncloud.android.data.sharing.shares.datasources.mapper

import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.resources.shares.RemoteShare
import org.jetbrains.annotations.TestOnly
import com.owncloud.android.lib.resources.shares.ShareType as RemoteShareType

class RemoteShareMapper : RemoteMapper<OCShare, RemoteShare> {
    override fun toModel(remote: RemoteShare?): OCShare? =
        remote?.let {
            OCShare(
                fileSource = remote.fileSource,
                itemSource = remote.itemSource,
                shareType = ShareType.fromValue(remote.shareType!!.value)!!,
                shareWith = remote.shareWith,
                path = remote.path,
                permissions = remote.permissions,
                sharedDate = remote.sharedDate,
                expirationDate = remote.expirationDate,
                token = remote.token,
                sharedWithDisplayName = remote.sharedWithDisplayName,
                sharedWithAdditionalInfo = remote.sharedWithAdditionalInfo,
                isFolder = remote.isFolder,
                userId = remote.userId,
                remoteId = remote.id,
                name = remote.name,
                shareLink = remote.shareLink
            )
        }

    @TestOnly
    override fun toRemote(model: OCShare?): RemoteShare? =
        model?.let {
            RemoteShare(
                id = model.remoteId,
                shareWith = model.shareWith!!,
                path = model.path,
                token = model.token!!,
                sharedWithDisplayName = model.sharedWithDisplayName!!,
                sharedWithAdditionalInfo = model.sharedWithAdditionalInfo!!,
                name = model.name!!,
                shareLink = model.shareLink!!,
                fileSource = model.fileSource,
                itemSource = model.itemSource,
                shareType = RemoteShareType.fromValue(model.shareType.value),
                permissions = model.permissions,
                sharedDate = model.sharedDate,
                expirationDate = model.expirationDate,
                isFolder = model.isFolder,
                userId = model.userId,
                isValid = model.remoteId > -1
            )
        }
}
