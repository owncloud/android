package com.owncloud.android.data.mdnsdiscovery.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response model for device status endpoint: GET /api/v1/status
 */
@JsonClass(generateAdapter = true)
data class HCDeviceStatusResponse(
    @Json(name = "OOBE")
    val oobe: HCOOBEStatus,
    @Json(name = "apps")
    val apps: HCAppsStatus,
    @Json(name = "state")
    val state: String
)

@JsonClass(generateAdapter = true)
data class HCOOBEStatus(
    @Json(name = "done")
    val done: Boolean
)

@JsonClass(generateAdapter = true)
data class HCAppsStatus(
    @Json(name = "files")
    val files: String,
    @Json(name = "photos")
    val photos: String
)

