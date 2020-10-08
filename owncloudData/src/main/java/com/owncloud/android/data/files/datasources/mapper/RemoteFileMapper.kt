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

import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.files.RemoteFile

class RemoteFileMapper : RemoteMapper<OCFile, RemoteFile> {
    override fun toModel(remote: RemoteFile?): OCFile? =
        remote?.let {
            OCFile(
                owner = it.owner,
                remoteId = it.remoteId,
                remotePath = it.remotePath,
                length = if (it.isFolder) it.size else it.length,
                creationTimestamp = it.creationTimestamp,
                modifiedTimestamp = it.modifiedTimestamp,
                mimeType = it.mimeType,
                etag = it.etag,
                permissions = it.permissions,
                privateLink = it.privateLink
            )
        }

    // Not needed
    override fun toRemote(model: OCFile?): RemoteFile? = null

}
