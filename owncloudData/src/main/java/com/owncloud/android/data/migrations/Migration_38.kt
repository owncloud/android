/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parentId` INTEGER, `owner` TEXT NOT NULL, `remotePath` TEXT NOT NULL, `remoteId` TEXT, `length` INTEGER NOT NULL, `creationTimestamp` INTEGER, `modificationTimestamp` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `etag` TEXT, `permissions` TEXT, `privateLink` TEXT, `storagePath` TEXT, `name` TEXT, `treeEtag` TEXT, `keepInSync` INTEGER, `lastSyncDateForData` INTEGER, `fileShareViaLink` INTEGER, `lastSyncDateForProperties` INTEGER, `needsToUpdateThumbnail` INTEGER NOT NULL, `modifiedAtLastSyncForData` INTEGER, `etagInConflict` TEXT, `fileIsDownloading` INTEGER, `sharedWithSharee` INTEGER, `sharedByLink` INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`localPath` TEXT NOT NULL, `remotePath` TEXT NOT NULL, `accountName` TEXT NOT NULL, `fileSize` INTEGER NOT NULL, `status` INTEGER NOT NULL, `localBehaviour` INTEGER NOT NULL, `forceOverwrite` INTEGER NOT NULL, `transferEndTimestamp` INTEGER, `lastResult` INTEGER, `createdBy` INTEGER NOT NULL, `transferId` TEXT, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }
}
