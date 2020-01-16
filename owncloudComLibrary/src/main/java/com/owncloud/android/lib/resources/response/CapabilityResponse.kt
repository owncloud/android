/* ownCloud Android Library is available under MIT license
 *   @author Abel García de Prada
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
package com.owncloud.android.lib.resources.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CapabilityResponse(
    @Json(name = NODE_SERVER_VERSION)
    val serverVersion: ServerVersion,
    val capabilities: Capabilities
)

@JsonClass(generateAdapter = true)
data class Capabilities(
    @Json(name = NODE_CORE)
    val coreCapabilities: CoreCapabilities,
    @Json(name = NODE_FILES_SHARING)
    val fileSharingCapabilities: FileSharingCapabilities,
    @Json(name = NODE_FILES)
    val filesCapabilities: FilesCapabilities
)

@JsonClass(generateAdapter = true)
data class CoreCapabilities(
    val pollinterval: Int
)

@JsonClass(generateAdapter = true)
data class FileSharingCapabilities(
    @Json(name = PROPERTY_FILES_SHARING_API_ENABLED)
    val fileSharingApiEnabled: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_SEARCH_MIN_LENGTH)
    val fileSharingSearchMinLength: Int,
    @Json(name = NODE_FILES_SHARING_PUBLIC)
    val fileSharingPublic: FileSharingPublic,
    @Json(name = PROPERTY_FILES_SHARING_RESHARING)
    val fileSharingReSharing: Boolean,
    @Json(name = NODE_FEDERATION)
    val fileSharingFederation: FileSharingFederation
)

@JsonClass(generateAdapter = true)
data class FileSharingPublic(
    val enabled: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_UPLOAD)
    val fileSharingPublicUpload: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_UPLOAD_ONLY)
    val fileSharingPublicUploadOnly: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_MULTIPLE)
    val fileSharingPublicMultiple: Boolean,
    @Json(name = NODE_PASSWORD)
    val fileSharingPublicPassword: FileSharingPublicPassword,
    @Json(name = NODE_EXPIRE_DATE)
    val fileSharingPublicExpireDate: FileSharingPublicExpireDate
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicPassword(
    val enforced: Boolean,
    @Json(name = NODE_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED)
    val enforcedFor: FileSharingPublicPasswordEnforced
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicPasswordEnforced(
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)
    val enforcedReadOnly: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)
    val enforcedReadWrite: Boolean,
    @Json(name = PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)
    val enforcedUploadOnly: Boolean
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicExpireDate(
    val enabled: Boolean,
    val days: Int?,
    val enforced: Boolean?
)

@JsonClass(generateAdapter = true)
data class FileSharingFederation(
    val incoming: Boolean,
    val outgoing: Boolean
)

@JsonClass(generateAdapter = true)
data class FilesCapabilities(
    val bigfilechunking: Boolean,
    val undelete: Boolean,
    val versioning: Boolean
)

@JsonClass(generateAdapter = true)
data class ServerVersion(
    var major: Int = 0,
    var minor: Int = 0,
    var micro: Int = 0,
    var string: String = "",
    var edition: String = ""
)

private const val NODE_SERVER_VERSION = "version"

private const val NODE_CORE = "core"

private const val NODE_FILES_SHARING = "files_sharing"
private const val PROPERTY_FILES_SHARING_API_ENABLED = "api_enabled"
private const val PROPERTY_FILES_SHARING_SEARCH_MIN_LENGTH = "search_min_length"
private const val PROPERTY_FILES_SHARING_RESHARING = "resharing"

private const val NODE_FILES_SHARING_PUBLIC = "public"
private const val PROPERTY_FILES_SHARING_PUBLIC_UPLOAD = "upload"
private const val PROPERTY_FILES_SHARING_PUBLIC_UPLOAD_ONLY = "supports_upload_only"
private const val PROPERTY_FILES_SHARING_PUBLIC_MULTIPLE = "multiple"

private const val NODE_FEDERATION = "federation"

private const val NODE_PASSWORD = "password"
private const val NODE_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED = "enforced_for"
private const val PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY = "read_only"
private const val PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE = "read_write"
private const val PROPERTY_FILES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY = "upload_only"

private const val NODE_EXPIRE_DATE = "expire_date"

private const val NODE_FILES = "files"
