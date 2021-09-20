/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME

val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.run {
            execSQL(
                "CREATE TABLE IF NOT EXISTS `${FOLDER_BACKUP_TABLE_NAME}2` (`accountName` TEXT NOT NULL, `behavior` TEXT NOT NULL, `sourcePath` TEXT NOT NULL, `uploadPath` TEXT NOT NULL, `wifiOnly` INTEGER NOT NULL, `chargingOnly` INTEGER NOT NULL, `name` TEXT NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
            )
            execSQL("ALTER TABLE $FOLDER_BACKUP_TABLE_NAME ADD COLUMN chargingOnly INTEGER NOT NULL DEFAULT '0'")
            execSQL(
                "INSERT INTO `${FOLDER_BACKUP_TABLE_NAME}2` SELECT accountName, behavior, sourcePath, uploadPath, wifiOnly, IFNULL(chargingOnly, '0'), name, lastSyncTimeStamp, id  FROM $FOLDER_BACKUP_TABLE_NAME"
            )
            execSQL("DROP TABLE $FOLDER_BACKUP_TABLE_NAME")
            execSQL("ALTER TABLE ${FOLDER_BACKUP_TABLE_NAME}2 RENAME TO $FOLDER_BACKUP_TABLE_NAME")
        }
    }
}
