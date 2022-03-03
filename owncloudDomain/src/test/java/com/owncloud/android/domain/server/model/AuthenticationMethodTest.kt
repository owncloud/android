/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationMethodTest {

    @Test
    fun basicAuthenticationMethodToString() {
        val expectedValue = "basic"
        val currentValue = AuthenticationMethod.BASIC_HTTP_AUTH.toString()
        assertEquals(expectedValue, currentValue)
    }

    @Test
    fun bearerAuthenticationMethodToString() {
        val expectedValue = "bearer"
        val currentValue = AuthenticationMethod.BEARER_TOKEN.toString()
        assertEquals(expectedValue, currentValue)
    }

    @Test
    fun noneAuthenticationMethodToString() {
        val expectedValue = "none"
        val currentValue = AuthenticationMethod.NONE.toString()
        assertEquals(expectedValue, currentValue)
    }
}
