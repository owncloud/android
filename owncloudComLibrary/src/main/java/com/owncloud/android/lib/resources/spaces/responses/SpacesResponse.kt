/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.lib.resources.spaces.responses

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpacesResponseWrapper(
    val value: List<SpaceResponse>
)

@JsonClass(generateAdapter = true)
data class SpaceResponse(
    val description: String?,
    val driveAlias: String,
    val driveType: String,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val owner: OwnerResponse?,
    val quota: QuotaResponse?,
    val root: RootResponse,
    val special: List<SpecialResponse>?,
    val webUrl: String,
)

@JsonClass(generateAdapter = true)
data class OwnerResponse(
    val user: UserResponse
)

@JsonClass(generateAdapter = true)
data class QuotaResponse(
    val remaining: Long?,
    val state: String?,
    val total: Long,
    val used: Long?,
)

@JsonClass(generateAdapter = true)
data class RootResponse(
    val eTag: String?,
    val id: String,
    val webDavUrl: String,
    val deleted: DeleteResponse?,
)

@JsonClass(generateAdapter = true)
data class SpecialResponse(
    val eTag: String,
    val file: FileResponse,
    val id: String,
    val lastModifiedDateTime: String,
    val name: String,
    val size: Int,
    val specialFolder: SpecialFolderResponse,
    val webDavUrl: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String
)

@JsonClass(generateAdapter = true)
data class FileResponse(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class DeleteResponse(
    val state: String,
)

@JsonClass(generateAdapter = true)
data class SpecialFolderResponse(
    val name: String
)
