/* ownCloud Android Library is available under MIT license
 *   @author Abel García de Prada
 *
 *   Copyright (C) 2023 ownCloud GmbH.
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
package com.owncloud.android.lib.resources.appregistry.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppRegistryResponse(
    @Json(name = "mime-types")
    val value: List<AppRegistryMimeTypeResponse>
)

@JsonClass(generateAdapter = true)
data class AppRegistryMimeTypeResponse(
    @Json(name = "mime_type") val mimeType: String,
    val ext: String? = null,
    @Json(name = "app_providers")
    val appProviders: List<AppRegistryProviderResponse>,
    val name: String? = null,
    val icon: String? = null,
    val description: String? = null,
    @Json(name = "allow_creation")
    val allowCreation: Boolean? = null,
    @Json(name = "default_application")
    val defaultApplication: String? = null
)

@JsonClass(generateAdapter = true)
data class AppRegistryProviderResponse(
    val name: String,
    val icon: String,
)
