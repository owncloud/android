package com.owncloud.android.lib.common.http.logging

data class LogResponse(
    val requestId: String,
    val method: String,
    val url: String,
    val code: Int,
    val headers: Map<String, String>,
    val bodyLength: Int,
    val bodyContentType: String,
    val body: String,
)
