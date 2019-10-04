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

package com.owncloud.android.capabilities.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.utils.LiveDataTestUtil.getValue
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OCCapabilityDaoTest {
    private lateinit var ocCapabilityDao: OCCapabilityDao

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocCapabilityDao = db.capabilityDao()
    }

    @Test
    fun insertCapabilitiesAndRead() {
        ocCapabilityDao.insert(
            listOf(
                TestUtil.createCapability("user1@server", 3, 2, 1, "3.1"),
                TestUtil.createCapability("user2@server", 6, 5, 4, "6.0")
            )
        )

        val capability = getValue(
            ocCapabilityDao.getCapabilityForAccountAsLiveData(
                "user2@server"
            )
        )
        assertThat(capability, notNullValue())
        assertEquals("user2@server", capability.accountName)
        assertEquals(6, capability.versionMayor)
        assertEquals(5, capability.versionMinor)
        assertEquals(4, capability.versionMicro)
        assertEquals("6.0", capability.versionString)
    }

    @Test
    fun getNonExistingCapabilities() {
        ocCapabilityDao.insert(
            TestUtil.createCapability("user@server", 10, 9, 8, "10.1.4")
        )

        val capability = getValue(
            ocCapabilityDao.getCapabilityForAccountAsLiveData(
                "user@server2"
            )
        )
        assertThat(capability, nullValue())
    }

    @Test
    fun replaceCapabilityIfAlreadyExists_exists() {
        ocCapabilityDao.insert(
            TestUtil.createCapability(
                "admin@server",
                3,
                2,
                1,
                "3.7.5",
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
            )
        )

        ocCapabilityDao.replace(
            listOf( // Update capability
                TestUtil.createCapability(
                    "admin@server",
                    3,
                    2,
                    1,
                    "3.7.5",
                    sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
                )
            )
        )

        val capability = getValue(
            ocCapabilityDao.getCapabilityForAccountAsLiveData(
                "admin@server"
            )
        )
        assertThat(capability, notNullValue())
        assertEquals("admin@server", capability.accountName)
        assertEquals(3, capability.versionMayor)
        assertEquals(2, capability.versionMinor)
        assertEquals(1, capability.versionMicro)
        assertEquals("3.7.5", capability.versionString)
        assertEquals(1, capability.filesSharingPublicPasswordEnforced)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        ocCapabilityDao.insert(
            TestUtil.createCapability(
                "cto@server",
                10,
                8,
                6,
                "10.0.2",
                sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE.value
            )
        )

        ocCapabilityDao.replace(
            listOf( // Update capability
                TestUtil.createCapability(
                    "seo@server",
                    14,
                    13,
                    12,
                    "14.3.8",
                    sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.TRUE.value
                )
            )
        )

        val capability1 = getValue(
            ocCapabilityDao.getCapabilityForAccountAsLiveData(
                "cto@server"
            )
        )
        assertThat(capability1, notNullValue())
        assertEquals("cto@server", capability1.accountName)
        assertEquals(10, capability1.versionMayor)
        assertEquals(8, capability1.versionMinor)
        assertEquals(6, capability1.versionMicro)
        assertEquals("10.0.2", capability1.versionString)
        assertEquals(CapabilityBooleanType.FALSE.value, capability1.filesSharingPublicPasswordEnforcedReadOnly)

        // capability2 didn't exist before, it should not replace the old one but got created
        val capability2 = getValue(
            ocCapabilityDao.getCapabilityForAccountAsLiveData(
                "seo@server"
            )
        )
        assertThat(capability2, notNullValue())
        assertEquals("seo@server", capability2.accountName)
        assertEquals(14, capability2.versionMayor)
        assertEquals(13, capability2.versionMinor)
        assertEquals(12, capability2.versionMicro)
        assertEquals("14.3.8", capability2.versionString)
        assertEquals(CapabilityBooleanType.TRUE.value, capability2.filesSharingPublicPasswordEnforcedReadOnly)
    }
}
