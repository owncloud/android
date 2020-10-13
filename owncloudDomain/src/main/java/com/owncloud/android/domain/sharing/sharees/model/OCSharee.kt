package com.owncloud.android.domain.sharing.sharees.model

enum class ShareeType(val rawType:Int) {
    USER(0),
    GROUP(1),
    REMOTE(6);

    companion object {
        private val values = values();
        fun getByValue(value: Int) = values.firstOrNull { it.rawType == value }
    }
}

data class OCSharee(
    val label: String,
    val shareType: ShareeType,
    val shareWith: String,
    val isExactMatch: Boolean
)