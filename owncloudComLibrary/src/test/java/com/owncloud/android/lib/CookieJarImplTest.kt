/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2021 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib

import com.owncloud.android.lib.common.http.CookieJarImpl
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieJarImplTest {

    private val oldCookies = listOf(COOKIE_A, COOKIE_B_OLD)
    private val newCookies = listOf(COOKIE_B_NEW)
    private val updatedCookies = listOf(COOKIE_A, COOKIE_B_NEW)
    private val cookieStore = hashMapOf(SOME_HOST to oldCookies)

    private val cookieJarImpl = CookieJarImpl(cookieStore)

    @Test
    fun `contains cookie with name - ok - true`() {
        assertTrue(cookieJarImpl.containsCookieWithName(oldCookies, COOKIE_B_OLD.name))
    }

    @Test
    fun `contains cookie with name - ok - false`() {
        assertFalse(cookieJarImpl.containsCookieWithName(newCookies, COOKIE_A.name))
    }

    @Test
    fun `get updated cookies - ok`() {
        val generatedUpdatedCookies = cookieJarImpl.getUpdatedCookies(oldCookies, newCookies)
        assertEquals(2, generatedUpdatedCookies.size)
        assertEquals(updatedCookies[0], generatedUpdatedCookies[1])
        assertEquals(updatedCookies[1], generatedUpdatedCookies[0])
    }

    @Test
    fun `store cookie via saveFromResponse - ok`() {
        cookieJarImpl.saveFromResponse(SOME_URL, newCookies)
        val generatedUpdatedCookies = cookieStore[SOME_HOST]
        assertEquals(2, generatedUpdatedCookies?.size)
        assertEquals(updatedCookies[0], generatedUpdatedCookies?.get(1))
        assertEquals(updatedCookies[1], generatedUpdatedCookies?.get(0))
    }

    @Test
    fun `load for request - ok`() {
        val cookies = cookieJarImpl.loadForRequest(SOME_URL)
        assertEquals(oldCookies[0], cookies[0])
        assertEquals(oldCookies[1], cookies[1])
    }

    companion object {
        const val SOME_HOST = "some.host.com"
        val SOME_URL = "https://$SOME_HOST".toHttpUrl()
        val COOKIE_A = Cookie.parse(SOME_URL, "CookieA=CookieValueA")!!
        val COOKIE_B_OLD = Cookie.parse(SOME_URL, "CookieB=CookieOldValueB")!!
        val COOKIE_B_NEW = Cookie.parse(SOME_URL, "CookieB=CookieNewValueB")!!
    }
}
