/* ownCloud Android Library is available under MIT license
 *
 * Copyright (C) 2026 ownCloud GmbH.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.owncloud.android.lib.resources.status.responses

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CapabilityResponseTest {

    private val adapter: JsonAdapter<CapabilityResponse> = Moshi.Builder()
        .build()
        .adapter(CapabilityResponse::class.java)

    @Test
    fun `toRemoteCapability maps misspelled search min length field`() {
        val capabilityResponse = adapter.fromJson(
            """
            {
              "version": {
                "major": 10,
                "minor": 0,
                "micro": 0,
                "string": "10.0.0",
                "edition": "community"
              },
              "capabilities": {
                "files_sharing": {
                  "search_min_lenght": 5
                }
              }
            }
            """.trimIndent()
        )

        assertNotNull(capabilityResponse)
        assertEquals(5, capabilityResponse?.toRemoteCapability()?.filesSharingSearchMinLength)
    }

    @Test
    fun `toRemoteCapability maps correctly spelled search min length field`() {
        val capabilityResponse = adapter.fromJson(
            """
            {
              "version": {
                "major": 10,
                "minor": 0,
                "micro": 0,
                "string": "10.0.0",
                "edition": "community"
              },
              "capabilities": {
                "files_sharing": {
                  "search_min_length": 4
                }
              }
            }
            """.trimIndent()
        )

        assertNotNull(capabilityResponse)
        assertEquals(4, capabilityResponse?.toRemoteCapability()?.filesSharingSearchMinLength)
    }
}
