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

package com.owncloud.android.presentation.viewmodels.sharing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.usecases.CreatePrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.CreatePublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.DeleteShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPrivateShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetShareAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetSharesAsLiveDataUseCase
import com.owncloud.android.domain.sharing.shares.usecases.RefreshSharesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.testutil.livedata.getEmittedValues
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkAll
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

@ExperimentalCoroutinesApi
class OCShareViewModelTest {
    private lateinit var ocShareViewModel: OCShareViewModel

    private lateinit var getSharesAsLiveDataUseCase: GetSharesAsLiveDataUseCase
    private lateinit var getShareAsLiveDataUseCase: GetShareAsLiveDataUseCase
    private lateinit var refreshSharesFromServerAsyncUseCase: RefreshSharesFromServerAsyncUseCase
    private lateinit var createPrivateShareAsyncUseCase: CreatePrivateShareAsyncUseCase
    private lateinit var editPrivateShareAsyncUseCase: EditPrivateShareAsyncUseCase
    private lateinit var createPublicShareAsyncUseCase: CreatePublicShareAsyncUseCase
    private lateinit var editPublicShareAsyncUseCase: EditPublicShareAsyncUseCase
    private lateinit var deletePublicShareAsyncUseCase: DeleteShareAsyncUseCase

    private val filePath = "/Photos/image.jpg"
    private val testAccountName = OC_ACCOUNT_NAME

    private val sharesLiveData = MutableLiveData<List<OCShare>>()
    private val privateShareLiveData = MutableLiveData<OCShare>()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()
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
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()

        unmockkAll()
    }

    private fun initTest() {
        getSharesAsLiveDataUseCase = spyk(mockkClass(GetSharesAsLiveDataUseCase::class))
        getShareAsLiveDataUseCase = spyk(mockkClass(GetShareAsLiveDataUseCase::class))
        refreshSharesFromServerAsyncUseCase = spyk(mockkClass(RefreshSharesFromServerAsyncUseCase::class))
        createPrivateShareAsyncUseCase = spyk(mockkClass(CreatePrivateShareAsyncUseCase::class))
        editPrivateShareAsyncUseCase = spyk(mockkClass(EditPrivateShareAsyncUseCase::class))
        createPublicShareAsyncUseCase = spyk(mockkClass(CreatePublicShareAsyncUseCase::class))
        editPublicShareAsyncUseCase = spyk(mockkClass(EditPublicShareAsyncUseCase::class))
        deletePublicShareAsyncUseCase = spyk(mockkClass(DeleteShareAsyncUseCase::class))

        every { getSharesAsLiveDataUseCase.execute(any()) } returns sharesLiveData
        every { getShareAsLiveDataUseCase.execute(any()) } returns privateShareLiveData

        testCoroutineDispatcher.pauseDispatcher()

        ocShareViewModel = OCShareViewModel(
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
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Success()))
        )
    }

    @Test
    fun insertPrivateShareError() {
        val error = Throwable()

        insertPrivateShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Error(error)))
        )
    }

    private fun insertPrivateShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit?>>>
    ) {
        initTest()
        coEvery { createPrivateShareAsyncUseCase.execute(any()) } returns useCaseResult

        ocShareViewModel.insertPrivateShare(
            filePath = OC_SHARE.path,
            shareType = OC_SHARE.shareType,
            shareeName = OC_SHARE.accountOwner,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )

        val emittedValues = ocShareViewModel.privateShareCreationStatus.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) { createPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 0) { createPublicShareAsyncUseCase.execute(any()) }
    }

    @Test
    fun refreshPrivateShare() {
        initTest()
        coEvery { getShareAsLiveDataUseCase.execute(any()) } returns MutableLiveData(OC_SHARE)

        ocShareViewModel.refreshPrivateShare(OC_SHARE.remoteId)

        val emittedValues = ocShareViewModel.privateShare.getLastEmittedValue {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(Event(UIResult.Success(OC_SHARE)), emittedValues)

        coVerify(exactly = 1) { getShareAsLiveDataUseCase.execute(any()) }
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
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Error(error)))
        )
    }

    private fun updatePrivateShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) {
        initTest()
        coEvery { editPrivateShareAsyncUseCase.execute(any()) } returns useCaseResult

        ocShareViewModel.updatePrivateShare(
            remoteId = OC_SHARE.remoteId,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )

        val emittedValues = ocShareViewModel.privateShareEditionStatus.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) { editPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 0) { editPublicShareAsyncUseCase.execute(any()) }
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun insertPublicShareSuccess() {
        insertPublicShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Success()))
        )
    }

    @Test
    fun insertPublicShareError() {
        val error = Throwable()

        insertPublicShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Error(error)))
        )
    }

    private fun insertPublicShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) {
        initTest()
        coEvery { createPublicShareAsyncUseCase.execute(any()) } returns useCaseResult

        ocShareViewModel.insertPublicShare(
            filePath = OC_SHARE.path,
            name = "Photos 2 link",
            password = "1234",
            expirationTimeInMillis = -1,
            publicUpload = false,
            permissions = OC_SHARE.permissions,
            accountName = OC_SHARE.accountOwner
        )

        val emittedValues = ocShareViewModel.publicShareCreationStatus.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 0) { createPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 1) { createPublicShareAsyncUseCase.execute(any()) }
    }

    @Test
    fun updatePublicShareSuccess() {
        updatePublicShareVerification(
            useCaseResult = UseCaseResult.Success(Unit),
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Success()))
        )
    }

    @Test
    fun updatePublicShareError() {
        val error = Throwable()

        updatePublicShareVerification(
            useCaseResult = UseCaseResult.Error(error),
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Error(error)))
        )
    }

    private fun updatePublicShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) {
        initTest()
        coEvery { editPublicShareAsyncUseCase.execute(any()) } returns useCaseResult

        ocShareViewModel.updatePublicShare(
            remoteId = 1,
            name = "Photos 2 link",
            password = "1234",
            expirationDateInMillis = -1,
            publicUpload = false,
            permissions = -1,
            accountName = "Carlos"
        )

        val emittedValues = ocShareViewModel.publicShareEditionStatus.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 0) { editPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 1) { editPublicShareAsyncUseCase.execute(any()) }
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
            expectedValues = listOf(Event(UIResult.Loading()), Event(UIResult.Error(error)))
        )
    }

    private fun deleteShareVerification(
        useCaseResult: UseCaseResult<Unit>,
        expectedValues: List<Event<UIResult<Unit>?>>
    ) {
        initTest()
        coEvery { deletePublicShareAsyncUseCase.execute(any()) } returns useCaseResult

        ocShareViewModel.deleteShare(remoteId = OC_SHARE.remoteId)

        val emittedValues = ocShareViewModel.shareDeletionStatus.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }

        assertEquals(expectedValues, emittedValues)

        coVerify(exactly = 1) {
            deletePublicShareAsyncUseCase.execute(any())
        }
    }
}
