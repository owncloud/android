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
package com.owncloud.android.domain.files.model

import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import org.junit.Assert
import org.junit.Test

class OCFileTest {

    @Test
    fun testEqualsOk() {
        val item1 = OCFile(
            122,
            123,
            OC_ACCOUNT_NAME,
            123123123,
            0,
            1593510589000,
            "/Photos",
            "DIR",
            "5efb0c13c688f",
            "RDNVCK",
            "00000003oci9p7er2hay",
            "http://server.url/f/3"
        )

        val item2 = OCFile(
            id = 122,
            parentId = 123,
            remotePath = "/Photos",
            owner = OC_ACCOUNT_NAME,
            permissions = "RDNVCK",
            remoteId = "00000003oci9p7er2hay",
            privateLink = "http://server.url/f/3",
            creationTimestamp = 0,
            modifiedTimestamp = 1593510589000,
            etag = "5efb0c13c688f",
            mimeType = "DIR",
            length = 123123123
        )


        Assert.assertTrue(item1 == item2)
        Assert.assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = OCFile(
            123,
            122,
            OC_ACCOUNT_NAME,
            123123123,
            0,
            1593510589000,
            "/Photos",
            "DIR",
            "5efb0c13c688f",
            "RDNVCK",
            "00000003oci9p7er2hay",
            "http://server.url/f/3"
        )

        val item2 = OCFile(
            id = 122,
            parentId = 123,
            remotePath = "/Photos",
            owner = OC_ACCOUNT_NAME,
            permissions = "RDNVCK",
            remoteId = "00000003oci9p7er2hay",
            privateLink = "http://server.url/f/3",
            creationTimestamp = 0,
            modifiedTimestamp = 1593510589000,
            etag = "5efb0c13c688f",
            mimeType = "DIR",
            length = 123123123
        )

        Assert.assertFalse(item1 == item2)
        Assert.assertFalse(item1 === item2)
    }
}
