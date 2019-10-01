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

package com.owncloud.android.lib.resources.shares

import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.domain.sharing.shares.model.OCShare

class RemoteShareMapper : RemoteMapper<OCShare, RemoteShare> {
    override fun toModel(remote: RemoteShare?): OCShare? =
        remote?.let {
            OCShare(
                fileSource = remote.fileSource,
                itemSource = remote.itemSource,
                shareType = remote.shareType!!,
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

    override fun toRemote(model: OCShare?): RemoteShare? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
