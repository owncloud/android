/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.files.datasources.mapper

import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.mappers.Mapper

class OCFileMapper : Mapper<OCFile, OCFileEntity> {
    override fun toModel(entity: OCFileEntity?): OCFile? =
        entity?.let {
            OCFile(
                id = it.id,
                parentId = it.parentId,
                remotePath = it.remotePath,
                owner = it.owner,
                permissions = it.permissions,
                remoteId = it.remoteId,
                privateLink = it.privateLink,
                creationTimestamp = it.creationTimestamp,
                modificationTimestamp = it.modifiedTimestamp,
                etag = it.etag,
                mimeType = it.mimeType,
                length = it.length
            )
        }

    override fun toEntity(model: OCFile?): OCFileEntity? =
        model?.let {
            OCFileEntity(
                parentId = it.parentId,
                remotePath = it.remotePath,
                owner = it.owner,
                permissions = it.permissions,
                remoteId = it.remoteId,
                privateLink = it.privateLink,
                creationTimestamp = it.creationTimestamp,
                modifiedTimestamp = it.modificationTimestamp,
                etag = it.etag,
                mimeType = it.mimeType,
                length = it.length
            )
        }
}