package com.owncloud.android.lib.common.http.logging

data class LogResponse(
    val response: Response
)

data class Response(
    val body: Body?,
    val headers: Map<String, String>,
    val info: ResponseInfo,
)

data class ResponseInfo(
    val id: String,
    val method: String,
    val reply: Reply,
    val url: String,
)

data class Reply(
    val cached: Boolean,
    val duration: Long,
    val durationString: String,
    val status: Int,
    val version: String,
)

data class Body(
    val data: String?,
    val length: Int,
)

const val DURATION_FORMAT = "duration(%dh, %dmin, %ds, %dms)"
