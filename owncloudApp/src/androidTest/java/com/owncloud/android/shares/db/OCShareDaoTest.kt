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

package com.owncloud.android.shares.db

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShareDao
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

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OwncloudDatabase.switchToInMemory(context)
        val db: OwncloudDatabase = OwncloudDatabase.getDatabase(context)
        ocShareDao = db.shareDao()
    }

    @Test
    fun insertEmptySharesList() {
        ocShareDao.insert(listOf())

        val shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Test/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(shares, notNullValue())
        assertEquals(0, shares.size)
    }

    @Test
    fun insertPublicSharesFromDifferentFilesAndRead() {
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
                TestUtil.createPublicShare(
                    path = "/Photos/image2.jpg",
                    isFolder = false,
                    name = "Image 2 link",
                    shareLink = "http://server:port/s/3"
                )
            )
        )

        val photosFolderShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(photosFolderShares, notNullValue())
        assertEquals(1, photosFolderShares.size)
        assertEquals("/Photos/", photosFolderShares.get(0).path)
        assertEquals(true, photosFolderShares.get(0).isFolder)
        assertEquals("admin@server", photosFolderShares.get(0).accountOwner)
        assertEquals("Photos folder link", photosFolderShares.get(0).name)
        assertEquals("http://server:port/s/1", photosFolderShares.get(0).shareLink)

        val image1Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(image1Shares, notNullValue())
        assertEquals(1, image1Shares.size)
        assertEquals("/Photos/image1.jpg", image1Shares.get(0).path)
        assertEquals(false, image1Shares.get(0).isFolder)
        assertEquals("admin@server", image1Shares.get(0).accountOwner)
        assertEquals("Image 1 link", image1Shares.get(0).name)
        assertEquals("http://server:port/s/2", image1Shares.get(0).shareLink)

        val image2Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/image2.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(image2Shares, notNullValue())
        assertEquals(1, image2Shares.size)
        assertEquals("/Photos/image2.jpg", image2Shares.get(0).path)
        assertEquals(false, image2Shares.get(0).isFolder)
        assertEquals("admin@server", image1Shares.get(0).accountOwner)
        assertEquals("Image 2 link", image2Shares.get(0).name)
        assertEquals("http://server:port/s/3", image2Shares.get(0).shareLink)
    }

    @Test
    fun insertPublicSharesFromDifferentAccountsAndRead() {
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
                TestUtil.createPublicShare(
                    path = "/Documents/document1.docx",
                    isFolder = false,
                    accountOwner = "user3@server",
                    name = "Document 1 link",
                    shareLink = "http://server:port/s/3"
                )
            )
        )

        val document1SharesForUser1 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user1@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(document1SharesForUser1, notNullValue())
        assertEquals(1, document1SharesForUser1.size)
        assertEquals("/Documents/document1.docx", document1SharesForUser1.get(0).path)
        assertEquals(false, document1SharesForUser1.get(0).isFolder)
        assertEquals("user1@server", document1SharesForUser1.get(0).accountOwner)
        assertEquals("Document 1 link", document1SharesForUser1.get(0).name)
        assertEquals("http://server:port/s/1", document1SharesForUser1.get(0).shareLink)

        val document1SharesForUser2 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user2@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(document1SharesForUser2, notNullValue())
        assertEquals(1, document1SharesForUser2.size)
        assertEquals("/Documents/document1.docx", document1SharesForUser2.get(0).path)
        assertEquals(false, document1SharesForUser2.get(0).isFolder)
        assertEquals("user2@server", document1SharesForUser2.get(0).accountOwner)
        assertEquals("Document 1 link", document1SharesForUser2.get(0).name)
        assertEquals("http://server:port/s/2", document1SharesForUser2.get(0).shareLink)

        val document1SharesForUser3 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user3@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(document1SharesForUser3, notNullValue())
        assertEquals(1, document1SharesForUser3.size)
        assertEquals("/Documents/document1.docx", document1SharesForUser3.get(0).path)
        assertEquals(false, document1SharesForUser3.get(0).isFolder)
        assertEquals("user3@server", document1SharesForUser3.get(0).accountOwner)
        assertEquals("Document 1 link", document1SharesForUser3.get(0).name)
        assertEquals("http://server:port/s/3", document1SharesForUser3.get(0).shareLink)
    }

    @Test
    fun getNonExistingPublicShare() {
        ocShareDao.insert(newPublicShare())

        val nonExistingShare = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text2.txt", "user@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(nonExistingShare, notNullValue())
        assertEquals(0, nonExistingShare.size)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_exists() {
        ocShareDao.insert(newPublicShare())

        ocShareDao.replaceSharesForFile(
            listOf(newPublicShare(name = "Text 2 link"))
        )

        val textShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(1, textShares.size)
        assertEquals("Text 2 link", textShares.get(0).name)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        ocShareDao.insert(newPublicShare())

        ocShareDao.replaceSharesForFile(
            listOf(newPublicShare(path = "/Texts/text2.txt", name = "Text 2 link"))
        )

        val text1Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(text1Shares, notNullValue())
        assertEquals(1, text1Shares.size)
        assertEquals("Text 1 link", text1Shares.get(0).name)

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text2.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(text2Shares, notNullValue())
        assertEquals(1, text2Shares.size)
        assertEquals("Text 2 link", text2Shares.get(0).name)
    }

    @Test
    fun updatePublicShare() {
        ocShareDao.insert(newPublicShare())

        ocShareDao.update(
            newPublicShare(name = "Text 1 link updated", expirationDate = 2000)
        )

        val textShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(1, textShares.size)
        assertEquals("Text 1 link updated", textShares.get(0).name)
        assertEquals(2000, textShares.get(0).expirationDate)
    }

    @Test
    fun deletePublicShare() {
        ocShareDao.insert(newPublicShare())

        ocShareDao.deleteShare(1)

        val textShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )
        assertThat(textShares, notNullValue())
        assertEquals(0, textShares.size) // List of textShares empty after deleting the existing share
    }

    private fun newPublicShare(
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
