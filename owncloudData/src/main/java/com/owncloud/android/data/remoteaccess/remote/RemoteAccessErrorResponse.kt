package com.owncloud.android.data.remoteaccess.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessErrorResponse(
    @Json(name = "error")
    val error: String,
    @Json(name = "message")
    val message: String
)

