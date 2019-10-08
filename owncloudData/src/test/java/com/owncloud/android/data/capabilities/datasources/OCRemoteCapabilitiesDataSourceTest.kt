/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
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

package com.owncloud.android.data.capabilities.datasources

import com.owncloud.android.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.network.OCCapabilityService
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteCapabilityMapper
import com.owncloud.android.data.utils.DataTestUtil
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class OCRemoteCapabilitiesDataSourceTest {
    private lateinit var ocRemoteCapabilitiesDataSource: OCRemoteCapabilitiesDataSource

    private val ocCapabilityService: OCCapabilityService = mockk()
    private val remoteCapabilityMapper = RemoteCapabilityMapper()

    @Before
    fun init() {
        ocRemoteCapabilitiesDataSource =
            OCRemoteCapabilitiesDataSource(
                ocCapabilityService,
                remoteCapabilityMapper
            )
    }

    @Test
    fun readRemoteCapabilities() {
        val accountName = "ceo@server"

        val remoteCapability = DataTestUtil.createRemoteCapability(
            accountName, 15, 14, 13
        )

        val getRemoteCapabilitiesOperationResult = DataTestUtil.createRemoteOperationResultMock(
            remoteCapability,
            true
        )

        every {
            ocCapabilityService.getCapabilities()
        } returns getRemoteCapabilitiesOperationResult

        // Get capability from remote datasource
        val capabilities = ocRemoteCapabilitiesDataSource.getCapabilities(accountName)

        assertThat(capabilities, notNullValue())

        assertEquals("ceo@server", capabilities.accountName)
        assertEquals(15, capabilities.versionMayor)
        assertEquals(14, capabilities.versionMinor)
        assertEquals(13, capabilities.versionMicro)
    }
}
