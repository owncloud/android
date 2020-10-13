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
package com.owncloud.android.lib.resources.shares.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This was modeled according to the documentation:
 * https://doc.owncloud.com/server/developer_manual/core/apis/ocs-recipient-api.html#get-shares-recipients
 */
@JsonClass(generateAdapter = true)
data class ShareeOcsResponse(
    @Json(name = "exact")
    val exact: ExactSharees?,
    @Json(name = "groups")
    val groups: List<ShareeItem>?,
    @Json(name = "remotes")
    val remotes: List<ShareeItem>?,
    @Json(name = "users")
    val users: List<ShareeItem>?
) {
    fun getFlatRepresentation()
            = ArrayList<ShareeItem>().apply {
        if(exact != null) {
            addAll(exact.getFlatRepresentation())
        }
        if(users != null) {
            addAll(users)
        }
        if(remotes != null) {
            addAll(remotes)
        }
        if(groups != null) {
            addAll(groups)
        }
    }
}

@JsonClass(generateAdapter = true)
data class ExactSharees(
    @Json(name = "groups")
    val groups: List<ShareeItem>?,
    @Json(name = "remotes")
    val remotes: List<ShareeItem>?,
    @Json(name = "users")
    val users: List<ShareeItem>?
) {
    fun getFlatRepresentation()
    = ArrayList<ShareeItem>().apply {
        if(users != null) {
            addAll(users)
        }
        if(remotes != null) {
            addAll(remotes)
        }
        if(groups != null) {
            addAll(groups)
        }
    }
}

@JsonClass(generateAdapter = true)
data class ShareeItem(
    @Json(name = "label")
    val label: String?,
    @Json(name = "value")
    val value: ShareeValue?
)

@JsonClass(generateAdapter = true)
data class ShareeValue(
    @Json(name = "shareType")
    val shareType: Int?,
    @Json(name = "shareWith")
    val shareWith: String?
)
