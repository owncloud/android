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

package com.owncloud.android.shares.datasource

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.db.OCShareDao
import com.owncloud.android.utils.LiveDataTestUtil.getValue
import com.owncloud.android.utils.TestUtil
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class OCLocalDataSourceTest {
    private lateinit var ocLocalSharesDataSource: OCLocalSharesDataSource
    private val ocSharesDao = mock(OCShareDao::class.java)

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val db = mock(OwncloudDatabase::class.java)
        `when`(db.shareDao()).thenReturn(ocSharesDao)

        val sharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        sharesAsLiveData.value = listOf(
            TestUtil.createPublicShare(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link",
                shareLink = "http://server:port/s/1"
            ),
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Image link",
                shareLink = "http://server:port/s/2"
            )
        )

        `when`(
            ocSharesDao.getSharesForFileAsLiveData(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        ).thenReturn(
            sharesAsLiveData
        )

        val newShareAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        newShareAsLiveData.value = listOf(
            TestUtil.createPublicShare(
                path = "/Photos/",
                expirationDate = 20,
                isFolder = true,
                name = "Photos 2 link",
                shareLink = "http://server:port/s/3"
            )
        )

        `when`(
            ocSharesDao.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        ).thenReturn(
            newShareAsLiveData
        )

        `when`(
            ocSharesDao.insert(
                listOf(sharesAsLiveData.value!![0])
            )
        ).thenReturn(
            listOf(7)
        )

        `when`(
            ocSharesDao.update(
                sharesAsLiveData.value!![1]
            )
        ).thenReturn(
            8
        )

        ocLocalSharesDataSource = OCLocalSharesDataSource(ocSharesDao)
    }

    @Test
    fun readLocalPublicShares() {
        val shares = getValue(
            ocLocalSharesDataSource.getSharesForFileAsLiveData(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        )

        assertEquals(shares.size, 2)

        assertEquals(shares.get(0).path, "/Photos/")
        assertEquals(shares.get(0).isFolder, true)
        assertEquals(shares.get(0).name, "Photos link")
        assertEquals(shares.get(0).shareLink, "http://server:port/s/1")

        assertEquals(shares.get(1).path, "/Photos/image.jpg")
        assertEquals(shares.get(1).isFolder, false)
        assertEquals(shares.get(1).name, "Image link")
        assertEquals(shares.get(1).shareLink, "http://server:port/s/2")
    }

    @Test
    fun insertPublicShares() {
        val insertedShareId = ocLocalSharesDataSource.insert(
            listOf(
                TestUtil.createPublicShare(
                    path = "/Photos/",
                    isFolder = true,
                    name = "Photos link",
                    shareLink = "http://server:port/s/1"
                )
            )
        )

        assertEquals(insertedShareId.size, 1)
        assertEquals(insertedShareId[0], 7)
    }

    @Test
    fun updatePublicShares() {
        val updatedShareId = ocLocalSharesDataSource.update(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Image link",
                shareLink = "http://server:port/s/2"
            )
        )

        assertEquals(updatedShareId, 8)
    }
}
