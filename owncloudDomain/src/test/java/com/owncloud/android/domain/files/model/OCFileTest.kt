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
package com.owncloud.android.domain.files.model

import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OCFileTest {

    @Test
    fun `test equals - ok`() {
        val item1 = OCFile(
            OC_FILE.id,
            OC_FILE.parentId,
            OC_FILE.owner,
            OC_FILE.length,
            OC_FILE.creationTimestamp,
            OC_FILE.modificationTimestamp,
            OC_FILE.remotePath,
            OC_FILE.mimeType,
            OC_FILE.etag,
            OC_FILE.permissions,
            OC_FILE.remoteId,
            OC_FILE.privateLink,
            OC_FILE.storagePath,
            OC_FILE.treeEtag,
            OC_FILE.keepInSync,
            OC_FILE.lastSyncDateForData,
            OC_FILE.lastSyncDateForProperties,
            OC_FILE.needsToUpdateThumbnail,
            OC_FILE.modifiedAtLastSyncForData,
            OC_FILE.etagInConflict,
            OC_FILE.fileIsDownloading,
            OC_FILE.sharedWithSharee,
            OC_FILE.sharedByLink
        )

        val item2 = OCFile(
            id = OC_FILE.id,
            parentId = OC_FILE.parentId,
            owner = OC_FILE.owner,
            length = OC_FILE.length,
            creationTimestamp = OC_FILE.creationTimestamp,
            modificationTimestamp = OC_FILE.modificationTimestamp,
            remotePath = OC_FILE.remotePath,
            mimeType = OC_FILE.mimeType,
            etag = OC_FILE.etag,
            permissions = OC_FILE.permissions,
            remoteId = OC_FILE.remoteId,
            privateLink = OC_FILE.privateLink,
            storagePath = OC_FILE.storagePath,
            treeEtag = OC_FILE.treeEtag,
            keepInSync = OC_FILE.keepInSync,
            lastSyncDateForData = OC_FILE.lastSyncDateForData,
            lastSyncDateForProperties = OC_FILE.lastSyncDateForProperties,
            needsToUpdateThumbnail = OC_FILE.needsToUpdateThumbnail,
            modifiedAtLastSyncForData = OC_FILE.modifiedAtLastSyncForData,
            etagInConflict = OC_FILE.etagInConflict,
            fileIsDownloading = OC_FILE.fileIsDownloading,
            sharedWithSharee = OC_FILE.sharedWithSharee,
            sharedByLink = OC_FILE.sharedByLink
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun `test equals - ko`() {
        val item1 = OCFile(
            OC_FILE.id,
            OC_FILE.parentId,
            OC_FILE.owner,
            OC_FILE.length,
            OC_FILE.creationTimestamp,
            OC_FILE.modificationTimestamp,
            OC_FILE.remotePath,
            OC_FILE.mimeType,
            OC_FILE.etag,
            OC_FILE.permissions,
            OC_FILE.remoteId,
            OC_FILE.privateLink,
            OC_FILE.storagePath,
            OC_FILE.treeEtag,
            OC_FILE.keepInSync,
            OC_FILE.lastSyncDateForData,
            OC_FILE.lastSyncDateForProperties,
            OC_FILE.needsToUpdateThumbnail,
            OC_FILE.modifiedAtLastSyncForData,
            OC_FILE.etagInConflict,
            OC_FILE.fileIsDownloading,
            OC_FILE.sharedWithSharee,
            OC_FILE.sharedByLink
        )

        val item2 = OCFile(
            id = 123,
            parentId = OC_FILE.parentId,
            owner = OC_FILE.owner,
            length = OC_FILE.length,
            creationTimestamp = OC_FILE.creationTimestamp,
            modificationTimestamp = OC_FILE.modificationTimestamp,
            remotePath = OC_FILE.remotePath,
            mimeType = OC_FILE.mimeType,
            etag = OC_FILE.etag,
            permissions = OC_FILE.permissions,
            remoteId = OC_FILE.remoteId,
            privateLink = OC_FILE.privateLink,
            storagePath = OC_FILE.storagePath,
            treeEtag = OC_FILE.treeEtag,
            keepInSync = OC_FILE.keepInSync,
            lastSyncDateForData = OC_FILE.lastSyncDateForData,
            lastSyncDateForProperties = OC_FILE.lastSyncDateForProperties,
            needsToUpdateThumbnail = OC_FILE.needsToUpdateThumbnail,
            modifiedAtLastSyncForData = OC_FILE.modifiedAtLastSyncForData,
            etagInConflict = OC_FILE.etagInConflict,
            fileIsDownloading = OC_FILE.fileIsDownloading,
            sharedWithSharee = OC_FILE.sharedWithSharee,
            sharedByLink = OC_FILE.sharedByLink
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun `test filename - ok`() {
        val ocFile = OCFile(
            owner = OC_FILE.owner,
            length = OC_FILE.length,
            modificationTimestamp = OC_FILE.modificationTimestamp,
            remotePath = "/Photos/",
            mimeType = OC_FILE.mimeType
        )
        assertNotNull(ocFile.fileName)
        assertEquals("Photos", ocFile.fileName)
    }

    @Test
    fun `test file is folder - unix dir`() {
        val ocFile = OC_FOLDER.copy(mimeType = MIME_DIR_UNIX)
        assertTrue(ocFile.isFolder)
    }

    @Test
    fun `test file is folder - dir`() {
        val ocFile = OC_FOLDER.copy(mimeType = MIME_DIR)
        assertTrue(ocFile.isFolder)
    }

    @Test
    fun `test file is audio - ok`() {
        val ocFile = OC_FILE.copy(mimeType = "${MIME_PREFIX_AUDIO}ogg")
        assertTrue(ocFile.isAudio)
    }

    @Test
    fun `test file is video - ok`() {
        val ocFile = OC_FILE.copy(mimeType = "${MIME_PREFIX_VIDEO}mp4")
        assertTrue(ocFile.isVideo)
    }

    @Test
    fun `test is image - ok`() {
        val ocFile = OC_FILE.copy(mimeType = "${MIME_PREFIX_IMAGE}jpeg")
        assertTrue(ocFile.isImage)
    }

    @Test
    fun `test file is available locally - ok - null`() {
        val ocFile = OC_FILE.copy(storagePath = null)
        assertFalse(ocFile.isAvailableLocally)
    }

    @Test
    fun `test file exists - ok - null`() {
        val ocFile = OC_FILE.copy(id = null)
        assertFalse(ocFile.fileExists)
    }

    @Test
    fun `test file exists - ok - (-1)`() {
        val ocFile = OC_FILE.copy(id = -1)
        assertFalse(ocFile.fileExists)
    }

    @Test
    fun `test file exists - ok`() {
        val ocFile = OC_FILE.copy(id = 1123)
        assertTrue(ocFile.fileExists)
    }

    @Test
    fun `test is hidden - ok`() {
        val ocFile = OC_FILE.copy(remotePath = ".secretFile")
        assertTrue(ocFile.isHidden)
    }

    @Test
    fun `test shared with me - ok`() {
        val ocFile = OC_FILE.copy(permissions = "RDNSCK")
        assertTrue(ocFile.isSharedWithMe)
    }

    @Test
    fun `test shared with me - ok - false`() {
        val ocFile = OC_FILE.copy(permissions = "RDCK")
        assertFalse(ocFile.isSharedWithMe)
    }

}
