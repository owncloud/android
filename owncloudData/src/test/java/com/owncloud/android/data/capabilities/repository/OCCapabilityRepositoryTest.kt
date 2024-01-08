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

package com.owncloud.android.data.capabilities.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.domain.appregistry.AppRegistryRepository
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OCCapabilityRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxUnitFun = true)
    private val remoteCapabilitiesDataSource = mockk<RemoteCapabilitiesDataSource>(relaxUnitFun = true)
    private val appRegistryRepository = mockk<AppRegistryRepository>()
    private val ocCapabilityRepository: OCCapabilityRepository =
        OCCapabilityRepository(localCapabilitiesDataSource, remoteCapabilitiesDataSource, appRegistryRepository)

    @Test
    fun `getCapabilitiesAsLiveData returns a livedata of OCCapability correctly`() {
        val capabilitiesLiveData = MutableLiveData<OCCapability>()

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(any())
        } returns capabilitiesLiveData

        val capabilitiesEmitted = mutableListOf<OCCapability>()
        ocCapabilityRepository.getCapabilitiesAsLiveData(OC_ACCOUNT_NAME).observeForever {
            capabilitiesEmitted.add(it!!)
        }

        val capabilitiesToEmit = listOf(OC_CAPABILITY)
        capabilitiesToEmit.forEach {
            capabilitiesLiveData.postValue(it)
        }
        assertEquals(capabilitiesToEmit, capabilitiesEmitted)

        verify(exactly = 1) { localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getStoredCapabilities returns an object of OCCapability` () {

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(any())
        } returns OC_CAPABILITY

        val actualResult = ocCapabilityRepository.getStoredCapabilities(OC_ACCOUNT_NAME)

        assertEquals(OC_CAPABILITY, actualResult)

        verify(exactly = 1) { localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getStoredCapabilities returns null when DataSource receive a null capability`() {

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(any())
        } returns null

        val result = ocCapabilityRepository.getStoredCapabilities(OC_ACCOUNT_NAME)

        assertEquals(null, result)

        verify(exactly = 1) { localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME) }

    }

    @Test
    fun `refreshCapabilitiesForAccount update capabilities correctly`() {
        val capability = OC_CAPABILITY.copy(accountName = OC_ACCOUNT_NAME)

        every { remoteCapabilitiesDataSource.getCapabilities(any()) } returns capability

        ocCapabilityRepository.refreshCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)
        }

        verify(exactly = 1) {
            localCapabilitiesDataSource.insertCapabilities(listOf(capability))
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun refreshCapabilitiesFromNetworkNoConnection() {

        every { remoteCapabilitiesDataSource.getCapabilities(any()) } throws NoConnectionWithServerException()

        ocCapabilityRepository.refreshCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)
        }
        verify(exactly = 0) {
            localCapabilitiesDataSource.insertCapabilities(any())
        }
    }
}
