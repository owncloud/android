/**
 *   ownCloud Android client application
 *
 *   @author Abel García de Prada
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parentId` INTEGER, `owner` TEXT NOT NULL, `remotePath` TEXT NOT NULL, `remoteId` TEXT, `length` INTEGER NOT NULL, `creationTimestamp` INTEGER, `modifiedTimestamp` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `etag` TEXT, `permissions` TEXT, `privateLink` TEXT, `storagePath` TEXT, `name` TEXT, `treeEtag` TEXT, `lastSyncDate` INTEGER, `keepInSync` INTEGER, `lastSyncDateForData` INTEGER, `fileShareViaLink` INTEGER, `needsToUpdateThumbnail` INTEGER, `publicLink` TEXT, `modifiedAtLastSyncForData` INTEGER, `etagInConflict` TEXT, `fileIsDownloading` INTEGER, `sharedWithSharee` INTEGER)")
    }
}
