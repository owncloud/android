package com.owncloud.android.lib.common.http.logging

data class LogResponse(
    val response: Response
)

data class Response(
    val body: String,
    val headers: Map<String, String>,
    val info: ResponseInfo,
)

data class ResponseInfo(
    val id: String,
    val method: String,
    val reply: Reply,
    val url: String,
    val bodyLength: Int,
)

data class Reply(
    val cached: Boolean,
    val duration: Long,
    val durationString: String,
    val status: Int,
    val version: String
)

const val DURATION_FORMAT = "duration(%dh, %dmin, %ds, %dms)"
