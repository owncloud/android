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
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OCCapabilityRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxed = true)
    private val remoteCapabilitiesDataSource = mockk<RemoteCapabilitiesDataSource>(relaxed = true)
    private val ocCapabilityRepository: OCCapabilityRepository =
        OCCapabilityRepository(localCapabilitiesDataSource, remoteCapabilitiesDataSource)

    @Test
    fun refreshCapabilitiesFromNetworkOk() {
        val capability = OC_CAPABILITY.copy(accountName = OC_ACCOUNT_NAME)

        every { remoteCapabilitiesDataSource.getCapabilities(any()) } returns capability

        ocCapabilityRepository.refreshCapabilitiesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)
        }

        verify(exactly = 1) {
            localCapabilitiesDataSource.insert(listOf(capability))
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
            localCapabilitiesDataSource.insert(any())
        }
    }

    @Test
    fun getCapabilitiesAsLiveData() {
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

        Assert.assertEquals(capabilitiesToEmit, capabilitiesEmitted)
    }

    @Test(expected = Exception::class)
    fun getCapabilitiesAsLiveDataException() {
        every {
            localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(any())
        } throws Exception()

        ocCapabilityRepository.getCapabilitiesAsLiveData(OC_ACCOUNT_NAME)
    }

    @Test
    fun getStoredCapabilities() {

        every {
            localCapabilitiesDataSource.getCapabilityForAccount(any())
        } returns OC_CAPABILITY

        val result = ocCapabilityRepository.getStoredCapabilities(OC_ACCOUNT_NAME)

        Assert.assertEquals(OC_CAPABILITY, result)
    }

    @Test
    fun getStoredCapabilitiesNull() {

        every {
            localCapabilitiesDataSource.getCapabilityForAccount(any())
        } returns null

        val result = ocCapabilityRepository.getStoredCapabilities(OC_ACCOUNT_NAME)

        Assert.assertEquals(null, result)
    }
}
