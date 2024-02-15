/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_CAPABILITY_WITH_FILES_APP_PROVIDERS
import com.owncloud.android.testutil.livedata.getLastEmittedValue
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
    private val remoteCapabilitiesDataSource = mockk<RemoteCapabilitiesDataSource>()
    private val appRegistryRepository = mockk<AppRegistryRepository>(relaxUnitFun = true)
    private val ocCapabilityRepository: OCCapabilityRepository =
        OCCapabilityRepository(localCapabilitiesDataSource, remoteCapabilitiesDataSource, appRegistryRepository)

    @Test
    fun `getCapabilitiesAsLiveData returns a LiveData of OCCapability`() {
        val capabilitiesLiveData = MutableLiveData(OC_CAPABILITY)

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME)
        } returns capabilitiesLiveData

        val capabilitiesToEmit = capabilitiesLiveData.getLastEmittedValue()

        val capabilitiesEmitted = ocCapabilityRepository.getCapabilitiesAsLiveData(OC_ACCOUNT_NAME).getLastEmittedValue()

        assertEquals(capabilitiesToEmit, capabilitiesEmitted)

        verify(exactly = 1) { localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getStoredCapabilities returns an OCCapability`() {

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME)
        } returns OC_CAPABILITY

        val actualResult = ocCapabilityRepository.getStoredCapabilities(OC_ACCOUNT_NAME)

        assertEquals(OC_CAPABILITY, actualResult)

        verify(exactly = 1) { localCapabilitiesDataSource.getCapabilitiesForAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `refreshCapabilitiesForAccount updates capabilities correctly when files app providers is not null`() {

        every { remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME) } returns OC_CAPABILITY_WITH_FILES_APP_PROVIDERS

        ocCapabilityRepository.refreshCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)
            localCapabilitiesDataSource.insertCapabilities(listOf(OC_CAPABILITY_WITH_FILES_APP_PROVIDERS))
            appRegistryRepository.refreshAppRegistryForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `refreshCapabilitiesForAccount updates capabilities correctly but not refresh AppRegistry when filesAppProviders is null`() {

        every { remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME) } returns OC_CAPABILITY

        ocCapabilityRepository.refreshCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)
            localCapabilitiesDataSource.insertCapabilities(listOf(OC_CAPABILITY))
        }
    }
}
