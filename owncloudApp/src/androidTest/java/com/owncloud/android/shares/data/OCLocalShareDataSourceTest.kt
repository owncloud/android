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

package com.owncloud.android.shares.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.data.datasources.OCLocalSharesDataSource
import com.owncloud.android.shares.data.datasources.OCShareDao
import com.owncloud.android.shares.domain.OCShare
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

    val publicShares = listOf(
        TestUtil.createPublicShare(
            path = "/Photos/",
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare(
            path = "/Photos/",
            isFolder = true,
            name = "Photos link 2",
            shareLink = "http://server:port/s/2"
        )
    )

    private val privateShares = listOf(
        TestUtil.createPrivateShare(
            path = "/Docs/doc1.doc",
            isFolder = false,
            shareWith = "username",
            sharedWithDisplayName = "Sophie"
        ),
        TestUtil.createPrivateShare(
            path = "/Docs/doc1.doc",
            isFolder = false,
            shareWith = "user.name",
            sharedWithDisplayName = "Nicole"
        )
    )

    private val privateShareTypes = listOf(
        ShareType.USER, ShareType.GROUP, ShareType.FEDERATED
    )

    @Before
    fun init() {
        val db = mock(OwncloudDatabase::class.java)
        `when`(db.shareDao()).thenReturn(ocSharesDao)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ocLocalSharesDataSource =
            OCLocalSharesDataSource(context, ocSharesDao)
    }


    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun readLocalPrivateShares() {
        val privateSharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        privateSharesAsLiveData.value = privateShares

        `when`(
            ocSharesDao.getSharesAsLiveData(
                "/Docs/doc1.doc", "admin@server", privateShareTypes.map {
                    it.value
                }
            )
        ).thenReturn(
            privateSharesAsLiveData
        )

        val shares = getValue(
            ocLocalSharesDataSource.getSharesAsLiveData(
                "/Docs/doc1.doc", "admin@server", privateShareTypes
            )
        )

        assertEquals(2, shares.size)

        assertEquals("/Docs/doc1.doc", shares[0].path)
        assertEquals(false, shares[0].isFolder)
        assertEquals("username", shares[0].shareWith)
        assertEquals("Sophie", shares[0].sharedWithDisplayName)

        assertEquals("/Docs/doc1.doc", shares[1].path)
        assertEquals(false, shares[1].isFolder)
        assertEquals("user.name", shares[1].shareWith)
        assertEquals("Nicole", shares[1].sharedWithDisplayName)
    }

    @Test
    fun insertPrivateShares() {
        val privateSharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        privateSharesAsLiveData.value = privateShares

        `when`(
            ocSharesDao.insert(
                privateSharesAsLiveData.value!![0]
            )
        ).thenReturn(
            10
        )

        val insertedShareId = ocLocalSharesDataSource.insert(
            TestUtil.createPrivateShare(
                shareType = ShareType.USER.value,
                path = "/Docs/doc1.doc",
                isFolder = false,
                shareWith = "username",
                sharedWithDisplayName = "Sophie"
            )
        )
        assertEquals(10, insertedShareId)
    }


    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun readLocalPublicShares() {
        val publicSharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        publicSharesAsLiveData.value = publicShares

        `when`(
            ocSharesDao.getSharesAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        ).thenReturn(
            publicSharesAsLiveData
        )

        val shares = getValue(
            ocLocalSharesDataSource.getSharesAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK)
            )
        )

        assertEquals(2, shares.size)

        assertEquals("/Photos/", shares[0].path)
        assertEquals(true, shares[0].isFolder)
        assertEquals("Photos link", shares[0].name)
        assertEquals("http://server:port/s/1", shares[0].shareLink)

        assertEquals("/Photos/", shares[1].path)
        assertEquals(true, shares[1].isFolder)
        assertEquals("Photos link 2", shares[1].name)
        assertEquals("http://server:port/s/2", shares[1].shareLink)
    }

    @Test
    fun insertPublicShares() {
        val publicSharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        publicSharesAsLiveData.value = publicShares

        `when`(
            ocSharesDao.insert(
                publicSharesAsLiveData.value!![0]
            )
        ).thenReturn(
            7
        )

        val insertedShareId = ocLocalSharesDataSource.insert(
            TestUtil.createPublicShare(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link",
                shareLink = "http://server:port/s/1"
            )
        )
        assertEquals(7, insertedShareId)
    }

    @Test
    fun updatePublicShares() {
        val publicSharesAsLiveData: MutableLiveData<List<OCShare>> = MutableLiveData()
        publicSharesAsLiveData.value = publicShares

        `when`(
            ocSharesDao.update(
                publicSharesAsLiveData.value!![1]
            )
        ).thenReturn(
            8
        )

        val updatedShareId = ocLocalSharesDataSource.update(
            TestUtil.createPublicShare(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link 2",
                shareLink = "http://server:port/s/2"
            )
        )
        assertEquals(8, updatedShareId)
    }

    @Test
    fun deletePublicShare() {
        `when`(
            ocSharesDao.deleteShare(
                5
            )
        ).thenReturn(
            1
        )

        val deletedRows = ocLocalSharesDataSource.deleteShare(
            5
        )
        assertEquals(1, deletedRows)
    }
}
