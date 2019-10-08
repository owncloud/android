/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.capabilities.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
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
        val defaultAccountName = "admin@server"

        val capability = DataTestUtil.createCapability(defaultAccountName)

        every { remoteCapabilitiesDataSource.getCapabilities(any()) } returns capability

        ocCapabilityRepository.refreshCapabilitiesForAccount(defaultAccountName)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(defaultAccountName)
        }

        verify(exactly = 1) {
            localCapabilitiesDataSource.insert(listOf(capability))
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun refreshCapabilitiesFromNetworkNoConnection() {
        val defaultAccountName = "admin@server"
        val capability = DataTestUtil.createCapability(defaultAccountName)

        every { remoteCapabilitiesDataSource.getCapabilities(any()) } throws NoConnectionWithServerException()

        ocCapabilityRepository.refreshCapabilitiesForAccount(defaultAccountName)

        verify(exactly = 1) {
            remoteCapabilitiesDataSource.getCapabilities(defaultAccountName)
        }
        verify(exactly = 0) {
            localCapabilitiesDataSource.insert(listOf(capability))
        }
    }

    @Test
    fun getStoredCapabilitiesOk() {
        val defaultAccountName = "admin@server"

        val capabilitiesLiveData = MutableLiveData<OCCapability>()

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(any())
        } returns capabilitiesLiveData

        val capabilitiesEmitted = mutableListOf<OCCapability>()
        ocCapabilityRepository.getStoredCapabilities(defaultAccountName).observeForever {
            capabilitiesEmitted.add(it!!)
        }

        val capabilitiesToEmit = listOf(DataTestUtil.createCapability())
        capabilitiesToEmit.forEach {
            capabilitiesLiveData.postValue(it)
        }

        Assert.assertEquals(capabilitiesToEmit, capabilitiesEmitted)
    }

    @Test(expected = Exception::class)
    fun getStoredCapabilitiesException() {
        val defaultAccountName = "admin@server"

        val capabilitiesLiveData = MutableLiveData<OCCapability>()

        every {
            localCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(any())
        } throws Exception()

        val capabilitiesEmitted = mutableListOf<OCCapability>()
        ocCapabilityRepository.getStoredCapabilities(defaultAccountName)

        val capabilitiesToEmit = listOf(DataTestUtil.createCapability())
        capabilitiesToEmit.forEach {
            capabilitiesLiveData.postValue(it)
        }

        Assert.assertEquals(capabilitiesToEmit, capabilitiesEmitted)
    }
}
