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

package com.owncloud.android.data.capabilities.datasources

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.data.utils.LiveDataTestUtil.getValue
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

        val capabilityAsLiveData: MutableLiveData<OCCapabilityEntity> = MutableLiveData()
        capabilityAsLiveData.value = DataTestUtil.createCapability(
            "user@server1", 5, 4, 3
        )

        `when`(
            ocCapabilityDao.getCapabilityForAccount(
                "user@server1"
            )
        ).thenReturn(
            capabilityAsLiveData
        )

        val newCapabilityAsLiveData: MutableLiveData<OCCapabilityEntity> = MutableLiveData()
        newCapabilityAsLiveData.value = DataTestUtil.createCapability(
            "user@server2", 2, 1, 0
        )

        `when`(
            ocCapabilityDao.getCapabilityForAccount(
                "user@server2"
            )
        ).thenReturn(
            newCapabilityAsLiveData
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        ocLocalCapabilitiesDataSource =
            OCLocalCapabilitiesDataSource(context, ocCapabilityDao)
    }

    @Test
    fun readLocalCapability() {
        val capability = getValue(
            ocLocalCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "user@server1"
            )
        )
        assertEquals("user@server1", capability.accountName)
        assertEquals(5, capability.versionMayor)
        assertEquals(4, capability.versionMinor)
        assertEquals(3, capability.versionMicro)
    }

    @Test
    fun insertCapabilityAndRead() {
        ocLocalCapabilitiesDataSource.insert(
            listOf(
                DataTestUtil.createCapability(
                    "user@server1", 7, 6, 5
                ),
                DataTestUtil.createCapability(
                    "user@server2", 2, 1, 0
                )
            )
        )

        val capability = getValue(
            ocLocalCapabilitiesDataSource.getCapabilityForAccountAsLiveData(
                "user@server2"
            )
        )
        assertEquals("user@server2", capability.accountName)
        assertEquals(2, capability.versionMayor)
        assertEquals(1, capability.versionMinor)
        assertEquals(0, capability.versionMicro)
    }
}
