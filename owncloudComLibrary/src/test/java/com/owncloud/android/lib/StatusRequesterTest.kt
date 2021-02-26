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

import com.owncloud.android.lib.resources.status.StatusRequester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRequesterTest {
    private val requester = StatusRequester()

    @Test
    fun `update location - ok - absolute path`() {
        val newLocation = requester.updateLocationWithRedirectPath(TEST_DOMAIN, "$TEST_DOMAIN$SUB_PATH")
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    @Test
    fun `update location - ok - smaller absolute path`() {
        val newLocation = requester.updateLocationWithRedirectPath("$TEST_DOMAIN$SUB_PATH", TEST_DOMAIN)
        assertEquals(TEST_DOMAIN, newLocation)
    }

    @Test
    fun `update location - ok - relative path`() {
        val newLocation = requester.updateLocationWithRedirectPath(TEST_DOMAIN, SUB_PATH)
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    @Test
    fun `update location - ok - replace relative path`() {
        val newLocation = requester.updateLocationWithRedirectPath("$TEST_DOMAIN/some/other/subdir", SUB_PATH)
        assertEquals("$TEST_DOMAIN$SUB_PATH", newLocation)
    }

    @Test
    fun `check redirect to unsecure connection - ok - redirect to http`() {
        assertTrue(
            requester.isRedirectedToNonSecureConnection(false, SECURE_DOMAIN, UNSECURE_DOMAIN
            )
        )
    }

    @Test
    fun `check redirect to unsecure connection - ko - redirect to https from http`() {
        assertFalse(
            requester.isRedirectedToNonSecureConnection(false, UNSECURE_DOMAIN, SECURE_DOMAIN
            )
        )
    }

    @Test
    fun `check redirect to unsecure connection - ko - from https to https`() {
        assertFalse(
            requester.isRedirectedToNonSecureConnection(false, SECURE_DOMAIN, SECURE_DOMAIN)
        )
    }

    @Test
    fun `check redirect to unsecure connection - ok - from https to https with previous http`() {
        assertTrue(
            requester.isRedirectedToNonSecureConnection(true, SECURE_DOMAIN, SECURE_DOMAIN)
        )
    }

    @Test
    fun `check redirect to unsecure connection - ok - from http to http`() {
        assertFalse(
            requester.isRedirectedToNonSecureConnection(false, UNSECURE_DOMAIN, UNSECURE_DOMAIN)
        )
    }

    companion object {
        const val TEST_DOMAIN = "https://cloud.somewhere.com"
        const val SUB_PATH = "/subdir"

        const val SECURE_DOMAIN = "https://cloud.somewhere.com"
        const val UNSECURE_DOMAIN = "http://somewhereelse.org"
    }
}
