package com.owncloud.android.domain.user.model

enum class UserQuotaState(val value: String) {
    NORMAL(value = "normal"),
    NEARING(value = "nearing"),
    CRITICAL(value = "critical"),
    EXCEEDED(value = "exceeded");

    companion object {
        fun fromValue(value: String): UserQuotaState =
            when (value) {
                "normal" -> NORMAL
                "nearing" -> NEARING
                "critical" -> CRITICAL
                "exceeded" -> EXCEEDED
                else -> NORMAL
            }
    }
}
