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

package com.owncloud.android.capabilities.datasource

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCRemoteCapabilitiesDataSourceTest {
    private lateinit var ocRemoteCapabilitiesDataSource: OCRemoteCapabilitiesDataSource
    private val ownCloudClient = mock(OwnCloudClient::class.java)

    @Before
    fun init() {
        ocRemoteCapabilitiesDataSource = OCRemoteCapabilitiesDataSource(ownCloudClient)
    }

    @Test
    fun readRemoteCapabilities() {
        val getRemoteCapabilitiesOperation = mock(GetRemoteCapabilitiesOperation::class.java)

        val remoteCapability = TestUtil.createRemoteCapability(
            "ceo@server", 15, 14, 13
        )

        val getRemoteCapabilitiesOperationResult = TestUtil.createRemoteOperationResultMock(
            remoteCapability,
            true
        )

        `when`(getRemoteCapabilitiesOperation.execute(ownCloudClient)).thenReturn(
            getRemoteCapabilitiesOperationResult
        )

        // Get capability from remote datasource
        val remoteOperationResult = ocRemoteCapabilitiesDataSource.getCapabilities(
            getRemoteCapabilitiesOperation
        )

        assertThat(remoteOperationResult, notNullValue())
        assertThat(remoteOperationResult.data, notNullValue())

        val capability = remoteOperationResult.data

        assertEquals(capability.accountName, "ceo@server")
        assertEquals(capability.versionMayor, 15)
        assertEquals(capability.versionMinor, 14)
        assertEquals(capability.versionMicro, 13)
    }
}
