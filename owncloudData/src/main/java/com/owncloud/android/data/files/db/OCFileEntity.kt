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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
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
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_MODIFIED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_OWNER
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PARENT
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PERMISSIONS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_PRIVATE_LINK
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_REMOTE_ID
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_SHARED_VIA_LINK
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_SHARED_WITH_SHAREE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_SPACE_ID
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_STORAGE_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_TREE_ETAG
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILE_UPDATE_THUMBNAIL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta._ID
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ACCOUNT_NAME
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ID
import com.owncloud.android.domain.extensions.isOneOf
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_DIR_UNIX

@Entity(
    tableName = FILES_TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = SpacesEntity::class,
        parentColumns = arrayOf(SPACES_ACCOUNT_NAME, SPACES_ID),
        childColumns = arrayOf(FILE_OWNER, FILE_SPACE_ID),
        onDelete = ForeignKey.CASCADE
    )]
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
    @ColumnInfo(name = "keepInSync")
    var availableOfflineStatus: Int? = null,
    val lastSyncDateForData: Long? = null,
    val lastUsage: Long? = null,
    val fileShareViaLink: Int? = null,
    var needsToUpdateThumbnail: Boolean = false,
    val modifiedAtLastSyncForData: Long? = null,
    val etagInConflict: String? = null,
    val fileIsDownloading: Boolean? = null,
    val sharedWithSharee: Boolean? = false,
    var sharedByLink: Boolean = false,
    val spaceId: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    val isFolder
        get() = mimeType.isOneOf(MIME_DIR, MIME_DIR_UNIX)

    companion object {
        fun fromCursor(cursor: Cursor): OCFileEntity {
            return OCFileEntity(
                parentId = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_PARENT)),
                remotePath = cursor.getString(cursor.getColumnIndexOrThrow(FILE_PATH)),
                owner = cursor.getString(cursor.getColumnIndexOrThrow(FILE_ACCOUNT_OWNER)),
                permissions = cursor.getString(cursor.getColumnIndexOrThrow(FILE_PERMISSIONS)),
                remoteId = cursor.getString(cursor.getColumnIndexOrThrow(FILE_REMOTE_ID)),
                privateLink = cursor.getString(cursor.getColumnIndexOrThrow(FILE_PRIVATE_LINK)),
                creationTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_CREATION)),
                modificationTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_MODIFIED)),
                etag = cursor.getString(cursor.getColumnIndexOrThrow(FILE_ETAG)),
                mimeType = cursor.getStringFromColumnOrEmpty(FILE_CONTENT_TYPE),
                length = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_CONTENT_LENGTH)),
                storagePath = cursor.getString(cursor.getColumnIndexOrThrow(FILE_STORAGE_PATH)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME)),
                treeEtag = cursor.getString(cursor.getColumnIndexOrThrow(FILE_TREE_ETAG)),
                lastSyncDateForData = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_LAST_SYNC_DATE_FOR_DATA)),
                availableOfflineStatus = cursor.getInt(cursor.getColumnIndexOrThrow(FILE_KEEP_IN_SYNC)),
                fileShareViaLink = cursor.getInt(cursor.getColumnIndexOrThrow(FILE_SHARED_VIA_LINK)),
                needsToUpdateThumbnail = cursor.getInt(cursor.getColumnIndexOrThrow(FILE_UPDATE_THUMBNAIL)) == 1,
                modifiedAtLastSyncForData = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)),
                etagInConflict = cursor.getString(cursor.getColumnIndexOrThrow(FILE_ETAG_IN_CONFLICT)),
                fileIsDownloading = cursor.getInt(cursor.getColumnIndexOrThrow(FILE_IS_DOWNLOADING)) == 1,
                sharedWithSharee = cursor.getInt(cursor.getColumnIndexOrThrow(FILE_SHARED_WITH_SHAREE)) == 1
            ).apply {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(_ID))
            }
        }

        private fun Cursor.getStringFromColumnOrEmpty(
            columnName: String
        ): String = getColumnIndex(columnName).takeUnless { it < 0 }?.let { getString(it) }.orEmpty()
    }
}
