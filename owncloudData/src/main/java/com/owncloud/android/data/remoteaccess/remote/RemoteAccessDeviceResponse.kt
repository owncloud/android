package com.owncloud.android.data.remoteaccess.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessDeviceResponse(
    @Json(name = "seagateDeviceID")
    val seagateDeviceId: String,
    @Json(name = "friendlyName")
    val friendlyName: String,
    @Json(name = "hostname")
    val hostname: String,
    @Json(name = "certificateCommonName")
    val certificateCommonName: String
)

