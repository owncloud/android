package com.owncloud.android.lib.common.http.logging

data class LogRequest(
    val request: Request
)

data class Request(
    val body: String?,
    val headers: Map<String, String>,
    val info: RequestInfo,
)

data class RequestInfo(
    val id: String,
    val method: String,
    val url: String,
)
