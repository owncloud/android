package com.owncloud.android.domain.searches.model

data class SavedSearch(
    val id: Long? = null,
    val accountName: String,
    val name: String?,
    val searchPattern: String,
    val ignoreCase: Boolean = true,
    val minSize: Long = 0L,
    val maxSize: Long = Long.MAX_VALUE,
    val mimePrefix: String = "",
    val minDate: Long = 0L,
    val maxDate: Long = Long.MAX_VALUE,
    val createdAt: Long = System.currentTimeMillis(),
)
