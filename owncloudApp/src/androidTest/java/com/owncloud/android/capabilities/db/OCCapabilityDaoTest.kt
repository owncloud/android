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
import com.owncloud.android.lib.resources.shares.ShareType
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
            ocCapabilityDao.getCapabilityForAccount(
                "user2@server"
            )
        )

        assertThat(capability, notNullValue())
        assertEquals(capability.accountName, "user2@server")
        assertEquals(capability.versionMayor, 6)
        assertEquals(capability.versionMinor, 5)
        assertEquals(capability.versionMicro, 4)
        assertEquals(capability.versionString, "6.0")
    }

    @Test
    fun getNonExistingCapabilities() {
        ocCapabilityDao.insert(
            listOf(
                TestUtil.createCapability("user@server", 10, 9, 8, "10.1.4")
            )
        )

        val capability = getValue(
            ocCapabilityDao.getCapabilityForAccount(
                "user@server2"
            )
        )

        assertThat(capability, nullValue())
    }

    @Test
    fun replaceCapabilityIfAlreadyExists_exists() {
        ocCapabilityDao.insert(
            listOf(
                TestUtil.createCapability(
                    "admin@server",
                    3,
                    2,
                    1,
                    "3.7.5",
                    sharingPublicPasswordEnforced = 0
                )
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
                    sharingPublicPasswordEnforced = 1
                )
            )
        )

        val capability = getValue(
            ocCapabilityDao.getCapabilityForAccount(
                "admin@server"
            )
        )

        assertThat(capability, notNullValue())
        assertEquals(capability.accountName, "admin@server")
        assertEquals(capability.versionMayor, 3)
        assertEquals(capability.versionMinor, 2)
        assertEquals(capability.versionMicro, 1)
        assertEquals(capability.versionString, "3.7.5")
        assertEquals(capability.filesSharingPublicPasswordEnforced, 1)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        ocCapabilityDao.insert(
            listOf(
                TestUtil.createCapability(
                    "cto@server",
                    10,
                    8,
                    6,
                    "10.0.2",
                    sharingPublicPasswordEnforcedReadOnly = 0
                )
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
                    sharingPublicPasswordEnforcedReadOnly = 1
                )
            )
        )

        val capability1 = getValue(
            ocCapabilityDao.getCapabilityForAccount(
                "cto@server"
            )
        )

        assertThat(capability1, notNullValue())
        assertEquals(capability1.accountName, "cto@server")
        assertEquals(capability1.versionMayor, 10)
        assertEquals(capability1.versionMinor, 8)
        assertEquals(capability1.versionMicro, 6)
        assertEquals(capability1.versionString, "10.0.2")
        assertEquals(capability1.filesSharingPublicPasswordEnforcedReadOnly, 0)

        // capability2 link didn't exist before, it should not replace the old one but be created
        val capability2 = getValue(
            ocCapabilityDao.getCapabilityForAccount(
                "seo@server"
            )
        )

        assertThat(capability2, notNullValue())
        assertEquals(capability2.accountName, "seo@server")
        assertEquals(capability2.versionMayor, 14)
        assertEquals(capability2.versionMinor, 13)
        assertEquals(capability2.versionMicro, 12)
        assertEquals(capability2.versionString, "14.3.8")
        assertEquals(capability2.filesSharingPublicPasswordEnforcedReadOnly, 1)
    }
}
