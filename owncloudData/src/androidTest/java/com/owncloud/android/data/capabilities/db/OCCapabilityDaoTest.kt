/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.capabilities.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.capabilities.datasources.mapper.OCCapabilityMapper
import com.owncloud.android.data.utils.LiveDataTestUtil.getValue
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SmallTest
class OCCapabilityDaoTest {
    private lateinit var ocCapabilityDao: OCCapabilityDao
    private val ocCapabilityMapper = OCCapabilityMapper()
    private val user1 = "user1@server"
    private val user2 = "user2@server"

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocCapabilityDao = db.capabilityDao()
    }

    @Test
    fun insertCapabilitiesListAndRead() {
        val entityList: List<OCCapabilityEntity> = listOf(
            ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user1))!!,
            ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user2))!!
        )

        ocCapabilityDao.insert(entityList)

        val capability = ocCapabilityDao.getCapabilitiesForAccount(user2)
        val capabilityAsLiveData = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user2))

        assertNotNull(capability)
        assertNotNull(capabilityAsLiveData)
        assertEquals(entityList[1], capability)
        assertEquals(entityList[1], capabilityAsLiveData)
    }

    @Test
    fun insertCapabilitiesAndRead() {
        val entity1 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user1))!!
        val entity2 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user2))!!

        ocCapabilityDao.insert(entity1)
        ocCapabilityDao.insert(entity2)

        val capability = ocCapabilityDao.getCapabilitiesForAccount(user2)
        val capabilityAsLiveData = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user2))

        assertNotNull(capability)
        assertNotNull(capabilityAsLiveData)
        assertEquals(entity2, capability)
        assertEquals(entity2, capabilityAsLiveData)
    }

    @Test
    fun getNonExistingCapabilities() {
        ocCapabilityDao.insert(ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user1))!!)

        val capability = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user2))

        assertNull(capability)
    }

    @Test
    fun replaceCapabilityIfAlreadyExists_exists() {
        val entity1 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(filesVersioning = CapabilityBooleanType.FALSE))!!
        val entity2 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(filesVersioning = CapabilityBooleanType.TRUE))!!

        ocCapabilityDao.insert(entity1)
        ocCapabilityDao.replace(listOf(entity2))

        val capability = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(OC_CAPABILITY.accountName!!))

        assertNotNull(capability)
        assertEquals(entity2, capability)
    }

    @Test
    fun replaceCapabilityIfAlreadyExists_doesNotExist() {
        val entity1 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user1))!!
        val entity2 = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user2))!!

        ocCapabilityDao.insert(entity1)

        ocCapabilityDao.replace(listOf(entity2))

        val capability1 = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user1))

        assertNotNull(capability1)
        assertEquals(entity1, capability1)

        // capability2 didn't exist before, it should not replace the old one but got created
        val capability2 = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user2))

        assertNotNull(capability2)
        assertEquals(entity2, capability2)
    }

    @Test
    fun deleteCapability() {
        val entity = ocCapabilityMapper.toEntity(OC_CAPABILITY.copy(accountName = user1))!!

        ocCapabilityDao.insert(entity)

        val capability1 = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user1))

        assertNotNull(capability1)

        ocCapabilityDao.delete(user1)

        val capability2 = getValue(ocCapabilityDao.getCapabilitiesForAccountAsLiveData(user1))

        assertNull(capability2)
    }
}
