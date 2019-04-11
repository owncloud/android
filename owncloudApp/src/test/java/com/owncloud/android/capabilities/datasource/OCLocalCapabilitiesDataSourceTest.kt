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

package com.owncloud.android.capabilities.datasource

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.db.OCCapabilityDao
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.utils.LiveDataTestUtil.getValue
import com.owncloud.android.utils.TestUtil
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCLocalCapabilitiesDataSourceTest {
    private lateinit var ocLocalCapabilitiesDataSource: OCLocalCapabilitiesDataSource
    private val ocCapabilityDao = mock(OCCapabilityDao::class.java)

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val db = mock(OwncloudDatabase::class.java)
        `when`(db.capabilityDao()).thenReturn(ocCapabilityDao)

        val capabilityAsLiveData: MutableLiveData<OCCapability> = MutableLiveData()
        capabilityAsLiveData.value = TestUtil.createCapability(
            "user@server1", 5, 4, 3
        )

        `when`(
            ocCapabilityDao.getCapabilityForAccount(
                "user@server1"
            )
        ).thenReturn(
            capabilityAsLiveData
        )

        val newCapabilityAsLiveData: MutableLiveData<OCCapability> = MutableLiveData()
        newCapabilityAsLiveData.value = TestUtil.createCapability(
            "user@server2", 2, 1, 0
        )

        `when`(
            ocCapabilityDao.getCapabilityForAccount(
                "user@server2"
            )
        ).thenReturn(
            newCapabilityAsLiveData
        )

        ocLocalCapabilitiesDataSource = OCLocalCapabilitiesDataSource(ocCapabilityDao)
    }

    @Test
    fun readLocalCapability() {
        val capability = getValue(
            ocLocalCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "user@server1"
            )
        )

        assertEquals(capability.accountName, "user@server1")
        assertEquals(capability.versionMayor, 5)
        assertEquals(capability.versionMinor, 4)
        assertEquals(capability.versionMicro, 3)
    }

    @Test
    fun insertCapabilityAndRead() {
        ocLocalCapabilitiesDataSource.insert(
            TestUtil.createCapability(
                "user@server2", 2, 1, 0
            )
        )

        val capability = getValue(
            ocLocalCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "user@server2"
            )
        )

        assertEquals(capability.accountName, "user@server2")
        assertEquals(capability.versionMayor, 2)
        assertEquals(capability.versionMinor, 1)
        assertEquals(capability.versionMicro, 0)
    }
}
