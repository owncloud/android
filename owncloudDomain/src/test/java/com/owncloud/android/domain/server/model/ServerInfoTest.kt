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
package com.owncloud.android.domain.server.model

import com.owncloud.android.domain.server.model.AuthenticationMethod.BASIC_HTTP_AUTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerInfoTest {

    @Test
    fun testConstructor() {
        val item = ServerInfo(
            "10.3.2.1",
            "https://demo.owncloud.com",
            BASIC_HTTP_AUTH,
            false
        )

        assertEquals(BASIC_HTTP_AUTH, item.authenticationMethod)
        assertEquals("https://demo.owncloud.com", item.baseUrl)
        assertEquals("10.3.2.1", item.ownCloudVersion)
        assertEquals(false, item.isSecureConnection)
    }

    @Test
    fun testEqualsOk() {
        val item1 = ServerInfo(
            authenticationMethod = BASIC_HTTP_AUTH,
            baseUrl = "https://demo.owncloud.com",
            ownCloudVersion = "10.3.2.1",
            isSecureConnection = false
        )

        val item2 = ServerInfo(
            "10.3.2.1",
            "https://demo.owncloud.com",
            BASIC_HTTP_AUTH,
            false
        )

        assertTrue(item1 == item2)
        assertFalse(item1 === item2)
    }

    @Test
    fun testEqualsKo() {
        val item1 = ServerInfo(
            authenticationMethod = BASIC_HTTP_AUTH,
            baseUrl = "https://demo.owncloud.com",
            ownCloudVersion = "10.3.2.1",
            isSecureConnection = false
        )

        val item2 = ServerInfo(
            "10.0.0.0",
            "https://demo.owncloud.com",
            BASIC_HTTP_AUTH,
            false
        )

        assertFalse(item1 == item2)
        assertFalse(item1 === item2)
    }
}
