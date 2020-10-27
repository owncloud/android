/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.data.files.db

import android.database.Cursor
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_CONTENT_LENGTH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_CONTENT_TYPE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_CREATION
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_ETAG
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_ETAG_IN_CONFLICT
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_IS_DOWNLOADING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_KEEP_IN_SYNC
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_LAST_SYNC_DATE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_MODIFIED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PARENT
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PERMISSIONS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PRIVATE_LINK
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_REMOTE_ID
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_SHARED_VIA_LINK
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_SHARED_WITH_SHAREE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_TREE_ETAG
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_UPDATE_THUMBNAIL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta._ID

@Entity(
    tableName = FILES_TABLE_NAME
)
data class OCFileEntity(
    var parentId: Long? = null,
    val owner: String,
    val remotePath: String,
    val remoteId: String?,
    val length: Long,
    val creationTimestamp: Long?,
    val modificationTimestamp: Long,
    val mimeType: String,
    val etag: String?,
    val permissions: String?,
    val privateLink: String? = null,
    val storagePath: String? = null,
    var name: String? = null,
    val treeEtag: String? = null,

    //TODO: May not needed
    val keepInSync: Int? = null,
    val lastSyncDateForData: Int? = null,
    val fileShareViaLink: Int? = null,
    var lastSyncDateForProperties: Long? = null,
    var needsToUpdateThumbnail: Boolean = false,
    val modifiedAtLastSyncForData: Int? = null,
    val etagInConflict: String? = null,
    val fileIsDownloading: Boolean? = null,
    val sharedWithSharee: Boolean? = false,
    var sharedByLink: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    companion object {
        fun fromCursor(cursor: Cursor): OCFileEntity {
            return OCFileEntity(
                parentId = cursor.getLong(cursor.getColumnIndex(FILE_PARENT)),
                remotePath = cursor.getString(cursor.getColumnIndex(FILE_PATH)),
                owner = cursor.getString(cursor.getColumnIndex(FILE_ACCOUNT_OWNER)),
                permissions = cursor.getString(cursor.getColumnIndex(FILE_PERMISSIONS)),
                remoteId = cursor.getString(cursor.getColumnIndex(FILE_REMOTE_ID)),
                privateLink = cursor.getString(cursor.getColumnIndex(FILE_PRIVATE_LINK)),
                creationTimestamp = cursor.getLong(cursor.getColumnIndex(FILE_CREATION)),
                modificationTimestamp = cursor.getLong(cursor.getColumnIndex(FILE_MODIFIED)),
                etag = cursor.getString(cursor.getColumnIndex(FILE_ETAG)),
                mimeType = cursor.getString(cursor.getColumnIndex(FILE_CONTENT_TYPE)),
                length = cursor.getLong(cursor.getColumnIndex(FILE_CONTENT_LENGTH)),
                storagePath = cursor.getString(cursor.getColumnIndex(FILE_STORAGE_PATH)),
                name = cursor.getString(cursor.getColumnIndex(FILE_NAME)),
                treeEtag = cursor.getString(cursor.getColumnIndex(FILE_TREE_ETAG)),
                lastSyncDateForProperties = cursor.getInt(cursor.getColumnIndex(FILE_LAST_SYNC_DATE)).toLong(),
                lastSyncDateForData = cursor.getInt(cursor.getColumnIndex(FILE_LAST_SYNC_DATE_FOR_DATA)),
                keepInSync = cursor.getInt(cursor.getColumnIndex(FILE_KEEP_IN_SYNC)),
                fileShareViaLink = cursor.getInt(cursor.getColumnIndex(FILE_SHARED_VIA_LINK)),
                needsToUpdateThumbnail = cursor.getInt(cursor.getColumnIndex(FILE_UPDATE_THUMBNAIL)) == 1,
                modifiedAtLastSyncForData = cursor.getInt(cursor.getColumnIndex(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)),
                etagInConflict = cursor.getString(cursor.getColumnIndex(FILE_ETAG_IN_CONFLICT)),
                fileIsDownloading = cursor.getInt(cursor.getColumnIndex(FILE_IS_DOWNLOADING)) == 1,
                sharedWithSharee = cursor.getInt(cursor.getColumnIndex(FILE_SHARED_WITH_SHAREE)) == 1
            ).apply {
                id = cursor.getLong(cursor.getColumnIndex(_ID))
            }
        }
    }
}
