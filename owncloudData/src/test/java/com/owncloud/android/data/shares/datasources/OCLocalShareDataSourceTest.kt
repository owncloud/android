/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.shares.datasources

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource.Companion.toEntity
import com.owncloud.android.data.sharing.shares.datasources.implementation.OCLocalShareDataSource.Companion.toModel
import com.owncloud.android.data.sharing.shares.db.OCShareDao
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_PRIVATE_SHARE
import com.owncloud.android.testutil.OC_PUBLIC_SHARE
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.testutil.livedata.getLastEmittedValue
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCLocalShareDataSourceTest {
    private lateinit var ocLocalSharesDataSource: OCLocalShareDataSource
    private val ocSharesDao = mockkClass(OCShareDao::class)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val db = mockkClass(OwncloudDatabase::class)

        every {
            db.shareDao()
        } returns ocSharesDao

        ocLocalSharesDataSource =
            OCLocalShareDataSource(
                ocSharesDao,
            )
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val privateShares = listOf(
        OC_PRIVATE_SHARE.copy(
            path = "/Docs/doc1.doc",
            shareWith = "username",
            sharedWithDisplayName = "Sophie"
        ).toEntity(),
        OC_PRIVATE_SHARE.copy(
            path = "/Docs/doc1.doc",
            shareWith = "user.name",
            sharedWithDisplayName = "Nicole"
        ).toEntity()
    )

    private val privateShareTypes = listOf(ShareType.USER, ShareType.GROUP, ShareType.FEDERATED)

    @Test
    fun readLocalPrivateShares() {
        val privateSharesAsLiveData: MutableLiveData<List<OCShareEntity>> = MutableLiveData()
        privateSharesAsLiveData.value = privateShares

        every {
            ocSharesDao.getSharesAsLiveData(
                "/Docs/doc1.doc",
                "admin@server",
                privateShareTypes.map { it.value })
        } returns privateSharesAsLiveData

        val shares =
            ocLocalSharesDataSource.getSharesAsLiveData(
                "/Docs/doc1.doc", "admin@server", privateShareTypes
            ).getLastEmittedValue()!!

        assertEquals(2, shares.size)

        assertEquals("/Docs/doc1.doc", shares.first().path)
        assertEquals(false, shares.first().isFolder)
        assertEquals("username", shares.first().shareWith)
        assertEquals("Sophie", shares.first().sharedWithDisplayName)

        assertEquals("/Docs/doc1.doc", shares[1].path)
        assertEquals(false, shares[1].isFolder)
        assertEquals("user.name", shares[1].shareWith)
        assertEquals("Nicole", shares[1].sharedWithDisplayName)
    }

    @Test
    fun readLocalPrivateShare() {
        val privateShareAsLiveData: MutableLiveData<OCShareEntity> = MutableLiveData()
        privateShareAsLiveData.value = privateShares.first()

        every { ocSharesDao.getShareAsLiveData(OC_SHARE.remoteId) } returns privateShareAsLiveData

        val share = ocLocalSharesDataSource.getShareAsLiveData(OC_SHARE.remoteId).getLastEmittedValue()!!

        assertEquals("/Docs/doc1.doc", share.path)
        assertEquals(false, share.isFolder)
        assertEquals("username", share.shareWith)
        assertEquals("Sophie", share.sharedWithDisplayName)
    }

    @Test
    fun insertPrivateShares() {
        every { ocSharesDao.insert(privateShares[0]) } returns 10

        val insertedShareId = ocLocalSharesDataSource.insert(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "username",
                sharedWithDisplayName = "Sophie"
            )
        )
        assertEquals(10, insertedShareId)
    }

    @Test
    fun updatePrivateShare() {
        every { ocSharesDao.update(privateShares[1]) } returns 3

        val updatedShareId = ocLocalSharesDataSource.update(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "user.name",
                sharedWithDisplayName = "Nicole"
            )
        )
        assertEquals(3, updatedShareId)
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val publicShares = listOf(
        OC_PUBLIC_SHARE.copy(
            path = "/Photos/",
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ).toEntity(),
        OC_PUBLIC_SHARE.copy(
            path = "/Photos/",
            isFolder = true,
            name = "Photos link 2",
            shareLink = "http://server:port/s/2"
        ).toEntity()
    )

    @Test
    fun readLocalPublicShares() {
        val publicSharesAsLiveData: MutableLiveData<List<OCShareEntity>> = MutableLiveData()
        publicSharesAsLiveData.value = publicShares

        every {
            ocSharesDao.getSharesAsLiveData(
                "/Photos/",
                "admin@server",
                listOf(ShareType.PUBLIC_LINK.value)
            )
        } returns publicSharesAsLiveData

        val shares = ocLocalSharesDataSource.getSharesAsLiveData(
            "/Photos/",
            "admin@server",
            listOf(ShareType.PUBLIC_LINK)
        ).getLastEmittedValue()!!

        assertEquals(2, shares.size)

        assertEquals("/Photos/", shares.first().path)
        assertEquals(true, shares.first().isFolder)
        assertEquals("Photos link", shares.first().name)
        assertEquals("http://server:port/s/1", shares.first().shareLink)

        assertEquals("/Photos/", shares[1].path)
        assertEquals(true, shares[1].isFolder)
        assertEquals("Photos link 2", shares[1].name)
        assertEquals("http://server:port/s/2", shares[1].shareLink)
    }

    @Test
    fun insertPublicShare() {
        every { ocSharesDao.insert(publicShares[0]) } returns 7

        val insertedShareId = ocLocalSharesDataSource.insert(
            OC_PUBLIC_SHARE.copy(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link",
                shareLink = "http://server:port/s/1"
            )
        )
        assertEquals(7, insertedShareId)
    }

    @Test
    fun insertPublicShares() {
        val expectedValues = listOf<Long>(1, 2)
        every { ocSharesDao.insert(publicShares) } returns expectedValues

        val retrievedValues = ocLocalSharesDataSource.insert(
            publicShares.map { it.toModel() }
        )

        assertEquals(expectedValues, retrievedValues)
    }

    @Test
    fun updatePublicShares() {
        every { ocSharesDao.update(publicShares[1]) } returns 8

        val updatedShareId = ocLocalSharesDataSource.update(
            OC_PUBLIC_SHARE.copy(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link 2",
                shareLink = "http://server:port/s/2"
            )
        )
        assertEquals(8, updatedShareId)
    }

    /**************************************************************************************************************
     *************************************************** COMMON ***************************************************
     **************************************************************************************************************/

    @Test
    fun replaceShares() {
        val expectedValues = listOf<Long>(1, 2)
        every { ocSharesDao.replaceShares(publicShares) } returns expectedValues

        val retrievedValues = ocLocalSharesDataSource.replaceShares(
            publicShares.map { it.toModel() }
        )

        assertEquals(expectedValues, retrievedValues)
    }

    @Test
    fun deleteSharesForFile() {
        every { ocSharesDao.deleteSharesForFile("file", OC_ACCOUNT_NAME) } returns Unit
        ocLocalSharesDataSource.deleteSharesForFile("file", OC_ACCOUNT_NAME)

        verify { ocSharesDao.deleteSharesForFile("file", OC_ACCOUNT_NAME) }
    }

    @Test
    fun deleteShare() {
        every { ocSharesDao.deleteShare(OC_SHARE.remoteId) } returns 1

        val deletedRows = ocLocalSharesDataSource.deleteShare(OC_SHARE.remoteId)

        assertEquals(1, deletedRows)
    }
}
