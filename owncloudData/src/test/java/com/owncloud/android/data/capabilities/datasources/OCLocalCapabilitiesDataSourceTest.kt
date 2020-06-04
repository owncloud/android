/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.capabilities.datasources

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.OCCapabilityMapper
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class OCLocalCapabilitiesDataSourceTest {
    private lateinit var ocLocalCapabilitiesDataSource: OCLocalCapabilitiesDataSource
    private val ocCapabilityDao = mockk<OCCapabilityDao>(relaxed = true)
    private val ocCapabilityMapper = OCCapabilityMapper()

    private val ocCapability = OC_CAPABILITY.copy(id = 0)
    private val ocCapabilityEntity = ocCapabilityMapper.toEntity(ocCapability)

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        ocLocalCapabilitiesDataSource =
            OCLocalCapabilitiesDataSource(
                ocCapabilityDao,
                ocCapabilityMapper
            )
    }

    @Test
    fun getCapabilitiesForAccountAsLiveData() {
        val capabilitiesLiveData = MutableLiveData<OCCapabilityEntity>()
        every { ocCapabilityDao.getCapabilitiesForAccountAsLiveData(any()) } returns capabilitiesLiveData

        capabilitiesLiveData.postValue(ocCapabilityEntity)

        val capabilityEmitted =
            ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(ocCapability.accountName!!)
                .getLastEmittedValue()

        assertEquals(ocCapability, capabilityEmitted)
    }

    @Test
    fun getCapabilitiesForAccountAsLiveDataNull() {
        val capabilitiesLiveData = MutableLiveData<OCCapabilityEntity>()
        every { ocCapabilityDao.getCapabilitiesForAccountAsLiveData(any()) } returns capabilitiesLiveData

        val capabilityEmitted =
            ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(ocCapability.accountName!!)
                .getLastEmittedValue()

        assertNull(capabilityEmitted)
    }

    @Test
    fun getCapabilitiesForAccount() {
        every { ocCapabilityDao.getCapabilitiesForAccount(any()) } returns ocCapabilityEntity!!

        val capabilityEmitted = ocLocalCapabilitiesDataSource.getCapabilityForAccount(ocCapability.accountName!!)

        assertEquals(ocCapability, capabilityEmitted)
    }

    @Test
    fun getCapabilitiesForAccountNull() {
        every { ocCapabilityDao.getCapabilitiesForAccountAsLiveData(any()) } returns MutableLiveData<OCCapabilityEntity>()

        val capabilityEmitted =
            ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(ocCapability.accountName!!)
                .getLastEmittedValue()

        assertNull(capabilityEmitted)
    }

    @Test
    fun insertCapabilities() {
        every { ocCapabilityDao.replace(any()) } returns Unit

        ocLocalCapabilitiesDataSource.insert(listOf(ocCapability))

        verify(exactly = 1) { ocCapabilityDao.replace(listOf(ocCapabilityEntity!!)) }
    }
}
