package com.owncloud.android.data.remoteaccess.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessDevicePathsResponse(
    @Json(name = "seagateDeviceID")
    val seagateDeviceId: String,
    @Json(name = "paths")
    val paths: List<RemoteAccessPath>
)

