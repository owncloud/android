/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.data.ProviderMeta
import timber.log.Timber

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.run {
            execSQL("ALTER TABLE ${ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME} ADD COLUMN `spaceId` TEXT")
            execSQL("ALTER TABLE ${ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME} ADD COLUMN `spaceName` TEXT")
            val query = "SELECT `accountName` FROM ${ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME}"
            val cursor = database.query(query)
            cursor.use {
                while (it.moveToNext()) {
                    val accountName = it.getString(it.getColumnIndexOrThrow("accountName"))

                    val spacePersonalQuery = "SELECT `space_id`, `name` FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}\n" +
                            "WHERE `account_name` = '${accountName}' AND `drive_type`= 'personal'"

                    val cursorSpacePersonal = database.query(spacePersonalQuery)
                    cursorSpacePersonal.use {
                        if (cursorSpacePersonal.moveToFirst()) {
                            do {
                                val spaceId = cursorSpacePersonal.getString(cursorSpacePersonal.getColumnIndexOrThrow("space_id"))
                                val spaceName = cursorSpacePersonal.getString(cursorSpacePersonal.getColumnIndexOrThrow("name"))

                                execSQL("UPDATE `folder_backup` SET `spaceId` = CASE WHEN '${spaceId}' IS NOT NULL THEN '${spaceId}' ELSE NULL END")
                                execSQL("UPDATE `folder_backup` SET `spaceName` = CASE WHEN '${spaceName}' IS NOT NULL THEN '${spaceName}' ELSE NULL END")

                            } while (cursorSpacePersonal.moveToNext())
                        } else {
                            Timber.d("the cursor is empty")
                        }
                    }
                }
            }
        }
    }
}
