/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.lib.resources.status.services.implementation.OCCapabilityService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteCapabilitiesDataSourceTest {

    private lateinit var ocRemoteCapabilitiesDataSource: OCRemoteCapabilitiesDataSource

    private val ocCapabilityService: OCCapabilityService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)
    private val remoteCapabilityMapper = RemoteCapabilityMapper()

    @Before
    fun setUp() {
        every { clientManager.getCapabilityService(OC_ACCOUNT_NAME) } returns ocCapabilityService

        ocRemoteCapabilitiesDataSource =
            OCRemoteCapabilitiesDataSource(
                clientManager,
                remoteCapabilityMapper
            )
    }

    @Test
    fun `getCapabilities returns a OCCapability`() {
        val remoteCapability = remoteCapabilityMapper.toRemote(OC_CAPABILITY)!!

        val getRemoteCapabilitiesOperationResult = createRemoteOperationResultMock(remoteCapability, true)

        every { ocCapabilityService.getCapabilities() } returns getRemoteCapabilitiesOperationResult

        val result = ocRemoteCapabilitiesDataSource.getCapabilities(OC_ACCOUNT_NAME)

        assertEquals(OC_CAPABILITY, result)

        verify(exactly = 1) {
            clientManager.getCapabilityService(OC_ACCOUNT_NAME)
            ocCapabilityService.getCapabilities()
        }
    }
}
