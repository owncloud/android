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

package com.owncloud.android.data.sharing.shares.db

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class OCShareEntityTest {

    @Test
    fun testEqualsNamedParams() {
        val item1 = OCShareEntity(
            shareType = 0,
            shareWith = "",
            path = "/Photos/image2.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "pwdasd12dasdWZ",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            remoteId = "remoteId",
            accountOwner = "admin@server",
            name = "",
            shareLink = ""
        )

        val item2 = OCShareEntity(
            0,
            "",
            "/Photos/image2.jpg",
            1,
            1542628397,
            0,
            "pwdasd12dasdWZ",
            "",
            "",
            false,
            "remoteId",
            "admin@server",
            "",
            ""
        )

        // Autogenerate Id should differ but it is not generated at this moment
        // Tested on DAO
        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsNamedParamsNullValues() {
        val item1 = OCShareEntity(
            shareType = 0,
            shareWith = null,
            path = "/Photos/image2.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = null,
            sharedWithDisplayName = null,
            sharedWithAdditionalInfo = null,
            isFolder = false,
            remoteId = "remoteId",
            accountOwner = "admin@server",
            name = null,
            shareLink = null
        )

        val item2 = OCShareEntity(
            0,
            null,
            "/Photos/image2.jpg",
            1,
            1542628397,
            0,
            null,
            null,
            null,
            false,
            "remoteId",
            "admin@server",
            null,
            null
        )

        // Autogenerate Id should differ but it is not generated at this moment
        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testNotEqualsNamedParams() {
        val item1 = OCShareEntity(
            shareType = 0,
            shareWith = "",
            path = "/Photos/image2.jpg",
            permissions = 1,
            sharedDate = 1542628397,
            expirationDate = 0,
            token = "pwdasd12dasdWZ",
            sharedWithDisplayName = "",
            sharedWithAdditionalInfo = "",
            isFolder = false,
            remoteId = "remoteId",
            accountOwner = "admin@server",
            name = "",
            shareLink = ""
        )

        val item2 = OCShareEntity(
            0,
            "",
            "/Photos/image2.jpg",
            1,
            1542628397,
            0,
            "pwdasd12dasdWZ",
            "",
            "",
            false,
            "remoteId",
            "AnyServer",
            "",
            ""
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }
}
