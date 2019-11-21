/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.data.shares.datasources.mapper

import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.testutil.OC_SHARE
import org.junit.Assert
import org.junit.Test

class OCShareMapperTest {
    private val ocShareMapper = OCShareMapper()

    private val ocShare =
        OC_SHARE.copy(
            isFolder = false,
            path = "/Photos/image2.jpg"
        )

    private val ocShareEntity =
        ocShareMapper.toEntity(
            OC_SHARE.copy(
                isFolder = false,
                path = "/Photos/image2.jpg"
            )
        )

    @Test
    fun checkToModelNull() {
        Assert.assertNull(ocShareMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {
        Assert.assertNotNull(ocShareEntity)

        val model = ocShareMapper.toModel(ocShareEntity)
        Assert.assertNotNull(model)
        Assert.assertEquals(ocShare, model)

        val mappedEntity = ocShareMapper.toEntity(model)
        Assert.assertNotNull(mappedEntity)

        Assert.assertEquals(ocShareEntity, mappedEntity)
    }

    @Test
    fun checkToEntityNull() {
        Assert.assertNull(ocShareMapper.toEntity(null))
    }

    @Test
    fun checkToEntityNotNull() {
        val entity = ocShareMapper.toEntity(ocShare)
        Assert.assertNotNull(entity)

        val model = ocShareMapper.toModel(entity)
        Assert.assertNotNull(model)

        Assert.assertEquals(ocShare, model)
    }
}
