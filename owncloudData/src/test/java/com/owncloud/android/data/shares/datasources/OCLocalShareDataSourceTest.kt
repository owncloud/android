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
    fun `getSharesAsLiveData read local private shares returns a list of LiveData OCShare`() {
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

        verify(exactly = 1) {
            ocSharesDao.getSharesAsLiveData(
                "/Docs/doc1.doc",
                "admin@server",
                privateShareTypes.map { it.value })
        }
    }

    @Test(expected = Exception::class)
    fun `getSharesAsLiveData read local private shares returns an exception when dao receive an exception`() {
        every {
            ocSharesDao.getSharesAsLiveData(
                "/Docs/doc1.doc",
                "admin@server",
                privateShareTypes.map { it.value })
        } throws Exception()

        ocLocalSharesDataSource.getSharesAsLiveData(
                "/Docs/doc1.doc", "admin@server", privateShareTypes
        ).getLastEmittedValue()!!

    }

    @Test
    fun `getSharesAsLiveData read local private share returns a OCShare`() {
        val privateShareAsLiveData: MutableLiveData<OCShareEntity> = MutableLiveData()
        privateShareAsLiveData.value = privateShares.first()

        every { ocSharesDao.getShareAsLiveData(OC_SHARE.remoteId) } returns privateShareAsLiveData

        val share = ocLocalSharesDataSource.getShareAsLiveData(OC_SHARE.remoteId).getLastEmittedValue()!!

        assertEquals("/Docs/doc1.doc", share.path)
        assertEquals(false, share.isFolder)
        assertEquals("username", share.shareWith)
        assertEquals("Sophie", share.sharedWithDisplayName)

        verify(exactly = 1) { ocSharesDao.getShareAsLiveData(OC_SHARE.remoteId) }
    }

    @Test(expected = Exception::class)
    fun `getSharesAsLiveData read local private share returns an exception when dao receive an exception`() {

        every { ocSharesDao.getShareAsLiveData(OC_SHARE.remoteId) } throws Exception()

        ocLocalSharesDataSource.getShareAsLiveData(OC_SHARE.remoteId).getLastEmittedValue()!!

    }

    @Test
    fun `insert private OCShare into repository returns a long`() {
        every { ocSharesDao.insertOrReplace(privateShares[0]) } returns 10

        val insertedShareId = ocLocalSharesDataSource.insert(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "username",
                sharedWithDisplayName = "Sophie"
            )
        )
        assertEquals(10, insertedShareId)

        verify(exactly = 1) { ocSharesDao.insertOrReplace(privateShares[0]) }
    }

    @Test(expected = Exception::class)
    fun `insert private OCShare into repository returns an exception when dao receive an exception`() {
        every { ocSharesDao.insertOrReplace(privateShares[0]) } throws Exception()

        ocLocalSharesDataSource.insert(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "username",
                sharedWithDisplayName = "Sophie"
            )
        )
    }


    @Test
    fun `insert list of private OCShares into repository returns a list of long`() {

        val expectedValues = listOf<Long>(1, 2)
        every { ocSharesDao.insertOrReplace(privateShares) } returns expectedValues

        val insertedSharesid = ocLocalSharesDataSource.insert(
            privateShares.map { it.toModel() }
        )

        assertEquals(expectedValues, insertedSharesid)

        verify(exactly = 1) { ocSharesDao.insertOrReplace(privateShares) }
    }

    @Test(expected = Exception::class)
    fun `insert list of private OCShares into repository returns an exception when dao receive an exception`() {

        every { ocSharesDao.insertOrReplace(privateShares) } throws Exception()

        ocLocalSharesDataSource.insert(
            privateShares.map { it.toModel() }
        )
    }

    @Test
    fun `update private OCShare and returns a long`() {
        every { ocSharesDao.update(privateShares[1]) } returns 3

        val updatedShareId = ocLocalSharesDataSource.update(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "user.name",
                sharedWithDisplayName = "Nicole"
            )
        )
        assertEquals(3, updatedShareId)

        verify(exactly = 1) { ocSharesDao.update(privateShares[1]) }
    }

    @Test(expected = Exception::class)
    fun `update private OCShare and returns returns an exception when dao receive an exception`() {
        every { ocSharesDao.update(privateShares[1]) } throws Exception()

        ocLocalSharesDataSource.update(
            OC_PRIVATE_SHARE.copy(
                path = "/Docs/doc1.doc",
                shareWith = "user.name",
                sharedWithDisplayName = "Nicole"
            )
        )
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
    fun `getSharesAsLiveData read local public shares returns a list of LiveData OCShare`() {
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

        verify(exactly = 1) {
            ocSharesDao.getSharesAsLiveData(
                "/Photos/",
                "admin@server",
                listOf(ShareType.PUBLIC_LINK.value))
        }
    }

    @Test(expected = Exception::class)
    fun `getSharesAsLiveData read local public shares returns an exception when dao receive an exception`() {
        every {
            ocSharesDao.getSharesAsLiveData(
                "/Photos/",
                "admin@server",
                listOf(ShareType.PUBLIC_LINK.value)
            )
        } throws Exception()

        ocLocalSharesDataSource.getSharesAsLiveData(
            "/Photos/",
            "admin@server",
            listOf(ShareType.PUBLIC_LINK)
        ).getLastEmittedValue()!!
    }

    @Test
    fun `insert public OCShare into repository returns a long`() {
        every { ocSharesDao.insertOrReplace(publicShares[0]) } returns 7

        val insertedShareId = ocLocalSharesDataSource.insert(
            OC_PUBLIC_SHARE.copy(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link",
                shareLink = "http://server:port/s/1"
            )
        )
        assertEquals(7, insertedShareId)

        verify(exactly = 1) { ocSharesDao.insertOrReplace(publicShares[0]) }
    }

    @Test(expected = Exception::class)
    fun `insert public OCShare into repository returns an exception when dao receive an exception`() {
        every { ocSharesDao.insertOrReplace(publicShares[0]) } throws Exception()

        ocLocalSharesDataSource.insert(
            OC_PUBLIC_SHARE.copy(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link",
                shareLink = "http://server:port/s/1"
            )
        )
    }

    @Test
    fun `insert list of public OCShares into repository returns a list of long`() {
        val expectedValues = listOf<Long>(1, 2)
        every { ocSharesDao.insertOrReplace(publicShares) } returns expectedValues

        val retrievedValues = ocLocalSharesDataSource.insert(
            publicShares.map { it.toModel() }
        )

        assertEquals(expectedValues, retrievedValues)

        verify(exactly = 1) { ocSharesDao.insertOrReplace(publicShares) }
    }

    @Test(expected = Exception::class)
    fun `insert list of public OCShares into repository returns an exception when dao receive an exception`() {

        every { ocSharesDao.insertOrReplace(publicShares) } throws Exception()

        ocLocalSharesDataSource.insert(
            publicShares.map { it.toModel() }
        )
    }

    @Test
    fun `update public OCShare and returns a long`() {
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

        verify(exactly = 1) { ocSharesDao.update(publicShares[1]) }
    }

    @Test(expected = Exception::class)
    fun `update public OCShare and returns an exception when dao receive an exception`() {
        every { ocSharesDao.update(publicShares[1]) } throws Exception()

       ocLocalSharesDataSource.update(
            OC_PUBLIC_SHARE.copy(
                path = "/Photos/",
                isFolder = true,
                name = "Photos link 2",
                shareLink = "http://server:port/s/2"
            )
        )
    }

    /**************************************************************************************************************
     *************************************************** COMMON ***************************************************
     **************************************************************************************************************/

    @Test
    fun `replaceShares OCShares and returns a list of long`() {
        val expectedValues = listOf<Long>(1, 2)
        every { ocSharesDao.replaceShares(publicShares) } returns expectedValues

        val retrievedValues = ocLocalSharesDataSource.replaceShares(
            publicShares.map { it.toModel() }
        )

        assertEquals(expectedValues, retrievedValues)

        verify(exactly = 1) { ocSharesDao.replaceShares(publicShares) }
    }

    @Test(expected = Exception::class)
    fun `replaceShares returns returns an exception when dao receive an exception`() {

        every { ocSharesDao.replaceShares(publicShares) } throws Exception()

        ocLocalSharesDataSource.replaceShares(
            publicShares.map { it.toModel() }
        )
    }

    @Test
    fun `deleteSharesForFile returns Unit`() {
        every { ocSharesDao.deleteSharesForFile("file", OC_ACCOUNT_NAME) } returns Unit
        ocLocalSharesDataSource.deleteSharesForFile("file", OC_ACCOUNT_NAME)

        verify(exactly = 1)  { ocSharesDao.deleteSharesForFile("file", OC_ACCOUNT_NAME) }
    }

    @Test(expected = Exception::class)
    fun `deleteSharesForFile returns an exception when dao receive an exception`() {
        every { ocSharesDao.deleteSharesForFile("file", OC_ACCOUNT_NAME) } throws Exception()
        ocLocalSharesDataSource.deleteSharesForFile("file", OC_ACCOUNT_NAME)
    }


    @Test
    fun `deleteShare OCShare by remoteId and returns a Int`() {
        every { ocSharesDao.deleteShare(OC_SHARE.remoteId) } returns 1

        val deletedRows = ocLocalSharesDataSource.deleteShare(OC_SHARE.remoteId)

        assertEquals(1, deletedRows)

        verify(exactly = 1) { ocSharesDao.deleteShare(OC_SHARE.remoteId) }
    }

    @Test(expected = Exception::class)
    fun `deleteShare OCShare by remoteId returns an exception when dao receive an exception`() {
        every { ocSharesDao.deleteShare(OC_SHARE.remoteId) } throws Exception()

        ocLocalSharesDataSource.deleteShare(OC_SHARE.remoteId)
    }

    @Test
    fun `deleteSharesForAccount by account returns unit`() {
        every { ocSharesDao.deleteSharesForAccount(OC_SHARE.accountOwner) } returns Unit

        ocLocalSharesDataSource.deleteSharesForAccount(OC_SHARE.accountOwner)

        verify(exactly = 1) { ocSharesDao.deleteSharesForAccount(OC_SHARE.accountOwner) }
    }

    @Test(expected = Exception::class)
    fun `deleteSharesForAccount by account returns an exception when dao receive an exception`() {
        every { ocSharesDao.deleteSharesForAccount(OC_SHARE.accountOwner) } throws Exception()

        ocLocalSharesDataSource.deleteSharesForAccount(OC_SHARE.accountOwner)
    }
}
