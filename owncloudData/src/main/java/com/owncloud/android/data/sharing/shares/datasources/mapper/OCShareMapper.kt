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

import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.mappers.Mapper
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType

class OCShareMapper : Mapper<OCShare, OCShareEntity> {
    override fun toModel(entity: OCShareEntity?): OCShare? =
        entity?.let {
            OCShare(
                id = entity.id,
                fileSource = entity.fileSource,
                itemSource = entity.fileSource,
                shareType = ShareType.fromValue(entity.shareType)!!,
                shareWith = entity.shareWith,
                path = entity.path,
                permissions = entity.permissions,
                sharedDate = entity.sharedDate,
                expirationDate = entity.expirationDate,
                token = entity.token,
                sharedWithDisplayName = entity.sharedWithDisplayName,
                sharedWithAdditionalInfo = entity.sharedWithAdditionalInfo,
                isFolder = entity.isFolder,
                userId = entity.userId,
                remoteId = entity.remoteId,
                accountOwner = entity.accountOwner,
                name = entity.name,
                shareLink = entity.shareLink
            )
        }

    override fun toEntity(model: OCShare?): OCShareEntity? =
        model?.let {
            OCShareEntity(
                fileSource = model.fileSource,
                itemSource = model.fileSource,
                shareType = model.shareType.value,
                shareWith = model.shareWith,
                path = model.path,
                permissions = model.permissions,
                sharedDate = model.sharedDate,
                expirationDate = model.expirationDate,
                token = model.token,
                sharedWithDisplayName = model.sharedWithDisplayName,
                sharedWithAdditionalInfo = model.sharedWithAdditionalInfo,
                isFolder = model.isFolder,
                userId = model.userId,
                remoteId = model.remoteId,
                accountOwner = model.accountOwner,
                name = model.name,
                shareLink = model.shareLink
            )
        }
}
