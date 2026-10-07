/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.sharing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicOcsShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetOcsShareAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetOcsSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshOcsSharesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.OcsShareViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.testutil.livedata.getEmittedValues
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
class OcsShareViewModelTest {
    private lateinit var shareViewModel: OcsShareViewModel

    private lateinit var getSharesAsLiveDataUseCase: GetOcsSharesAsLiveDataUseCase
    private lateinit var getShareAsLiveDataUseCase: GetOcsShareAsLiveDataUseCase
    private lateinit var refreshSharesFromServerAsyncUseCase: RefreshOcsSharesFromServerAsyncUseCase
    private lateinit var createPrivateShareAsyncUseCase: CreatePrivateOcsShareAsyncUseCase
    private lateinit var editPrivateShareAsyncUseCase: EditPrivateOcsShareAsyncUseCase
    private lateinit var createPublicShareAsyncUseCase: CreatePublicOcsShareAsyncUseCase
    private lateinit var editPublicShareAsyncUseCase: EditPublicOcsShareAsyncUseCase
    private lateinit var deletePublicShareAsyncUseCase: DeleteOcsShareAsyncUseCase
    private lateinit var getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase
    private lateinit var ocContextProvider: ContextProvider

    private val filePath = "/Photos/image.jpg"
    private val testAccountName = OC_ACCOUNT_NAME

    private val sharesLiveData = MutableLiveData<List<OCShare>>()
    private val privateShareLiveData = MutableLiveData<OCShare>()

    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider = CoroutinesDispatcherProvider(
        io = testCoroutineDispatcher,
        main = testCoroutineDispatcher,
        computation = testCoroutineDispatcher
    )

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        ocContextProvider = mockk(relaxed = true)

        // To do: Add tests when is not connected
        every { ocContextProvider.isConnected() } returns true

        Dispatchers.setMain(testCoroutineDispatcher)
        startKoin {
            allowOverride(true)
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

        stopKoin()
        unmockkAll()
    }

