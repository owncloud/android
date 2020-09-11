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

package com.owncloud.android.data.file.datasource

import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource
import com.owncloud.android.data.files.datasources.mapper.OCFileMapper
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.testutil.OC_FILE
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalFileDataSourceTest {
    private lateinit var localDataSource: OCLocalFileDataSource

    private val dao = mockk<FileDao>(relaxed = true)
    private val mapper = OCFileMapper()

    private val dummy = OC_FILE.copy(id = 0)
    private val dummy_entity = mapper.toEntity(OC_FILE)

    @Before
    fun init() {
        localDataSource = OCLocalFileDataSource(dao, mapper)
    }

    @Test
    fun getFileByIdSuccess() {
        every { dao.getFileById(any()) } returns dummy_entity!!

        val result = localDataSource.getFileById(dummy.id!!)

        assertEquals(dummy, result)
    }

    @Test(expected = Exception::class)
    fun getFileByIdException() {
        every { dao.getFileById(any()) } throws Exception()

        localDataSource.getFileById(dummy.id!!)
    }
}
