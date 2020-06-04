/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.domain.shares.model

import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OCShareTest {

    @Test
    fun testConstructor() {
        val item = OCShare(
            1,
            "7",
            "7",
            ShareType.USER,
            "",
            "/Photos/image.jpg",
            1,
            1542628397,
            0,
            "AnyToken",
            "",
            "",
            false,
            -1,
            1,
            "admin@server",
            "",
            ""
        )

        assertEquals(1, item.id)
        assertEquals("7", item.fileSource)
        assertEquals("7", item.itemSource)
        assertEquals(ShareType.USER, item.shareType)
        assertEquals("", item.shareWith)
        assertEquals("/Photos/image.jpg", item.path)
        assertEquals(1, item.permissions)
        assertEquals(1542628397, item.sharedDate)
        assertEquals(0, item.expirationDate)
        assertEquals("AnyToken", item.token)
        assertEquals("", item.sharedWithDisplayName)
        assertEquals("", item.sharedWithAdditionalInfo)
        assertEquals(false, item.isFolder)
        assertEquals(-1, item.userId)
        assertEquals(1, item.remoteId)
        assertEquals("admin@server", item.accountOwner)
        assertEquals("", item.name)
        assertEquals("", item.shareLink)
    }

    @Test
    fun testEqualsOk() {
        val item1 = OCShare(
            id = 1,
            fileSource = "7",
            itemSource = "7",
            shareType = ShareType.USER,
            shareWith = "",
            path = "/Photos/image.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "AnyToken",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            userId = -1,
            remoteId = 1,
            accountOwner = "admin@server",
            name = "",
            shareLink = ""
        )

        val item2 = OCShare(
            1,
            "7",
            "7",
            ShareType.USER,
            "",
            "/Photos/image.jpg",
            1,
            1542628397,
            0,
            "AnyToken",
            "",
            "",
            false,
            -1,
            1,
            "admin@server",
            "",
            ""
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsDefaultValues() {
        val item1 = OCShare(
            fileSource = "7",
            itemSource = "7",
            shareType = ShareType.USER,
            shareWith = "",
            path = "/Photos/image.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "AnyToken",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            userId = -1,
            remoteId = 1,
            name = "",
            shareLink = ""
        )

        val item2 = OCShare(
            null,
            "7",
            "7",
            ShareType.USER,
            "",
            "/Photos/image.jpg",
            1,
            1542628397,
            0,
            "AnyToken",
            "",
            "",
            false,
            -1,
            1,
            "",
            "",
            ""
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = OCShare(
            id = 123,
            fileSource = "7",
            itemSource = "7",
            shareType = ShareType.USER,
            shareWith = "",
            path = "/Photos/image.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "AnyToken",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            userId = -1,
            remoteId = 1,
            accountOwner = "admin@server",
            name = "",
            shareLink = ""
        )

        val item2 = OCShare(
            456,
            "7",
            "7",
            ShareType.USER,
            "",
            "/Photos/image.jpg",
            1,
            1542628397,
            0,
            "AnyToken",
            "",
            "",
            false,
            -1,
            1,
            "admin@server",
            "",
            ""
        )
        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testIsPasswordProtected() {
        val item1 = OCShare(
            id = 123,
            fileSource = "7",
            itemSource = "7",
            shareType = ShareType.PUBLIC_LINK,
            shareWith = "user@server",
            path = "/Photos/image.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "AnyToken",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            userId = -1,
            remoteId = 1,
            accountOwner = "admin@server",
            name = "",
            shareLink = ""
        )
        assertEquals(true, item1.isPasswordProtected)

        val item2 = item1.copy(shareWith = "")
        assertEquals(false, item2.isPasswordProtected)

        val item3 = item1.copy(shareType = ShareType.GROUP)
        assertEquals(false, item3.isPasswordProtected)
    }

    @Test
    fun testShareType() {
        val unknown = ShareType.fromValue(-1)
        val user = ShareType.fromValue(0)
        val group = ShareType.fromValue(1)
        val publicLink = ShareType.fromValue(3)
        val email = ShareType.fromValue(4)
        val contact = ShareType.fromValue(5)
        val federated = ShareType.fromValue(6)
        val fromValue2 = ShareType.fromValue(2)

        assertEquals(ShareType.UNKNOWN, unknown)
        assertEquals(ShareType.USER, user)
        assertEquals(ShareType.GROUP, group)
        assertEquals(ShareType.PUBLIC_LINK, publicLink)
        assertEquals(ShareType.EMAIL, email)
        assertEquals(ShareType.CONTACT, contact)
        assertEquals(ShareType.FEDERATED, federated)
        assertNull(fromValue2)
    }
}
