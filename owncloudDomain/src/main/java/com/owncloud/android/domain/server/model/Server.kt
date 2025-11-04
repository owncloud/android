package com.owncloud.android.domain.server.model

data class Server(
    val hostName: String,
    val hostUrl: String,
    val certificateCommonName: String = "",
)