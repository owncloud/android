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

package com.owncloud.android.data.capabilities.datasources.mapper

import com.owncloud.android.data.utils.DataTestUtil.createCapability
import com.owncloud.android.data.utils.DataTestUtil.createCapabilityEntity
import org.junit.Assert
import org.junit.Test

class OCCapabilityMapperTest {

    private val ocCapabilityMapper = OCCapabilityMapper()

    private val ocCapability = createCapability()
    private val ocCapabilityEntity = createCapabilityEntity()

    @Test
    fun checkToModelNull() {
        Assert.assertNull(ocCapabilityMapper.toModel(null))
    }

    @Test
    fun checkToModelNotNull() {
        Assert.assertNotNull(ocCapabilityEntity)

        val model = ocCapabilityMapper.toModel(ocCapabilityEntity)
        Assert.assertNotNull(model)
        Assert.assertEquals(ocCapability, model)

        val mappedEntity = ocCapabilityMapper.toEntity(model)
        Assert.assertNotNull(mappedEntity)

        Assert.assertEquals(ocCapabilityEntity, mappedEntity)
    }

    @Test
    fun checkToEntityNull() {
        Assert.assertNull(ocCapabilityMapper.toEntity(null))
    }

    @Test
    fun checkToEntityNotNull() {
        val entity = ocCapabilityMapper.toEntity(ocCapability)
        Assert.assertNotNull(entity)

        val model = ocCapabilityMapper.toModel(entity)
        Assert.assertNotNull(model)

        Assert.assertEquals(ocCapability, model)
    }
}
