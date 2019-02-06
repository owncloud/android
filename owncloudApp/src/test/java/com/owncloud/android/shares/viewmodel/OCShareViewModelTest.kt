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
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.owncloud.android.Resource
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.repository.OCShareRepository
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCShareViewModelTest {
    private lateinit var ocShareViewModel: OCShareViewModel
    private var ocShareRepository = mock(OCShareRepository::class.java)
    private var account = mock(Account::class.java)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        `when`(account.name).thenReturn("admin@server")

        val publicShares: List<OCShare> = listOf(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = true,
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

        val publicShareResourcesAsLiveData: MutableLiveData<Resource<List<OCShare>>> = MutableLiveData()
        publicShareResourcesAsLiveData.value = Resource.success(publicShares)

        `when`(
            ocShareRepository.loadSharesForFile(
                "/Photos/image.jpg",
                "admin@server",
                listOf(ShareType.PUBLIC_LINK),
                true,
                false
            )
        ).thenReturn(
            publicShareResourcesAsLiveData
        )

        ocShareViewModel = OCShareViewModel(
            account,
            "/Photos/image.jpg",
            listOf(ShareType.PUBLIC_LINK),
            ocShareRepository
        )
    }

    @Test
    fun loadPublicShares() {
        val resource : Resource<List<OCShare>>? = ocShareViewModel.sharesForFile.value
        val shares : List<OCShare>? = resource?.data

        assertEquals(shares?.size, 2)

        assertEquals(shares?.get(0)?.path, "/Photos/image.jpg")
        assertEquals(shares?.get(0)?.isFolder, true)
        assertEquals(shares?.get(0)?.name, "Photos 1 link")
        assertEquals(shares?.get(0)?.shareLink, "http://server:port/s/1")

        assertEquals(shares?.get(1)?.path, "/Photos/image.jpg")
        assertEquals(shares?.get(1)?.isFolder, false)
        assertEquals(shares?.get(1)?.name, "Photos 2 link")
        assertEquals(shares?.get(1)?.shareLink, "http://server:port/s/2")
    }
}
