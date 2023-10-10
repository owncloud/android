/* ownCloud Android Library is available under MIT license
 *
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
 */
package com.owncloud.android.lib.resources

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Response retrieved by OCS Rest API, used to obtain capabilities, shares and user info among others.
// More info: https://doc.owncloud.com/server/developer_manual/core/apis/ocs-capabilities.html
@JsonClass(generateAdapter = true)
data class CommonOcsResponse<T>(
    val ocs: OCSResponse<T>
)

@JsonClass(generateAdapter = true)
data class OCSResponse<T>(
    val meta: MetaData,
    val data: T?
)

@JsonClass(generateAdapter = true)
data class MetaData(
    val status: String,
    @Json(name = "statuscode")
    val statusCode: Int,
    val message: String?,
    @Json(name = "itemsperpage")
    val itemsPerPage: String?,
    @Json(name = "totalitems")
    val totalItems: String?
)
