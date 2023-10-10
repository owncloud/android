/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares.responses

import com.owncloud.android.lib.resources.CommonOcsResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.Type

class ShareeResponseTest {

    lateinit var adapter: JsonAdapter<CommonOcsResponse<ShareeOcsResponse>>

    private fun loadResponses(fileName: String) =
        adapter.fromJson(File(fileName).readText())

    @Before
    fun prepare() {
        val moshi = Moshi.Builder().build()
        val type: Type = Types.newParameterizedType(CommonOcsResponse::class.java, ShareeOcsResponse::class.java)
        adapter = moshi.adapter(type)
    }

    @Test
    fun `check structure - ok - contains meta`() {
        val response = loadResponses(EXAMPLE_RESPONSE_JSON)!!
        assertEquals("OK", response.ocs.meta.message!!)
        assertEquals(200, response.ocs.meta.statusCode)
        assertEquals("ok", response.ocs.meta.status)
        assertTrue(response.ocs.meta.itemsPerPage?.isEmpty()!!)
        assertTrue(response.ocs.meta.totalItems?.isEmpty()!!)
    }

    @Test
    fun `example response - ok - correct sturcture`() {
        val response = loadResponses(EXAMPLE_RESPONSE_JSON)!!
        assertEquals(2, response.ocs.data?.groups?.size)
        assertEquals(0, response.ocs.data?.remotes?.size)
        assertEquals(2, response.ocs.data?.users?.size)
        assertEquals(0, response.ocs.data?.exact?.groups?.size)
        assertEquals(0, response.ocs.data?.exact?.remotes?.size)
        assertEquals(1, response.ocs.data?.exact?.users?.size)
        assertEquals("user1@user1.com", response.ocs.data?.users?.get(0)?.value?.additionalInfo)
        assertNull(response.ocs.data?.users?.get(1)?.value?.additionalInfo)
    }

    @Test
    fun `check empty response - ok - parsing ok`() {
        val response = loadResponses(EMPTY_RESPONSE_JSON)!!
        assertTrue(response.ocs.data?.exact?.groups?.isEmpty()!!)
        assertTrue(response.ocs.data?.exact?.remotes?.isEmpty()!!)
        assertTrue(response.ocs.data?.exact?.users?.isEmpty()!!)
        assertTrue(response.ocs.data?.groups?.isEmpty()!!)
        assertTrue(response.ocs.data?.remotes?.isEmpty()!!)
        assertTrue(response.ocs.data?.users?.isEmpty()!!)
    }

    companion object {
        private const val RESOURCES_PATH =
            "src/test/responses/com.owncloud.android.lib.resources.sharees.responses"
        const val EXAMPLE_RESPONSE_JSON = "$RESOURCES_PATH/example_sharee_response.json"
        const val EMPTY_RESPONSE_JSON = "$RESOURCES_PATH/empty_sharee_response.json"
    }
}
