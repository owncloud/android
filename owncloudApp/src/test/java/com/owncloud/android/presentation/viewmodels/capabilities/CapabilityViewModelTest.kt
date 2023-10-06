/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.capabilities

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class CapabilityViewModelTest {
    private lateinit var capabilityViewModel: CapabilityViewModel

    private lateinit var getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase
    private lateinit var refreshCapabilitiesFromServerUseCase: RefreshCapabilitiesFromServerAsyncUseCase
    private lateinit var ocContextProvider: ContextProvider

    private val capabilitiesLiveData = MutableLiveData<OCCapability>()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider = CoroutinesDispatcherProvider(
        io = testCoroutineDispatcher,
        main = testCoroutineDispatcher,
        computation = testCoroutineDispatcher
    )

    private var testAccountName = OC_ACCOUNT_NAME

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(testCoroutineDispatcher)
        ocContextProvider = mockk(relaxed = true)

        //TODO: Add tests when is not connected
        every { ocContextProvider.isConnected() } returns true

        Dispatchers.setMain(testCoroutineDispatcher)
        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    factory {
                        ocContextProvider
                    }
                })
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()

        stopKoin()
        unmockkAll()
    }

    private fun initTest() {
        getCapabilitiesAsLiveDataUseCase = spyk(mockkClass(GetCapabilitiesAsLiveDataUseCase::class))
        refreshCapabilitiesFromServerUseCase = spyk(mockkClass(RefreshCapabilitiesFromServerAsyncUseCase::class))

        every { getCapabilitiesAsLiveDataUseCase(any()) } returns capabilitiesLiveData

        capabilityViewModel = CapabilityViewModel(
            accountName = testAccountName,
            getCapabilitiesAsLiveDataUseCase = getCapabilitiesAsLiveDataUseCase,
            refreshCapabilitiesFromServerAsyncUseCase = refreshCapabilitiesFromServerUseCase,
            coroutineDispatcherProvider = coroutineDispatcherProvider
        )
    }

    @Test
    fun getCapabilitiesAsLiveDataWithData() {
        initTest()

        val capability = OC_CAPABILITY.copy(accountName = testAccountName)

        getCapabilitiesAsLiveDataVerification(
            valueToTest = capability,
            expectedValue = Event(UIResult.Success(capability))
        )
    }

    @Test
    fun getCapabilitiesAsLiveDataWithoutData() {
        initTest()

        getCapabilitiesAsLiveDataVerification(
            valueToTest = null,
            expectedValue = Event(UIResult.Success(null))
        )
    }

    private fun getCapabilitiesAsLiveDataVerification(
        valueToTest: OCCapability?,
        expectedValue: Event<UIResult<OCCapability>>?
    ) {
        capabilitiesLiveData.postValue(valueToTest)

        val value = capabilityViewModel.capabilities.getLastEmittedValue()
        assertEquals(expectedValue, value)

        // Calls performed during OCCapabilityViewModel initialization
        verify(exactly = 1) { getCapabilitiesAsLiveDataUseCase(any()) }
        verify(exactly = 1) { refreshCapabilitiesFromServerUseCase(any()) }
    }

    @Test
    fun fetchCapabilitiesSuccess() {
        fetchCapabilitiesVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValue = Event(UIResult.Loading())
        )
    }

    @Test
    fun fetchCapabilitiesError() {
        val error = Throwable()
        fetchCapabilitiesVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValue = Event(UIResult.Error(error))
        )
    }

    private fun fetchCapabilitiesVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValue: Event<UIResult<Unit>?>
    ) {
        initTest()
        coEvery { refreshCapabilitiesFromServerUseCase(any()) } returns useCaseResult

        capabilityViewModel.refreshCapabilitiesFromNetwork()

        val value = capabilityViewModel.capabilities.getLastEmittedValue()
        assertEquals(expectedValue, value)

        coVerify(exactly = 2) { refreshCapabilitiesFromServerUseCase(any()) }
        verify(exactly = 1) { getCapabilitiesAsLiveDataUseCase(any()) }
    }
}
