package com.owncloud.android.data.remoteaccess.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessTokenResponse(
    @Json(name = "accessToken")
    val accessToken: String,
    @Json(name = "refreshToken")
    val refreshToken: String,
    @Json(name = "tokenType")
    val tokenType: String,
    @Json(name = "expiresIn")
    val expiresIn: Int
)

