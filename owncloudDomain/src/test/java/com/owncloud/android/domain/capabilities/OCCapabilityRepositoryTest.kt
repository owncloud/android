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

package com.owncloud.android.domain.capabilities

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.owncloud.android.data.DataResult
import com.owncloud.android.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.utils.DomainTestUtil
import com.owncloud.android.domain.utils.InstantExecutors
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.RemoteCapability
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OCCapabilityRepositoryTest {
    private lateinit var ocCapabilityRepository: OCCapabilityRepository

    private val localCapabilitiesDataSource = mockk<LocalCapabilitiesDataSource>(relaxed = true)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun loadCapabilityFromNetwork() {
        val dbData = MutableLiveData<OCCapabilityEntity>()
        val defaultAccountName = "admin@server"

        every {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                defaultAccountName
            )
        } returns dbData

        val remoteCapability = DomainTestUtil.createRemoteCapability(defaultAccountName)
        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(remoteCapability, true)
        val remoteCapabilitiesDataSource =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(), localCapabilitiesDataSource, remoteCapabilitiesDataSource
            )

        val data = ocCapabilityRepository.getCapabilityForAccount(defaultAccountName)

        val observer = mockk<Observer<DataResult<OCCapabilityEntity>>>(relaxed = true)
        data.observeForever(observer)

        dbData.postValue(null)

        // Get capabilities from database to observe them, is called twice (one showing current db capabilities while
        // getting capabilities from server and another one with db capabilities already updated with server ones)
        verify(exactly = 2) {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(defaultAccountName)
        }

        // Retrieving capabilities from server...

        // Capabilities are always retrieved from server and inserted in database if not empty list
        verify {
            localCapabilitiesDataSource.insert(
                listOf(OCCapabilityEntity.fromRemoteCapability(remoteCapability.apply {
                    accountName = defaultAccountName
                }))
            )
        }

        // Observe changes in database livedata when there's a new capability
        val newCapability = DomainTestUtil.createCapability("user@server")

        dbData.postValue(
            newCapability
        )

        verify { observer.onChanged(DataResult.success(newCapability)) }
    }

    @Test
    fun loadEmptyCapabilityForAccountFromNetwork() {
        val dbData = MutableLiveData<OCCapabilityEntity>()
        val defaultAccountName = "user@server"

        every {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                defaultAccountName
            )
        } returns dbData

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(RemoteCapability(), true)

        val remoteCapabilitiesDataSource =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(),
                localCapabilitiesDataSource,
                remoteCapabilitiesDataSource
            )


        val data = ocCapabilityRepository.getCapabilityForAccount(
            defaultAccountName
        )

        val observer = mockk<Observer<DataResult<OCCapabilityEntity>>>(relaxed = true)
        data.observeForever(observer)

        dbData.postValue(null)

        // Get capabilities from database to observe them, is called twice (one showing current db capabilities while
        // getting capabilities from server and another one with db capabilities already updated with server ones)
        verify(exactly = 2) {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(defaultAccountName)
        }

        // Retrieving capabilities from server...

        // Observe changes in database livedata when the list of capabilities is empty
        dbData.postValue(null)

        verify { observer.onChanged(DataResult.success(null)) }
    }

    @Test
    fun loadCapabilitiesForAccountFromNetworkWithError() {
        val dbData = MutableLiveData<OCCapabilityEntity>()
        val defaultAccountName = "cfo@server"

        dbData.value = null // DB does not include capabilities yet

        every {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(defaultAccountName)
        } returns dbData

        val exception = Exception("Error when retrieving capabilities")

        val remoteOperationResult = DomainTestUtil.createRemoteOperationResultMock(
            RemoteCapability(),
            false,
            resultCode = RemoteOperationResult.ResultCode.FORBIDDEN,
            exception = exception
        )

        val remoteCapabilitiesDataSourceTest =
            RemoteCapabilitiesDataSourceTest(remoteOperationResult)

        ocCapabilityRepository =
            OCCapabilityRepository.create(
                InstantExecutors(),
                localCapabilitiesDataSource,
                remoteCapabilitiesDataSourceTest
            )

        val data = ocCapabilityRepository.getCapabilityForAccount(
            defaultAccountName
        )

        // Get capabilities from database to observe them
        verify {
            localCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                defaultAccountName
            )
        }

        // Retrieving capabilities from server...

        // Observe changes in database livedata when there's an error from server
        val observer = mockk<Observer<DataResult<OCCapabilityEntity>>>(relaxed = true)
        data.observeForever(observer)

        verify {
            observer.onChanged(
                DataResult.error(
                    RemoteOperationResult.ResultCode.FORBIDDEN, dbData.value, exception = exception
                )
            )
        }
    }
}
