/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.data.capabilities.datasources.implementation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource.Companion.toEntity
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
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
    private val ocCapabilityDao = mockk<OCCapabilityDao>(relaxUnitFun = true)

    private val ocCapability = OC_CAPABILITY.copy(id = 0)
    private val ocCapabilityEntity = ocCapability.toEntity()

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        ocLocalCapabilitiesDataSource =
            OCLocalCapabilitiesDataSource(
                ocCapabilityDao,
            )
    }

    @Test
    fun `getCapabilitiesForAccountAsLiveData returns a LiveData of OCCapability`() {
        val capabilitiesLiveData = MutableLiveData(ocCapabilityEntity)
        every { ocCapabilityDao.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME) } returns capabilitiesLiveData

        val result = ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME).getLastEmittedValue()

        assertEquals(ocCapability, result)

        verify(exactly = 1) {
            ocCapabilityDao.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getCapabilitiesForAccountAsLiveData returns null when DAO returns a null capability`() {
        val capabilitiesLiveData = MutableLiveData<OCCapabilityEntity>(null)
        every { ocCapabilityDao.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME) } returns capabilitiesLiveData

        val result = ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME).getLastEmittedValue()

        assertNull(result)

        verify(exactly = 1) {
            ocCapabilityDao.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getCapabilitiesForAccount returns a OCCapability`() {
        every { ocCapabilityDao.getCapabilitiesForAccount(OC_ACCOUNT_NAME) } returns ocCapabilityEntity

        val result = ocLocalCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)

        assertEquals(ocCapability, result)

        verify(exactly = 1) {
            ocCapabilityDao.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getCapabilitiesForAccount returns null when DAO returns a null capability`() {
        every { ocCapabilityDao.getCapabilitiesForAccount(OC_ACCOUNT_NAME) } returns null

        val result = ocLocalCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)

        assertNull(result)

        verify(exactly = 1) {
            ocCapabilityDao.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `insertCapabilities saves a list of OCCapability correctly`() {
        ocLocalCapabilitiesDataSource.insertCapabilities(listOf(ocCapability))

        verify(exactly = 1) { ocCapabilityDao.replace(listOf(ocCapabilityEntity)) }
    }

    @Test
    fun `deleteCapabilitiesForAccount removes capabilities correctly`() {
        ocLocalCapabilitiesDataSource.deleteCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) { ocCapabilityDao.deleteByAccountName(OC_ACCOUNT_NAME) }
    }
}
