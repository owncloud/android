package com.owncloud.android.domain.user.model

enum class UserQuotaStatus(val value: String) {
    UNKNOWN (value = ""),
    NORMAL(value = "normal"),
    NEARING(value = "nearing"),
    CRITICAL(value = "critical"),
    EXCEEDED(value = "exceeded");

    companion object {
        fun fromValue(value: String): UserQuotaStatus =
            when (value) {
                "normal" -> NORMAL
                "nearing" -> NEARING
                "critical" -> CRITICAL
                "exceeded" -> EXCEEDED
                else -> UNKNOWN
            }
    }
}