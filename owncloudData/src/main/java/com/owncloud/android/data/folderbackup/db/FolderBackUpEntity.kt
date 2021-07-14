/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.data.folderbackup.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta

@Entity(tableName = ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME)
data class FolderBackUpEntity(
    val accountName: String,
    val behavior: String,
    val sourcePath: String,
    val uploadPath: String,
    val wifiOnly: Boolean,
    @ColumnInfo(name = folderBackUpEntityNameField) val name: String,
    val lastSyncTimestamp: Long,
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {
        internal const val folderBackUpEntityNameField = "name"
    }
}
