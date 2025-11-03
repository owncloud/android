package com.owncloud.android.data.mdnsdiscovery.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HCDeviceAboutResponse(
    @Json(name = "certificate_common_name")
    val certificateCommonName: String?
)
