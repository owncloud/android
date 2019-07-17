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

package com.owncloud.android.presentation.sharing.shares.viewmodels

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.DataResult
import com.owncloud.android.data.Status
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.utils.AppTestUtil
import io.mockk.every
import io.mockk.mockkClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OCShareViewModelTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val filePath = "/Photos/image.jpg"

    private var testAccount: Account = AppTestUtil.createAccount("admin@server", "test")
    private var ocShareRepository: OCShareRepository = mockkClass(OCShareRepository::class)

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun loadPrivateShares() {
        val privateShares = mutableListOf(
            AppTestUtil.createPrivateShare(
                path = filePath,
                isFolder = false,
                shareWith = "username1",
                sharedWithDisplayName = "Tim"
            ),
            AppTestUtil.createPrivateShare(
                path = filePath,
                isFolder = false,
                shareWith = "username2",
                sharedWithDisplayName = "Tom"
            )
        )

        every { ocShareRepository.refreshShares(filePath) } returns
                MutableLiveData<DataResult<List<OCShareEntity>>>().apply {
                    value = DataResult.success(privateShares)
                }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<List<OCShareEntity>>? = ocShareViewModel.getPrivateShares(filePath).value
        assertPrivateShareParameters(resource?.data)
    }

    @Test
    fun insertPrivateShare() {
        every {
            ocShareRepository.insertPrivateShare(
                filePath,
                ShareType.GROUP,
                "user",
                -1
            )
        } returns MutableLiveData<DataResult<Unit>>().apply {
            value = DataResult.success()
        }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<Unit>? = ocShareViewModel.insertPrivateShare(
            filePath,
            ShareType.GROUP,
            "user",
            -1
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    @Test
    fun updatePrivateShare() {
        `when`(
            ocShareRepository.updatePrivateShare(
                1,
                17
            )
        ).thenReturn(
            MutableLiveData<Resource<Unit>>().apply {
                value = Resource.success()
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<Unit>? = ocShareViewModel.updatePrivateShare(
            1,
            17
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    private fun assertPrivateShareParameters(shares: List<OCShareEntity>?) {
        assertCommonShareParameters(shares)

        assertEquals("username1", shares?.get(0)?.shareWith)
        assertEquals("Tim", shares?.get(0)?.sharedWithDisplayName)

        assertEquals("username2", shares?.get(1)?.shareWith)
        assertEquals("Tom", shares?.get(1)?.sharedWithDisplayName)
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun loadPublicShares() {
        val publicShares = mutableListOf(
            AppTestUtil.createPublicShare(
                path = filePath,
                isFolder = false,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            ),
            AppTestUtil.createPublicShare(
                path = filePath,
                isFolder = false,
                name = "Photos 2 link",
                shareLink = "http://server:port/s/2"
            )
        )

        every { ocShareRepository.refreshPublicShares(filePath) } returns
                MutableLiveData<DataResult<List<OCShareEntity>>>().apply {
                    value = DataResult.success(publicShares)
                }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<List<OCShareEntity>>? = ocShareViewModel.getPublicShares(filePath).value
        assertPublicShareParameters(resource?.data)
    }

    @Test
    fun insertPublicShare() {
        every {
            ocShareRepository.insertPublicShare(
                filePath,
                1,
                "Photos 2 link",
                "1234",
                -1,
                false
            )
        } returns MutableLiveData<DataResult<Unit>>().apply {
            value = DataResult.success()
        }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<Unit>? = ocShareViewModel.insertPublicShare(
            filePath,
            1,
            "Photos 2 link",
            "1234",
            -1,
            false
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    @Test
    fun updatePublicShare() {
        every {
            ocShareRepository.updatePublicShare(
                1,
                "Photos 1 link",
                "123456",
                1000,
                1,
                false
            )
        } returns MutableLiveData<DataResult<Unit>>().apply {
            value = DataResult.success()
        }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<Unit>? = ocShareViewModel.updatePublicShareForFile(
            1,
            "Photos 1 link",
            "123456",
            1000,
            1,
            false
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    private fun assertPublicShareParameters(shares: List<OCShare>?) {
        assertCommonShareParameters(shares)

        assertEquals("Photos 1 link", shares?.get(0)?.name)
        assertEquals("http://server:port/s/1", shares?.get(0)?.shareLink)

        assertEquals("Photos 2 link", shares?.get(1)?.name)
        assertEquals("http://server:port/s/2", shares?.get(1)?.shareLink)
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun deletePublicShare() {
        every {
            ocShareRepository.deletePublicShare(
                3
            )
        } returns MutableLiveData<DataResult<Unit>>().apply {
            value = DataResult.success()
        }

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: DataResult<Unit>? = ocShareViewModel.deletePublicShare(
            3
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    private fun assertPublicShareParameters(shares: List<OCShareEntity>?) {
        assertCommonShareParameters(shares)

        assertEquals("Photos 1 link", shares?.get(0)?.name)
        assertEquals("http://server:port/s/1", shares?.get(0)?.shareLink)

        assertEquals("Photos 2 link", shares?.get(1)?.name)
        assertEquals("http://server:port/s/2", shares?.get(1)?.shareLink)
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun createOCShareViewModel(ocShareRepository: OCShareRepository): OCShareViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        return OCShareViewModel(
            context,
            testAccount,
            ocShareRepository
        )
    }

    private fun assertCommonShareParameters(shares: List<OCShareEntity>?) {
        assertEquals(2, shares?.size)

        assertEquals(filePath, shares?.get(0)?.path)
        assertEquals(false, shares?.get(0)?.isFolder)

        assertEquals(filePath, shares?.get(1)?.path)
        assertEquals(false, shares?.get(1)?.isFolder)
    }
}
