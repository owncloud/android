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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.data.datasources.OCShareDao
import com.owncloud.android.utils.LiveDataTestUtil.getValue
import com.owncloud.android.utils.TestUtil
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OCShareDaoTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var ocShareDao: OCShareDao

    private val privateShareTypeValues = listOf(
        ShareType.USER.value, ShareType.GROUP.value, ShareType.FEDERATED.value
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocShareDao = db.shareDao()
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    @Test
    fun insertEmptySharesList() {
        ocShareDao.insert(listOf())

        val shares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Test/", "admin@server", mutableListOf(
                    ShareType.PUBLIC_LINK.value
                ).plus(
                    privateShareTypeValues
                )
            )
        )
        assertThat(shares, notNullValue())
        assertEquals(0, shares.size)
    }

    @Test
    fun insertSharesFromDifferentFilesAndRead() {
        ocShareDao.insert(
            listOf(
                TestUtil.createPublicShare(
                    path = "/Photos/",
                    isFolder = true,
                    name = "Photos folder link",
                    shareLink = "http://server:port/s/1"
                ),
                TestUtil.createPublicShare(
                    path = "/Photos/image1.jpg",
                    isFolder = false,
                    name = "Image 1 link",
                    shareLink = "http://server:port/s/2"
                ),
                TestUtil.createPrivateShare(
                    path = "/Photos/image2.jpg",
                    isFolder = false,
                    shareWith = "username",
                    sharedWithDisplayName = "John"
                )
            )
        )

        val photosFolderPublicShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(photosFolderPublicShares, notNullValue())
        assertEquals(1, photosFolderPublicShares.size)
        assertEquals("/Photos/", photosFolderPublicShares[0].path)
        assertEquals(true, photosFolderPublicShares[0].isFolder)
        assertEquals("admin@server", photosFolderPublicShares[0].accountOwner)
        assertEquals("Photos folder link", photosFolderPublicShares[0].name)
        assertEquals("http://server:port/s/1", photosFolderPublicShares[0].shareLink)

        val image1PublicShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(image1PublicShares, notNullValue())
        assertEquals(1, image1PublicShares.size)
        assertEquals("/Photos/image1.jpg", image1PublicShares[0].path)
        assertEquals(false, image1PublicShares[0].isFolder)
        assertEquals("admin@server", image1PublicShares[0].accountOwner)
        assertEquals("Image 1 link", image1PublicShares[0].name)
        assertEquals("http://server:port/s/2", image1PublicShares[0].shareLink)

        val image2PrivateShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Photos/image2.jpg", "admin@server", privateShareTypeValues
            )
        )
        assertThat(image2PrivateShares, notNullValue())
        assertEquals(1, image2PrivateShares.size)
        assertEquals("/Photos/image2.jpg", image2PrivateShares[0].path)
        assertEquals(false, image2PrivateShares[0].isFolder)
        assertEquals("admin@server", image1PublicShares[0].accountOwner)
        assertEquals("username", image2PrivateShares[0].shareWith)
        assertEquals("John", image2PrivateShares[0].sharedWithDisplayName)
    }

    @Test
    fun insertSharesFromDifferentAccountsAndRead() {
        ocShareDao.insert(
            listOf(
                TestUtil.createPublicShare(
                    path = "/Documents/document1.docx",
                    isFolder = false,
                    accountOwner = "user1@server",
                    name = "Document 1 link",
                    shareLink = "http://server:port/s/1"
                ),
                TestUtil.createPublicShare(
                    path = "/Documents/document1.docx",
                    isFolder = false,
                    accountOwner = "user2@server",
                    name = "Document 1 link",
                    shareLink = "http://server:port/s/2"
                ),
                TestUtil.createPrivateShare(
                    path = "/Documents/document1.docx",
                    isFolder = false,
                    accountOwner = "user3@server",
                    shareWith = "user_name",
                    sharedWithDisplayName = "Patrick"
                )
            )
        )

        val document1PublicSharesForUser1 = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Documents/document1.docx", "user1@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(document1PublicSharesForUser1, notNullValue())
        assertEquals(1, document1PublicSharesForUser1.size)
        assertEquals("/Documents/document1.docx", document1PublicSharesForUser1[0].path)
        assertEquals(false, document1PublicSharesForUser1[0].isFolder)
        assertEquals("user1@server", document1PublicSharesForUser1[0].accountOwner)
        assertEquals("Document 1 link", document1PublicSharesForUser1[0].name)
        assertEquals("http://server:port/s/1", document1PublicSharesForUser1[0].shareLink)

        val document1PublicSharesForUser2 = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Documents/document1.docx", "user2@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(document1PublicSharesForUser2, notNullValue())
        assertEquals(1, document1PublicSharesForUser2.size)
        assertEquals("/Documents/document1.docx", document1PublicSharesForUser2[0].path)
        assertEquals(false, document1PublicSharesForUser2[0].isFolder)
        assertEquals("user2@server", document1PublicSharesForUser2[0].accountOwner)
        assertEquals("Document 1 link", document1PublicSharesForUser2[0].name)
        assertEquals("http://server:port/s/2", document1PublicSharesForUser2[0].shareLink)

        val document1PrivateSharesForUser3 = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Documents/document1.docx", "user3@server", privateShareTypeValues
            )
        )
        assertThat(document1PrivateSharesForUser3, notNullValue())
        assertEquals(1, document1PrivateSharesForUser3.size)
        assertEquals("/Documents/document1.docx", document1PrivateSharesForUser3[0].path)
        assertEquals(false, document1PrivateSharesForUser3[0].isFolder)
        assertEquals("user3@server", document1PrivateSharesForUser3[0].accountOwner)
        assertEquals("user_name", document1PrivateSharesForUser3[0].shareWith)
        assertEquals("Patrick", document1PrivateSharesForUser3[0].sharedWithDisplayName)
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    @Test
    fun getNonExistingPrivateShare() {
        ocShareDao.insert(createDefaultPrivateShare())

        val nonExistingPrivateShare = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text2.txt", "user@server", privateShareTypeValues
            )
        )
        assertThat(nonExistingPrivateShare, notNullValue())
        assertEquals(0, nonExistingPrivateShare.size)
    }

    @Test
    fun replacePrivateShareIfAlreadyExists_exists() {
        ocShareDao.insert(createDefaultPrivateShare())

        ocShareDao.replaceShares(
            listOf(createDefaultPrivateShare(shareWith = "userName"))
        )

        val textShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.USER.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(1, textShares.size)
        assertEquals("userName", textShares[0].shareWith)
    }

    @Test
    fun replacePrivateShareIfAlreadyExists_doesNotExist() {
        ocShareDao.insert(
            createDefaultPrivateShare(
                shareType = ShareType.GROUP.value
            )
        )

        ocShareDao.replaceShares(
            listOf(
                createDefaultPrivateShare(
                    shareType = ShareType.GROUP.value,
                    shareWith = "userName",
                    path = "/Texts/text2.txt"
                )
            )
        )

        val text1Shares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.GROUP.value)
            )
        )
        assertThat(text1Shares, notNullValue())
        assertEquals(1, text1Shares.size)
        assertEquals("username", text1Shares[0].shareWith)

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text2.txt", "admin@server", listOf(ShareType.GROUP.value)
            )
        )
        assertThat(text2Shares, notNullValue())
        assertEquals(1, text2Shares.size)
        assertEquals("userName", text2Shares[0].shareWith)
    }

    private fun createDefaultPrivateShare(
        shareType: Int = 0,
        shareWith: String = "username",
        path: String = "/Texts/text1.txt",
        shareWithDisplayName: String = "Steve"
    ) = TestUtil.createPrivateShare(
        shareType = shareType,
        shareWith = shareWith,
        path = path,
        isFolder = false,
        sharedWithDisplayName = shareWithDisplayName
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    @Test
    fun getNonExistingPublicShare() {
        ocShareDao.insert(createDefaultPublicShare())

        val nonExistingPublicShare = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text2.txt", "user@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(nonExistingPublicShare, notNullValue())
        assertEquals(0, nonExistingPublicShare.size)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_exists() {
        ocShareDao.insert(createDefaultPublicShare())

        ocShareDao.replaceShares(
            listOf(createDefaultPublicShare(name = "Text 2 link"))
        )

        val textShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(1, textShares.size)
        assertEquals("Text 2 link", textShares[0].name)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        ocShareDao.insert(createDefaultPublicShare())

        ocShareDao.replaceShares(
            listOf(createDefaultPublicShare(path = "/Texts/text2.txt", name = "Text 2 link"))
        )

        val text1Shares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(text1Shares, notNullValue())
        assertEquals(1, text1Shares.size)
        assertEquals("Text 1 link", text1Shares[0].name)

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text2.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(text2Shares, notNullValue())
        assertEquals(1, text2Shares.size)
        assertEquals("Text 2 link", text2Shares[0].name)
    }

    @Test
    fun updatePublicShare() {
        ocShareDao.insert(createDefaultPublicShare())

        ocShareDao.update(
            createDefaultPublicShare(name = "Text 1 link updated", expirationDate = 2000)
        )

        val textShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(1, textShares.size)
        assertEquals("Text 1 link updated", textShares[0].name)
        assertEquals(2000, textShares[0].expirationDate)
    }

    @Test
    fun deletePublicShare() {
        ocShareDao.insert(createDefaultPublicShare())

        ocShareDao.deleteShare(1)

        val textShares = getValue(
            ocShareDao.getSharesAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(0, textShares.size) // List of textShares empty after deleting the existing share
    }

    private fun createDefaultPublicShare(
        path: String = "/Texts/text1.txt",
        expirationDate: Long = 1000,
        name: String = "Text 1 link"
    ) = TestUtil.createPublicShare(
        path = path,
        expirationDate = expirationDate,
        isFolder = false,
        name = name,
        shareLink = "http://server:port/s/1"
    )
}