    private fun initTest() {
        getSharesAsLiveDataUseCase = spyk(mockkClass(GetOcsSharesAsLiveDataUseCase::class))
        getShareAsLiveDataUseCase = spyk(mockkClass(GetOcsShareAsLiveDataUseCase::class))
        refreshSharesFromServerAsyncUseCase = spyk(mockkClass(RefreshOcsSharesFromServerAsyncUseCase::class))
        createPrivateShareAsyncUseCase = spyk(mockkClass(CreatePrivateOcsShareAsyncUseCase::class))
        editPrivateShareAsyncUseCase = spyk(mockkClass(EditPrivateOcsShareAsyncUseCase::class))
        createPublicShareAsyncUseCase = spyk(mockkClass(CreatePublicOcsShareAsyncUseCase::class))
        editPublicShareAsyncUseCase = spyk(mockkClass(EditPublicOcsShareAsyncUseCase::class))
        deletePublicShareAsyncUseCase = spyk(mockkClass(DeleteOcsShareAsyncUseCase::class))
        getStoredCapabilitiesUseCase = spyk(mockkClass(GetStoredCapabilitiesUseCase::class))

        every { getSharesAsLiveDataUseCase(any()) } returns sharesLiveData
        every { getShareAsLiveDataUseCase(any()) } returns privateShareLiveData
        every { getStoredCapabilitiesUseCase (any()) } returns OC_CAPABILITY


        shareViewModel = OcsShareViewModel(
            filePath,
            testAccountName,
            getSharesAsLiveDataUseCase,
            getShareAsLiveDataUseCase,
            refreshSharesFromServerAsyncUseCase,
            createPrivateShareAsyncUseCase,
            editPrivateShareAsyncUseCase,
            createPublicShareAsyncUseCase,
            editPublicShareAsyncUseCase,
            deletePublicShareAsyncUseCase,
            getStoredCapabilitiesUseCase,
            coroutineDispatcherProvider
        )
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun insertPrivateShareSuccess() {
        insertPrivateShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Success()))
        )
    }

    @Test
    fun insertPrivateShareError() {
        val error = Throwable()

        insertPrivateShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Error(error)))
        )
    }

    private fun insertPrivateShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit?>>>
    ) = runTest {
        initTest()
        coEvery { createPrivateShareAsyncUseCase(any()) } returns useCaseResult

        shareViewModel.insertPrivateShare(
            filePath = OC_SHARE.path,
            shareType = OC_SHARE.shareType,
            shareeName = OC_SHARE.accountOwner,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )
        advanceUntilIdle()
        val emittedValues = shareViewModel.privateShareCreationStatus.getEmittedValues(expectedValues.size)
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) { createPrivateShareAsyncUseCase(any()) }
        coVerify(exactly = 0) { createPublicShareAsyncUseCase(any()) }
    }

    @Test
    fun refreshPrivateShare() {
        initTest()
        coEvery { getShareAsLiveDataUseCase(any()) } returns MutableLiveData(OC_SHARE)

        shareViewModel.refreshPrivateShare(OC_SHARE.remoteId)

        val emittedValues = shareViewModel.privateShare.getLastEmittedValue()
        assertEquals(Event(UIResult.Success(OC_SHARE)), emittedValues)

        coVerify(exactly = 1) { getShareAsLiveDataUseCase(any()) }
    }

    @Test
    fun updatePrivateShareSuccess() {
        updatePrivateShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Loading()))
        )
    }

    @Test
    fun updatePrivateShareError() {
        val error = Throwable()

        updatePrivateShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Error(error)))
        )
    }

    private fun updatePrivateShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) = runTest {
        initTest()
        coEvery { editPrivateShareAsyncUseCase(any()) } returns useCaseResult

        shareViewModel.updatePrivateShare(
            remoteId = OC_SHARE.remoteId,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )
        advanceUntilIdle()
        val emittedValues = shareViewModel.privateShareEditionStatus.getEmittedValues(expectedValues.size)
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) { editPrivateShareAsyncUseCase(any()) }
        coVerify(exactly = 0) { editPublicShareAsyncUseCase(any()) }
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun insertPublicShareSuccess() {
        insertPublicShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Success()))
        )
    }

    @Test
    fun insertPublicShareError() {
        val error = Throwable()

        insertPublicShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Error(error)))
        )
    }

    private fun insertPublicShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) = runTest {
        initTest()
        coEvery { createPublicShareAsyncUseCase(any()) } returns useCaseResult

        shareViewModel.insertPublicShare(
            filePath = OC_SHARE.path,
            name = "Photos 2 link",
            password = "1234",
            expirationTimeInMillis = -1,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )
        advanceUntilIdle()
        val emittedValues = shareViewModel.publicShareCreationStatus.getEmittedValues(expectedValues.size)
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 0) { createPrivateShareAsyncUseCase(any()) }
        coVerify(exactly = 1) { createPublicShareAsyncUseCase(any()) }
    }

    @Test
    fun updatePublicShareSuccess() {
        updatePublicShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Success()))
        )
    }

    @Test
    fun updatePublicShareError() {
        val error = Throwable()

        updatePublicShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Error(error)))
        )
    }

    private fun updatePublicShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) = runTest {
        initTest()
        coEvery { editPublicShareAsyncUseCase(any()) } returns useCaseResult

        shareViewModel.updatePublicShare(
            remoteId = OC_SHARE.remoteId,
            name = "Photos 2 link",
            password = "1234",
            expirationDateInMillis = -1,
            permissions = -1,
            accountName = "Carlos"
        )
        advanceUntilIdle()
        val emittedValues = shareViewModel.publicShareEditionStatus.getEmittedValues(expectedValues.size)
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 0) { editPrivateShareAsyncUseCase(any()) }
        coVerify(exactly = 1) { editPublicShareAsyncUseCase(any()) }
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun deletePublicShareSuccess() {
        deleteShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Loading()))
        )
    }

    @Test
    fun deletePublicShareError() {
        val error = Throwable()

        deleteShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Error(error)))
        )
    }

    private fun deleteShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) = runTest {
        initTest()
        coEvery { deletePublicShareAsyncUseCase(any()) } returns useCaseResult

        shareViewModel.deleteShare(remoteId = OC_SHARE.remoteId)
        advanceUntilIdle()
        val emittedValues = shareViewModel.shareDeletionStatus.getEmittedValues(expectedValues.size)

        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) {
            deletePublicShareAsyncUseCase(any())
        }
    }
}
