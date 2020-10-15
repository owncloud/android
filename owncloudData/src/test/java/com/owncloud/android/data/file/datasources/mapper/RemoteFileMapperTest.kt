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

package com.owncloud.android.data.file.datasources.mapper

import com.owncloud.android.data.files.datasources.mapper.RemoteFileMapper
import com.owncloud.android.lib.resources.files.RemoteFile
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RemoteFileMapperTest {

    private val remoteFileMapper = RemoteFileMapper()

    private val remoteFile = RemoteFile(
        remoteId = OC_FILE.remoteId,
        remotePath = OC_FILE.remotePath,
        privateLink = OC_FILE.privateLink,
        creationTimestamp = OC_FILE.creationTimestamp!!,
        owner = OC_FILE.owner,
        modifiedTimestamp = OC_FILE.modificationTimestamp,
        mimeType = OC_FILE.mimeType,
        permissions = OC_FILE.permissions,
        etag = OC_FILE.etag,
        length = fileLength,
        quotaAvailableBytes = null,
        quotaUsedBytes = null,
        size = folderSize
    )

    @Test
    fun `to Model - ok - folder`() {
        val model = remoteFileMapper.toModel(remoteFile.copy(mimeType = OC_FOLDER.mimeType))

        assertNotNull(model)
        assertEquals(OC_FILE.owner, model!!.owner)
        assertEquals(OC_FILE.remoteId, model.remoteId)
        assertEquals(OC_FILE.remotePath, model.remotePath)
        assertEquals(folderSize, model.length)
        assertEquals(OC_FILE.creationTimestamp, model.creationTimestamp)
        assertEquals(OC_FILE.modificationTimestamp, model.modificationTimestamp)
        assertEquals(OC_FOLDER.mimeType, model.mimeType)
        assertEquals(OC_FILE.etag, model.etag)
        assertEquals(OC_FILE.permissions, model.permissions)
        assertEquals(OC_FILE.privateLink, model.privateLink)
    }

    @Test
    fun `to Model - ok - file`() {
        val model = remoteFileMapper.toModel(remoteFile.copy(mimeType = OC_FILE.mimeType))

        assertNotNull(model)
        assertEquals(OC_FILE.owner, model!!.owner)
        assertEquals(OC_FILE.remoteId, model.remoteId)
        assertEquals(OC_FILE.remotePath, model.remotePath)
        assertEquals(fileLength, model.length)
        assertEquals(OC_FILE.creationTimestamp, model.creationTimestamp)
        assertEquals(OC_FILE.modificationTimestamp, model.modificationTimestamp)
        assertEquals(OC_FILE.mimeType, model.mimeType)
        assertEquals(OC_FILE.etag, model.etag)
        assertEquals(OC_FILE.permissions, model.permissions)
        assertEquals(OC_FILE.privateLink, model.privateLink)
    }

    companion object {
        const val folderSize: Long = 1234
        const val fileLength: Long = 123
    }
}
