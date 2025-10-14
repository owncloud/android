package com.owncloud.android.data.remoteaccess.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessInitiateRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "clientId")
    val clientId: String,
    @Json(name = "clientFriendlyName")
    val clientFriendlyName: String
)

