/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.data.transfers.db

import android.database.Cursor
import android.provider.BaseColumns._ID
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.TRANSFERS_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_ACCOUNT_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_CREATED_BY
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_FILE_SIZE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_FORCE_OVERWRITE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_LAST_RESULT
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_LOCAL_BEHAVIOUR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_LOCAL_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_REMOTE_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_STATUS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_TRANSFER_ID
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.UPLOAD_UPLOAD_END_TIMESTAMP
import com.owncloud.android.domain.transfers.model.TransferStatus

@Entity(
    tableName = TRANSFERS_TABLE_NAME
)
data class OCTransferEntity(
    val localPath: String,
    val remotePath: String,
    val accountName: String,
    val fileSize: Long,
    val status: Int,
    val localBehaviour: Int,
    val forceOverwrite: Boolean,
    val transferEndTimestamp: Long? = null,
    val lastResult: Int? = null,
    val createdBy: Int,
    val transferId: String? = null,
    val spaceId: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    companion object {
        private const val LEGACY_UPLOAD_IN_PROGRESS = 0
        private const val LEGACY_UPLOAD_FAILED = 1
        private const val LEGACY_LOCAL_BEHAVIOUR_MOVE = 1
        private const val LEGACY_LOCAL_BEHAVIOUR_FORGET = 2

        fun fromCursor(cursor: Cursor): OCTransferEntity {
            val newStatus = when (cursor.getInt(cursor.getColumnIndexOrThrow(UPLOAD_STATUS))) {
                LEGACY_UPLOAD_IN_PROGRESS -> TransferStatus.TRANSFER_QUEUED.value
                LEGACY_UPLOAD_FAILED -> TransferStatus.TRANSFER_FAILED.value
                else -> TransferStatus.TRANSFER_SUCCEEDED.value
            }
            val newLocalBehaviour = cursor.getInt(cursor.getColumnIndexOrThrow(UPLOAD_LOCAL_BEHAVIOUR)).let {
                if (it == LEGACY_LOCAL_BEHAVIOUR_FORGET) LEGACY_LOCAL_BEHAVIOUR_MOVE
                else it
            }
            return OCTransferEntity(
                localPath = cursor.getString(cursor.getColumnIndexOrThrow(UPLOAD_LOCAL_PATH)),
                remotePath = cursor.getString(cursor.getColumnIndexOrThrow(UPLOAD_REMOTE_PATH)),
                accountName = cursor.getString(cursor.getColumnIndexOrThrow(UPLOAD_ACCOUNT_NAME)),
                fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(UPLOAD_FILE_SIZE)),
                status = newStatus,
                localBehaviour = newLocalBehaviour,
                forceOverwrite = cursor.getInt(cursor.getColumnIndexOrThrow(UPLOAD_FORCE_OVERWRITE)) == 1,
                transferEndTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(UPLOAD_UPLOAD_END_TIMESTAMP)),
                lastResult = cursor.getInt(cursor.getColumnIndexOrThrow(UPLOAD_LAST_RESULT)),
                createdBy = cursor.getInt(cursor.getColumnIndexOrThrow(UPLOAD_CREATED_BY)),
                transferId = cursor.getString(cursor.getColumnIndexOrThrow(UPLOAD_TRANSFER_ID))
            ).apply {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(_ID))
            }
        }
    }
}
