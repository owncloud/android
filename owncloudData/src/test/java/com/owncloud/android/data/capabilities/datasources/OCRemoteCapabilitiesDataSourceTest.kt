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

import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation
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
        ocRemoteCapabilitiesDataSource =
            OCRemoteCapabilitiesDataSource(ownCloudClient)
    }

    @Test
    fun readRemoteCapabilities() {
        val getRemoteCapabilitiesOperation = mock(GetRemoteCapabilitiesOperation::class.java)

        val remoteCapability = DataTestUtil.createRemoteCapability(
            "ceo@server", 15, 14, 13
        )

        val getRemoteCapabilitiesOperationResult = DataTestUtil.createRemoteOperationResultMock(
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

        assertEquals("ceo@server", capability.accountName)
        assertEquals(15, capability.versionMayor)
        assertEquals(14, capability.versionMinor)
        assertEquals(13, capability.versionMicro)
    }
}
