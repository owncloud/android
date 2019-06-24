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

package com.owncloud.android.shares.viewmodel

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.repository.OCShareRepository
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.vo.Resource
import com.owncloud.android.vo.Status
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class OCShareViewModelTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private var testAccount: Account = TestUtil.createAccount("admin@server", "test")
    private var ocShareRepository: OCShareRepository = mock(OCShareRepository::class.java)

    @Test
    fun loadPublicShares() {
        val publicShares = mutableListOf(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Photos 1 link",
                shareLink = "http://server:port/s/1"
            ),
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Photos 2 link",
                shareLink = "http://server:port/s/2"
            )
        )

        `when`(
            ocShareRepository.getPublicSharesForFile()
        ).thenReturn(
            MutableLiveData<Resource<List<OCShare>>>().apply {
                value = Resource.success(publicShares)
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<List<OCShare>>? = ocShareViewModel.getPublicSharesForFile().value
        assertPublicShareParameters(resource?.data)
    }

    @Test
    fun loadPrivateShares() {
        val privateShares = mutableListOf(
            TestUtil.createPrivateShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                shareWith = "username1",
                sharedWithDisplayName = "Tim"
            ),
            TestUtil.createPrivateShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                shareWith = "username2",
                sharedWithDisplayName = "Tom"
            )
        )

        `when`(
            ocShareRepository.getPrivateSharesForFile()
        ).thenReturn(
            MutableLiveData<Resource<List<OCShare>>>().apply {
                value = Resource.success(privateShares)
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<List<OCShare>>? = ocShareViewModel.getPrivateSharesForFile().value
        assertPrivateShareParameters(resource?.data)
    }

    @Test
    fun insertPublicShare() {
        val ocShareRepository = mock(OCShareRepository::class.java)

        `when`(
            ocShareRepository.insertPublicShareForFile(
                1,
                "Photos 2 link",
                "1234",
                -1,
                false
            )
        ).thenReturn(
            MutableLiveData<Resource<Unit>>().apply {
                value = Resource.success()
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<Unit>? = ocShareViewModel.insertPublicShareForFile(
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
        val ocShareRepository = mock(OCShareRepository::class.java)

        `when`(
            ocShareRepository.updatePublicShareForFile(
                1,
                "Photos 1 link",
                "123456",
                1000,
                1,
                false
            )
        ).thenReturn(
            MutableLiveData<Resource<Unit>>().apply {
                value = Resource.success()
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<Unit>? = ocShareViewModel.updatePublicShareForFile(
            1,
            "Photos 1 link",
            "123456",
            1000,
            1,
            false
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    @Test
    fun deletePublicShare() {
        val ocShareRepository = mock(OCShareRepository::class.java)

        `when`(
            ocShareRepository.deletePublicShare(
                3
            )
        ).thenReturn(
            MutableLiveData<Resource<Unit>>().apply {
                value = Resource.success()
            }
        )

        // Viewmodel that will ask ocShareRepository for shares
        val ocShareViewModel = createOCShareViewModel(ocShareRepository)

        val resource: Resource<Unit>? = ocShareViewModel.deletePublicShare(
            3
        ).value

        assertEquals(Status.SUCCESS, resource?.status)
    }

    private fun createOCShareViewModel(ocShareRepository: OCShareRepository): OCShareViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        return OCShareViewModel(
            context,
            "/Photos/image.jpg",
            testAccount,
            ocShareRepository
        )
    }

    private fun assertPublicShareParameters(shares: List<OCShare>?) {
        assertCommonShareParameters(shares)

        assertEquals("Photos 1 link", shares?.get(0)?.name)
        assertEquals("http://server:port/s/1", shares?.get(0)?.shareLink)

        assertEquals("Photos 2 link", shares?.get(1)?.name)
        assertEquals("http://server:port/s/2", shares?.get(1)?.shareLink)
    }

    private fun assertPrivateShareParameters(shares: List<OCShare>?) {
        assertCommonShareParameters(shares)

        assertEquals("username1", shares?.get(0)?.shareWith)
        assertEquals("Tim", shares?.get(0)?.sharedWithDisplayName)

        assertEquals("username2", shares?.get(1)?.shareWith)
        assertEquals("Tom", shares?.get(1)?.sharedWithDisplayName)
    }

    private fun assertCommonShareParameters(shares: List<OCShare>?) {
        assertEquals(2, shares?.size)

        assertEquals("/Photos/image.jpg", shares?.get(0)?.path)
        assertEquals(false, shares?.get(0)?.isFolder)

        assertEquals("/Photos/image.jpg", shares?.get(1)?.path)
        assertEquals(false, shares?.get(1)?.isFolder)
    }
}
