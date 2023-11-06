package com.owncloud.android.lib.common.http.logging

data class LogRequest(
    val requestId: String,
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val bodyLength: Long,
    val bodyContentType: String,
    val body: String,
)
