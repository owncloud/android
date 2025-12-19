/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2024 ownCloud GmbH.
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

import com.owncloud.android.lib.resources.roles.responses.RoleResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpacesResponseWrapper(
    val value: List<SpaceResponse>
)

@JsonClass(generateAdapter = true)
data class SpaceResponse(
    val description: String?,
    val driveAlias: String?,
    val driveType: String,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val owner: OwnerResponse?,
    val quota: QuotaResponse?,
    val root: RootResponse,
    val special: List<SpecialResponse>?,
    val webUrl: String?,
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
    val permissions: List<PermissionsResponse>?
)

@JsonClass(generateAdapter = true)
data class SpecialResponse(
    val eTag: String,
    val file: FileResponse,
    val id: String,
    val lastModifiedDateTime: String?,
    val name: String,
    val size: Int,
    val specialFolder: SpecialFolderResponse,
    val webDavUrl: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val displayName: String
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

@JsonClass(generateAdapter = true)
data class PermissionsResponse(
    // Member response
    val expirationDateTime: String?,
    val grantedToV2: GrantedToV2Response?,
    val id: String?,
    val roles: List<String>?,

    // Link response
    val createDateTime: String?,
    val hasPassword: Boolean?,
    val link: LinkInfoResponse?
)

@JsonClass(generateAdapter = true)
data class GrantedToV2Response(
    val user: UserResponse?,
    val group: GroupResponse?
)

@JsonClass(generateAdapter = true)
data class GroupResponse(
    val id: String,
    val displayName: String
)

@JsonClass(generateAdapter = true)
data class SpacePermissionsResponse(
    @Json(name = "@libre.graph.permissions.actions.allowedValues")
    val actions: List<String>,
    @Json(name = "@libre.graph.permissions.roles.allowedValues")
    val roles: List<RoleResponse>,
    @Json(name = "value")
    val members: List<PermissionsResponse>
)

@JsonClass(generateAdapter = true)
data class LinkInfoResponse(
    @Json(name = "@libre.graph.displayName")
    val displayName: String,
    @Json(name = "@libre.graph.quickLink")
    val quickLink: Boolean,
    val preventsDownload: Boolean,
    val type: String,
    val webUrl: String,
)

