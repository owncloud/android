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

package com.owncloud.android.presentation.sharing.shares.viewmodels

import android.accounts.Account
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
import com.owncloud.android.domain.utils.DomainTestUtil.DUMMY_SHARE
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.utils.AppTestUtil
import com.owncloud.android.utils.TIMEOUT_TEST_LONG
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

    private var testAccount: Account = AppTestUtil.createAccount("admin@server", "test")

    private val sharesLiveData = MutableLiveData<List<OCShare>?>()
    private val privateShareLiveData = MutableLiveData<OCShare>()

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @After
    fun tearDown() {
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

        ocShareViewModel = OCShareViewModel(
            filePath,
            testAccount.name,
            getSharesAsLiveDataUseCase,
            getShareAsLiveDataUseCase,
            refreshSharesFromServerAsyncUseCase,
            createPrivateShareAsyncUseCase,
            editPrivateShareAsyncUseCase,
            createPublicShareAsyncUseCase,
            editPublicShareAsyncUseCase,
            deletePublicShareAsyncUseCase
        )
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun insertPrivateShareLoading() {
        initTest()

        insertPrivateShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(),
            expectedOnPosition = 1
        )
    }

    @Test
    fun insertPrivateShareSuccess() {
        initTest()

        insertPrivateShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(),
            expectedOnPosition = 2
        )
    }

    @Test
    fun insertPrivateShareError() {
        initTest()

        val error = Throwable()

        insertPrivateShareVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error),
            expectedOnPosition = 2
        )
    }

    @Test
    fun updatePrivateShareLoading() {
        initTest()

        updatePrivateShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(),
            expectedOnPosition = 1
        )
    }

    @Test
    fun updatePrivateShareSuccess() {
        initTest()

        updatePrivateShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(),
            expectedOnPosition = 2
        )
    }

    @Test
    fun refreshPrivateShareSuccess() {
        initTest()

        val ocShare = DUMMY_SHARE.copy(id = 123, name = "PhotoLink")
        privateShareLiveData.postValue(ocShare)

        refreshPrivateShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(ocShare),
            expectedOnPosition = 1
        )
    }

    private fun refreshPrivateShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<OCShare>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { createPrivateShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.refreshPrivateShare(DUMMY_SHARE.remoteId)

        val value = ocShareViewModel.privateShare.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        verify(exactly = 1) { getShareAsLiveDataUseCase.execute(GetShareAsLiveDataUseCase.Params(DUMMY_SHARE.remoteId)) }
        // Just once on init
        verify(exactly = 1) { getSharesAsLiveDataUseCase.execute(any()) }
    }

    private fun insertPrivateShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { createPrivateShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.insertPrivateShare(
            filePath = DUMMY_SHARE.path,
            shareType = DUMMY_SHARE.shareType,
            shareeName = DUMMY_SHARE.accountOwner,
            permissions = DUMMY_SHARE.permissions,
            accountName = DUMMY_SHARE.accountOwner
        )

        val value = ocShareViewModel.privateShareCreationStatus.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { createPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 0) { createPublicShareAsyncUseCase.execute(any()) }
    }

    private fun updatePrivateShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { editPrivateShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.updatePrivateShare(
            remoteId = DUMMY_SHARE.remoteId,
            permissions = DUMMY_SHARE.permissions,
            accountName = DUMMY_SHARE.accountOwner
        )

        val value = ocShareViewModel.privateShareEditionStatus.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { editPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 0) { editPublicShareAsyncUseCase.execute(any()) }
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun insertPublicShareLoading() {
        initTest()

        insertPublicShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(),
            expectedOnPosition = 1
        )
    }

    @Test
    fun insertPublicShareSuccess() {
        initTest()

        insertPublicShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(),
            expectedOnPosition = 2
        )
    }

    @Test
    fun insertPublicShareError() {
        initTest()

        val error = Throwable()

        insertPublicShareVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error),
            expectedOnPosition = 2
        )
    }

    @Test
    fun updatePublicShareLoading() {
        initTest()

        updatePublicShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(),
            expectedOnPosition = 1
        )
    }

    @Test
    fun updatePublicShareSuccess() {
        initTest()

        updatePublicShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(),
            expectedOnPosition = 2
        )
    }

    @Test
    fun updatePublicShareError() {
        initTest()

        val error = Throwable()

        updatePublicShareVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error),
            expectedOnPosition = 2
        )
    }

    private fun insertPublicShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { createPublicShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.insertPublicShare(
            filePath = DUMMY_SHARE.path,
            name = "Photos 2 link",
            password = "1234",
            expirationTimeInMillis = -1,
            publicUpload = false,
            permissions = DUMMY_SHARE.permissions,
            accountName = DUMMY_SHARE.accountOwner
        )

        val value = ocShareViewModel.publicShareCreationStatus.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 0) { createPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { createPublicShareAsyncUseCase.execute(any()) }
    }

    private fun updatePublicShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { editPublicShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.updatePublicShare(
            remoteId = 1,
            name = "Photos 2 link",
            password = "1234",
            expirationDateInMillis = -1,
            publicUpload = false,
            permissions = -1,
            accountName = "Carlos"
        )

        val value = ocShareViewModel.publicShareEditionStatus.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 0) { editPrivateShareAsyncUseCase.execute(any()) }
        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { editPublicShareAsyncUseCase.execute(any()) }
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun deletePublicShareLoading() {
        initTest()

        deleteShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(),
            expectedOnPosition = 1
        )
    }

    @Test
    fun deletePublicShareSuccess() {
        initTest()

        deleteShareVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Success(),
            expectedOnPosition = 2
        )
    }

    @Test
    fun deletePublicShareError() {
        initTest()

        val error = Throwable()

        deleteShareVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error),
            expectedOnPosition = 2
        )
    }

    @Test
    fun getSharesAsLiveDataLoading() {
        initTest()

        getSharesAsLiveDataVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = UIResult.Loading(sharesLiveData.value),
            expectedOnPosition = 1
        )
    }

    @Test
    fun getSharesAsLiveDataError() {
        initTest()

        val error = Throwable()

        getSharesAsLiveDataVerification(
            valueToTest = UseCaseResult.Error(error),
            expectedValue = UIResult.Error(error, sharesLiveData.value),
            expectedOnPosition = 2
        )
    }

    @Test
    fun getSharesAsLiveDataWithData() {
        initTest()

        getSharesAsLiveDataVerification(
            valueToTest = UseCaseResult.Success(Unit),
            expectedValue = null,
            expectedOnPosition = 2
        )
    }

    private fun getSharesAsLiveDataVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<List<OCShare>>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { refreshSharesFromServerAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.refreshSharesFromNetwork()

        val value = ocShareViewModel.shares.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) { refreshSharesFromServerAsyncUseCase.execute(any()) }
    }

    private fun deleteShareVerification(
        valueToTest: UseCaseResult<Unit>,
        expectedValue: UIResult<Unit>?,
        expectedOnPosition: Int = 1
    ) {
        coEvery { deletePublicShareAsyncUseCase.execute(any()) } returns valueToTest

        ocShareViewModel.deleteShare(
            remoteId = DUMMY_SHARE.remoteId
        )

        val value = ocShareViewModel.shareDeletionStatus.getOrAwaitValues(expectedOnPosition)
        assertEquals(expectedValue, value[expectedOnPosition - 1])

        coVerify(exactly = 1, timeout = TIMEOUT_TEST_LONG) {
            deletePublicShareAsyncUseCase.execute(any())
        }
    }
}
