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

package com.owncloud.android.data.file.datasources

import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource
import com.owncloud.android.data.files.datasources.mapper.OCFileMapper
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.testutil.OC_FILE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OCLocalFileDataSourceTest {
    private lateinit var localDataSource: OCLocalFileDataSource
    private lateinit var dao: FileDao

    private val mapper = OCFileMapper()

    @Before
    fun init() {
        dao = spyk()
        localDataSource = OCLocalFileDataSource(dao, mapper)
    }

    @Test
    fun `get file by id - ok`() {
        every { dao.getFileById(any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileById(OC_FILE.id!!)

        assertEquals(OC_FILE, result)

        verify { dao.getFileById(OC_FILE.id!!) }
    }

    @Test
    fun `get file by id - ok - null`() {
        every { dao.getFileById(any()) } returns null

        val result = localDataSource.getFileById(DUMMY_FILE_ENTITY.id)

        assertNull(result)

        verify { dao.getFileById(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `get file by id - ko`() {
        every { dao.getFileById(any()) } throws Exception()

        localDataSource.getFileById(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `get file by remote path - ok`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)

        assertEquals(OC_FILE, result)

        verify { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath) }
    }

    @Test
    fun `get file by remote path - ok - null`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } returns null

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)

        assertNull(result)

        verify { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath) }
    }

    @Test
    fun `get file by remote path - ok - null - create root folder`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } returns null
        every { dao.mergeRemoteAndLocalFile(any()) } returns 1234
        every { dao.getFileById(1234) } returns DUMMY_FILE_ENTITY.copy(
            parentId = ROOT_PARENT_ID,
            mimeType = MIME_DIR,
            remotePath = ROOT_PATH
        )

        val result = localDataSource.getFileByRemotePath(ROOT_PATH, OC_FILE.owner)

        assertNotNull(result)
        assertEquals(ROOT_PARENT_ID, result!!.parentId)
        assertEquals(OC_FILE.owner, result.owner)
        assertEquals(MIME_DIR, result.mimeType)
        assertEquals(ROOT_PATH, result.remotePath)

        verify {
            dao.getFileByOwnerAndRemotePath(OC_FILE.owner, ROOT_PATH)
            dao.mergeRemoteAndLocalFile(any())
            dao.getFileById(1234)
        }
    }

    @Test(expected = Exception::class)
    fun `get file by remote path - ko`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } throws Exception()

        localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)

        verify { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath) }
    }

    @Test
    fun `get folder content - ok`() {
        every { dao.getFolderContent(any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFolderContent(DUMMY_FILE_ENTITY.id)

        assertEquals(listOf(OC_FILE), result)

        verify { dao.getFolderContent(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `get folder content - ko`() {
        every { dao.getFolderContent(any()) } throws Exception()

        localDataSource.getFolderContent(DUMMY_FILE_ENTITY.id)

        verify { dao.getFolderContent(DUMMY_FILE_ENTITY.id) }
    }

    @Test
    fun `get folder images - ok`() {
        every { dao.getFolderByMimeType(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        assertEquals(listOf(OC_FILE), result)

        verify { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test(expected = Exception::class)
    fun `get folder images - ko`() {
        every { dao.getFolderByMimeType(any(), any()) } throws Exception()

        localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        verify { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test
    fun `save file - ok`() {
        every { dao.mergeRemoteAndLocalFile(any()) } returns 1

        val result = localDataSource.saveFile(OC_FILE)

        assertEquals(Unit, result)

        verify(exactly = 1) { dao.mergeRemoteAndLocalFile(DUMMY_FILE_ENTITY) }
    }

    @Test(expected = Exception::class)
    fun `save file - ko`() {
        every { dao.mergeRemoteAndLocalFile(any()) } throws Exception()

        localDataSource.saveFile(OC_FILE)

        verify { dao.mergeRemoteAndLocalFile(DUMMY_FILE_ENTITY) }
    }

    companion object {
        private val DUMMY_FILE_ENTITY: OCFileEntity = OCFileMapper().toEntity(OC_FILE)!!
    }
}
