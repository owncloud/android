package com.owncloud.android.data.searches.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.SAVED_SEARCHES_TABLE_NAME

@Entity(
    tableName = SAVED_SEARCHES_TABLE_NAME
)
data class SavedSearchEntity(
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
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}


