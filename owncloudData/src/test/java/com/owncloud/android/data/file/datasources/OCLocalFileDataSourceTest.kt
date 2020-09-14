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
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.testutil.OC_FILE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalFileDataSourceTest {
    private lateinit var localDataSource: OCLocalFileDataSource
    private lateinit var dao: FileDao

    private val mapper = OCFileMapper()

    private val dummy = OC_FILE.copy(id = 0)
    private val dummyEntity = mapper.toEntity(OC_FILE)!!

    @Before
    fun init() {
        dao = spyk()
        localDataSource = OCLocalFileDataSource(dao, mapper)
    }

    @Test
    fun `get file by id - ok`() {
        every { dao.getFileById(any()) } returns dummyEntity

        val result = localDataSource.getFileById(dummy.id!!)

        assertEquals(dummy, result)

        verify { dao.getFileById(dummy.id!!) }
    }

    @Test
    fun `get file by id - ok - null`() {
        every { dao.getFileById(any()) } returns null

        val result = localDataSource.getFileById(dummy.id!!)

        assertEquals(null, result)

        verify { dao.getFileById(dummy.id!!) }
    }

    @Test(expected = Exception::class)
    fun `get file by id - ko`() {
        every { dao.getFileById(any()) } throws Exception()

        localDataSource.getFileById(dummy.id!!)
    }

    @Test
    fun `get file by remote path - ok`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } returns dummyEntity

        val result = localDataSource.getFileByRemotePath(dummy.remotePath, dummy.owner)

        assertEquals(dummy, result)

        verify { dao.getFileByOwnerAndRemotePath(dummy.owner, dummy.remotePath) }
    }

    @Test
    fun `get file by remote path - ok - null`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } returns null

        val result = localDataSource.getFileByRemotePath(dummy.remotePath, dummy.owner)

        assertEquals(null, result)

        verify { dao.getFileByOwnerAndRemotePath(dummy.owner, dummy.remotePath) }
    }

    @Test(expected = Exception::class)
    fun `get file by remote path - ko`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any()) } throws Exception()

        localDataSource.getFileByRemotePath(dummy.remotePath, dummy.owner)
    }

    @Test
    fun `get folder content - ok`() {
        every { dao.getFolderContent(any()) } returns listOf(dummyEntity)

        val result = localDataSource.getFolderContent(dummy.id!!)

        assertEquals(listOf(dummy), result)

        verify { dao.getFolderContent(dummy.id!!) }
    }

    @Test(expected = Exception::class)
    fun `get folder content - ko`() {
        every { dao.getFolderContent(any()) } throws Exception()

        localDataSource.getFolderContent(dummy.id!!)
    }

    @Test
    fun `get folder images - ok`() {
        every { dao.getFolderByMimeType(any(), any()) } returns listOf(dummyEntity)

        val result = localDataSource.getFolderImages(dummy.id!!)

        assertEquals(listOf(dummy), result)

        verify { dao.getFolderByMimeType(dummy.id!!, MIME_PREFIX_IMAGE) }
    }

    @Test(expected = Exception::class)
    fun `get folder images - ko`() {
        every { dao.getFolderByMimeType(any(), any()) } throws Exception()

        localDataSource.getFolderImages(dummy.id!!)
    }
}
