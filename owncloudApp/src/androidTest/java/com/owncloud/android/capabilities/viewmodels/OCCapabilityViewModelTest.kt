/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
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

package com.owncloud.android.capabilities.viewmodels

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.livedata.TIMEOUT_TEST_LONG
import com.owncloud.android.testutil.livedata.getOrAwaitValues
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OCCapabilityViewModelTest {
    private lateinit var ocCapabilityViewModel: OCCapabilityViewModel

    private lateinit var getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase
    private lateinit var refreshCapabilitiesFromServerUseCase: RefreshCapabilitiesFromServerAsyncUseCase

    private val capabilityLiveData = MutableLiveData<OCCapability>()

    private var testAccount: Account = OC_ACCOUNT

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun initTest() {
        getStoredCapabilitiesUseCase = spyk(mockkClass(GetStoredCapabilitiesUseCase::class))
        refreshCapabilitiesFromServerUseCase = spyk(mockkClass(RefreshCapabilitiesFromServerAsyncUseCase::class))

        every { getStoredCapabilitiesUseCase.execute(any()) } returns capabilityLiveData

        ocCapabilityViewModel = OCCapabilityViewModel(
            accountName = testAccount.name,
            getStoredCapabilitiesUseCase = getStoredCapabilitiesUseCase,
            refreshCapabilitiesFromServerUseCase = refreshCapabilitiesFromServerUseCase
        )
    }

    @Test
    fun getStoredCapabilitiesWithData() {
        initTest()

        val capability = OC_CAPABILITY.copy(accountName = testAccount.name)

        getStoredCapabilitiesVerification(
            valueToTest = capability,
            expectedValue = UIResult.Success(capability)
        )
    }

    @Test
    fun getStoredCapabilitiesWithoutData() {
        initTest()

        getStoredCapabilitiesVerification(
            valueToTest = null,
            expectedValue = null
        )
    }

    @Test
    fun fetchCapabilitiesLoading() {
        initTest()

        fetchCapabilitiesVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading()
        )
    }

    @Test
    fun fetchCapabilitiesError() {
        initTest()

        val error = Throwable()
        fetchCapabilitiesVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error),
            expectedOnPosition = 2
        )
    }

    @Test
    fun fetchCapabilitiesSuccess() {
        initTest()

        //Expect a null since we are mocking refreshCapabilities and we are not storing new capabilities on db
        fetchCapabilitiesVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = null,
            expectedOnPosition = 2
        )
    }

    private fun getStoredCapabilitiesVerification(
        valueToTest: OCCapability?,
        expectedValue: UIResult<OCCapability>?,
        expectedOnPosition: Int = 1
    ) {
        capabilityLiveData.postValue(valueToTest)

        val value = ocCapabilityViewModel.capabilities.getOrAwaitValues()
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 0) { refreshCapabilitiesFromServerUseCase.execute(any()) }
        verify(exactly = 1) { getStoredCapabilitiesUseCase.execute(any()) }
    }

    private fun fetchCapabilitiesVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { refreshCapabilitiesFromServerUseCase.execute(any()) } returns valueToTest

        ocCapabilityViewModel.refreshCapabilitiesFromNetwork()

        val value = ocCapabilityViewModel.capabilities.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { refreshCapabilitiesFromServerUseCase.execute(any()) }
        //Just once on init
        verify(exactly = 1) { getStoredCapabilitiesUseCase.execute(any()) }
    }
}
