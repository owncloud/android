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

import android.support.test.InstrumentationRegistry
import com.android.example.github.util.LiveDataTestUtil.getValue
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.resources.shares.ShareType
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class OCShareDaoTest {
    private var ocShareDao: OCShareDao

    init {
        val context = InstrumentationRegistry.getTargetContext()
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
        assertEquals(shares.size, 0)
    }

    @Test
    fun insertPublicSharesFromDifferentFilesAndRead() {
        ocShareDao.insert(
            listOf(
                publicShareWithNameAndLink(
                    "/Photos/",
                    true,
                    "admin@server",
                    "Photos folder link",
                    "http://server:port/s/1"
                ),
                publicShareWithNameAndLink(
                    "/Photos/image1.jpg",
                    false,
                    "admin@server",
                    "Image 1 link",
                    "http://server:port/s/2"
                ),
                publicShareWithNameAndLink(
                    "/Photos/image2.jpg",
                    false,
                    "admin@server",
                    "Image 2 link",
                    "http://server:port/s/3"
                )
            )
        )

        val photosFolderShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(photosFolderShares, notNullValue())
        assertEquals(photosFolderShares.size, 1)
        assertEquals(photosFolderShares.get(0).path, "/Photos/")
        assertEquals(photosFolderShares.get(0).isFolder, true)
        assertEquals(photosFolderShares.get(0).accountOwner, "admin@server")
        assertEquals(photosFolderShares.get(0).name, "Photos folder link")
        assertEquals(photosFolderShares.get(0).shareLink, "http://server:port/s/1")

        val image1Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/image1.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(image1Shares, notNullValue())
        assertEquals(image1Shares.size, 1)
        assertEquals(image1Shares.get(0).path, "/Photos/image1.jpg")
        assertEquals(image1Shares.get(0).isFolder, false)
        assertEquals(image1Shares.get(0).accountOwner, "admin@server")
        assertEquals(image1Shares.get(0).name, "Image 1 link")
        assertEquals(image1Shares.get(0).shareLink, "http://server:port/s/2")

        val image2Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Photos/image2.jpg", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(image2Shares, notNullValue())
        assertEquals(image2Shares.size, 1)
        assertEquals(image2Shares.get(0).path, "/Photos/image2.jpg")
        assertEquals(image2Shares.get(0).isFolder, false)
        assertEquals(image1Shares.get(0).accountOwner, "admin@server")
        assertEquals(image2Shares.get(0).name, "Image 2 link")
        assertEquals(image2Shares.get(0).shareLink, "http://server:port/s/3")
    }

    @Test
    fun insertPublicSharesFromDifferentAccountsAndRead() {
        ocShareDao.insert(
            listOf(
                publicShareWithNameAndLink(
                    "/Documents/document1.docx",
                    false,
                    "user1@server",
                    "Document 1 link",
                    "http://server:port/s/1"
                ),
                publicShareWithNameAndLink(
                    "/Documents/document1.docx",
                    false,
                    "user2@server",
                    "Document 1 link",
                    "http://server:port/s/2"
                ),
                publicShareWithNameAndLink(
                    "/Documents/document1.docx",
                    false,
                    "user3@server",
                    "Document 1 link",
                    "http://server:port/s/3"
                )
            )
        )

        val document1SharesForUser1 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user1@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(document1SharesForUser1, notNullValue())
        assertEquals(document1SharesForUser1.size, 1)
        assertEquals(document1SharesForUser1.get(0).path, "/Documents/document1.docx")
        assertEquals(document1SharesForUser1.get(0).isFolder, false)
        assertEquals(document1SharesForUser1.get(0).accountOwner, "user1@server")
        assertEquals(document1SharesForUser1.get(0).name, "Document 1 link")
        assertEquals(document1SharesForUser1.get(0).shareLink, "http://server:port/s/1")

        val document1SharesForUser2 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user2@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(document1SharesForUser2, notNullValue())
        assertEquals(document1SharesForUser2.size, 1)
        assertEquals(document1SharesForUser2.get(0).path, "/Documents/document1.docx")
        assertEquals(document1SharesForUser2.get(0).isFolder, false)
        assertEquals(document1SharesForUser2.get(0).accountOwner, "user2@server")
        assertEquals(document1SharesForUser2.get(0).name, "Document 1 link")
        assertEquals(document1SharesForUser2.get(0).shareLink, "http://server:port/s/2")

        val document1SharesForUser3 = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Documents/document1.docx", "user3@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(document1SharesForUser3, notNullValue())
        assertEquals(document1SharesForUser3.size, 1)
        assertEquals(document1SharesForUser3.get(0).path, "/Documents/document1.docx")
        assertEquals(document1SharesForUser3.get(0).isFolder, false)
        assertEquals(document1SharesForUser3.get(0).accountOwner, "user3@server")
        assertEquals(document1SharesForUser3.get(0).name, "Document 1 link")
        assertEquals(document1SharesForUser3.get(0).shareLink, "http://server:port/s/3")
    }

    @Test
    fun getNonExistingPublicShare() {
        ocShareDao.insert(
            listOf(
                publicShareWithNameAndLink(
                    "/Videos/video1.mp4",
                    false,
                    "user@server",
                    "Video 1 link",
                    "http://server:port/s/1"
                )
            )
        )

        val video2Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Videos/video2.mp4", "user@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(video2Shares, notNullValue())
        assertEquals(video2Shares.size, 0)
    }

    @Test
    fun replacePublicShareIfAlreadyExists_exists() {
        ocShareDao.insert(
            listOf(
                publicShareWithNameAndLink(
                    "/Texts/text1.txt",
                    false,
                    "admin@server",
                    "Text 1 link",
                    "http://server:port/s/1"
                )
            )
        )

        ocShareDao.replace(
            listOf( // Update link name
                publicShareWithNameAndLink(
                    "/Texts/text1.txt",
                    false,
                    "admin@server",
                    "Text new link",
                    "http://server:port/s/1"
                )
            )
        )

        val textShares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(textShares, notNullValue())
        assertEquals(textShares.size, 1)
        assertEquals(textShares.get(0).name, "Text new link")
    }

    @Test
    fun replacePublicShareIfAlreadyExists_doesNotExist() {
        ocShareDao.insert(
            listOf(
                publicShareWithNameAndLink(
                    "/Texts/text1.txt",
                    false,
                    "admin@server",
                    "Text 1 link",
                    "http://server:port/s/1"
                )
            )
        )

        ocShareDao.replace(
            listOf( // New link
                publicShareWithNameAndLink(
                    "/Texts/text2.txt",
                    false,
                    "admin@server",
                    "Text 2 link",
                    "http://server:port/s/2"
                )
            )
        )

        val text1Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text1.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(text1Shares, notNullValue())
        assertEquals(text1Shares.size, 1)
        assertEquals(text1Shares.get(0).name, "Text 1 link")

        // text2 link didn't exist before, it should not replace the old one but be created
        val text2Shares = getValue(
            ocShareDao.getSharesForFileAsLiveData(
                "/Texts/text2.txt", "admin@server", listOf(ShareType.PUBLIC_LINK.value)
            )
        )

        assertThat(text2Shares, notNullValue())
        assertEquals(text2Shares.size, 1)
        assertEquals(text2Shares.get(0).name, "Text 2 link")
    }

    fun publicShareWithNameAndLink(
        path: String,
        isFolder: Boolean,
        accountOwner: String,
        name: String,
        shareLink: String
    ): OCShare {
        return OCShare(
            7,
            7,
            3,
            "",
            path,
            1,
            1542628397,
            0,
            "pwdasd12dasdWZ",
            "",
            isFolder,
            -1,
            1,
            accountOwner,
            name,
            shareLink
        )
    }
}
