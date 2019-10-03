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
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.mapper.OCCapabilityMapper
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.utils.DataTestUtil
import com.owncloud.android.data.utils.LiveDataTestUtil.getValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class OCLocalCapabilitiesDataSourceTest {
    private lateinit var ocLocalCapabilitiesDataSource: OCLocalCapabilitiesDataSource
    private val ocCapabilityDao = mockk<OCCapabilityDao>(relaxed = true)

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val db = mockkClass(OwncloudDatabase::class)

        every {
            db.capabilityDao()
        } returns ocCapabilityDao

        val capabilityAsLiveData: MutableLiveData<OCCapabilityEntity> = MutableLiveData()
        capabilityAsLiveData.value = DataTestUtil.createCapabilityEntity(
            "user@server1", 5, 4, 3
        )

        every {
            ocCapabilityDao.getCapabilitiesForAccountAsLiveData(
                "user@server1"
            )
        } returns capabilityAsLiveData

        val newCapabilityAsLiveData: MutableLiveData<OCCapabilityEntity> = MutableLiveData()
        newCapabilityAsLiveData.value = DataTestUtil.createCapabilityEntity(
            "user@server2", 2, 1, 0
        )

        every {
            ocCapabilityDao.getCapabilitiesForAccountAsLiveData(
                "user@server2"
            )
        } returns newCapabilityAsLiveData

        ocLocalCapabilitiesDataSource =
            OCLocalCapabilitiesDataSource(
                ocCapabilityDao,
                OCCapabilityMapper()
            )
    }

    @Test
    fun readLocalCapability() {
        val capability = getValue(
            ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(
                "user@server1"
            )
        )
        assertEquals("user@server1", capability?.accountName)
        assertEquals(5, capability?.versionMayor)
        assertEquals(4, capability?.versionMinor)
        assertEquals(3, capability?.versionMicro)
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
            ocLocalCapabilitiesDataSource.getCapabilitiesForAccountAsLiveData(
                "user@server2"
            )
        )
        assertEquals("user@server2", capability?.accountName)
        assertEquals(2, capability?.versionMayor)
        assertEquals(1, capability?.versionMinor)
        assertEquals(0, capability?.versionMicro)
    }
}
