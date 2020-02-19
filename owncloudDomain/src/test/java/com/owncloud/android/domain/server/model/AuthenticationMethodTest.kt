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
