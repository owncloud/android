package com.owncloud.android.domain.remoteaccess.model

data class RemoteAccessPath(
    val type: RemoteAccessPathType,
    val address: String,
    val port: Int?
)

enum class RemoteAccessPathType {
    LOCAL,
    PUBLIC,
    REMOTE
}

