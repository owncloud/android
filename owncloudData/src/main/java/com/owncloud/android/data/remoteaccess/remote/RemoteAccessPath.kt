package com.owncloud.android.data.remoteaccess.remote

import com.owncloud.android.domain.device.model.DevicePathType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessPath(
    @Json(name = "type")
    val type: RemoteAccessPathType,
    @Json(name = "address")
    val address: String,
    @Json(name = "port")
    val port: Int? = null
)

enum class RemoteAccessPathType {
    @Json(name = "local")
    LOCAL,
    @Json(name = "public")
    PUBLIC,
    @Json(name = "remote")
    REMOTE;

    fun mapToDomain(): DevicePathType = when (this) {
        LOCAL -> DevicePathType.LOCAL
        PUBLIC -> DevicePathType.PUBLIC
        REMOTE -> DevicePathType.REMOTE
    }

}

