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

package com.owncloud.android.capabilities.viewmodel

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.repository.OCCapabilityRepository
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.vo.Resource
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class OCCapabilityViewModelTest {
    private var testAccount: Account = TestUtil.createAccount("admin@server", "test")
    private lateinit var capability: OCCapability

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        capability = TestUtil.createCapability("admin@server", 2, 1, 0)
    }

    @Test
    fun loadCapabilityAsLiveData() {
        val ocCapabilityRepository = mock(OCCapabilityRepository::class.java)

        `when`(
            ocCapabilityRepository.getCapabilityForAccountAsLiveData(
                "admin@server"
            )
        ).thenReturn(
            MutableLiveData<Resource<OCCapability>>().apply {
                value = Resource.success(capability)
            }
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val ocCapabilityViewModel = OCCapabilityViewModel(
            context,
            account = testAccount,
            capabilityRepository = ocCapabilityRepository
        )

        val resource: Resource<OCCapability>? = ocCapabilityViewModel.getCapabilityForAccountAsLiveData().value
        val capability: OCCapability? = resource?.data

        assertEquals("admin@server", capability?.accountName)
        assertEquals(2, capability?.versionMayor)
        assertEquals(1, capability?.versionMinor)
        assertEquals(0, capability?.versionMicro)
    }

    @Test
    fun loadCapability() {
        val ocCapabilityRepository = mock(OCCapabilityRepository::class.java)

        `when`(
            ocCapabilityRepository.getStoredCapabilityForAccount(
                "admin@server"
            )
        ).thenReturn(
            capability
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val ocCapabilityViewModel = OCCapabilityViewModel(
            context,
            account = testAccount,
            capabilityRepository = ocCapabilityRepository
        )

        val capability: OCCapability? = ocCapabilityViewModel.getStoredCapabilityForAccount()

        assertEquals("admin@server", capability?.accountName)
        assertEquals(2, capability?.versionMayor)
        assertEquals(1, capability?.versionMinor)
        assertEquals(0, capability?.versionMicro)
    }
}
