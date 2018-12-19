package com.owncloud.android.db.shares

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.owncloud.android.lib.resources.shares.ShareType

@Entity(tableName = "shares_table")
data class Share(
        @PrimaryKey val id: Long,
        val fileSource: Long,
        val itemSource: Long,
        val type: Int,
        val shareWith: String,
        val path: String,
        val permissions: Int,
        val sharedDate: Long,
        val expirationDate: Long,
        val token: String,
        val sharedWithDisplayName: String,
        val name: String,
        val isFolder: Boolean,
        val userId: Long,
        val remoteId: Long,
        val shareLink: String,
        val accountOwner: String
)