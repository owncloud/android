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

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME
import timber.log.Timber

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {

        try {
            // 1. Create new OCShares table
            database.execSQL("CREATE TABLE IF NOT EXISTS `${OCSHARES_TABLE_NAME}2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `file_source` TEXT NOT NULL, `item_source` TEXT NOT NULL, `share_type` INTEGER NOT NULL, `shate_with` TEXT, `path` TEXT NOT NULL, `permissions` INTEGER NOT NULL, `shared_date` INTEGER NOT NULL, `expiration_date` INTEGER NOT NULL, `token` TEXT, `shared_with_display_name` TEXT, `share_with_additional_info` TEXT, `is_directory` INTEGER NOT NULL, `user_id` INTEGER NOT NULL, `id_remote_shared` TEXT NOT NULL, `owner_share` TEXT NOT NULL, `name` TEXT, `url` TEXT)")

            // 2. Get old OCShares and insert them into the new table
            val cursor = database.query("SELECT * FROM $OCSHARES_TABLE_NAME")
            cursor.use {
                while (it.moveToNext()) {
                    val cv = ContentValues()
                    cv.put("id", it.getInt(it.getColumnIndex("id")))
                    cv.put("file_source", it.getString(it.getColumnIndex("file_source")))
                    cv.put("item_source", it.getString(it.getColumnIndex("item_source")))
                    cv.put("share_type", it.getInt(it.getColumnIndex("share_type")))
                    cv.put("shate_with", it.getString(it.getColumnIndex("shate_with")))
                    cv.put("path", it.getString(it.getColumnIndex("path")))
                    cv.put("permissions", it.getInt(it.getColumnIndex("permissions")))
                    cv.put("shared_date", it.getInt(it.getColumnIndex("shared_date")))
                    cv.put("expiration_date", it.getInt(it.getColumnIndex("expiration_date")))
                    cv.put("token", it.getString(it.getColumnIndex("token")))
                    cv.put("shared_with_display_name", it.getString(it.getColumnIndex("shared_with_display_name")))
                    cv.put("share_with_additional_info", it.getString(it.getColumnIndex("share_with_additional_info")))
                    cv.put("is_directory", it.getInt(it.getColumnIndex("is_directory")))
                    cv.put("user_id", it.getInt(it.getColumnIndex("user_id")))
                    cv.put("id_remote_shared", it.getString(it.getColumnIndex("id_remote_shared")))
                    cv.put("owner_share", it.getString(it.getColumnIndex("owner_share")))
                    cv.put("name", it.getString(it.getColumnIndex("name")))
                    cv.put("url", it.getString(it.getColumnIndex("url")))

                    database.insert("${OCSHARES_TABLE_NAME}2", 0, cv)
                }
            }

            // 3. Drop old table and rename new one.
            database.execSQL("DROP TABLE $OCSHARES_TABLE_NAME")
            database.execSQL("ALTER TABLE ${OCSHARES_TABLE_NAME}2 RENAME TO $OCSHARES_TABLE_NAME")

        } catch (e: SQLiteException) {
            Timber.e(e, "SQLiteException in migrate from database version 1 to version 2")
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate database version 1 to version 2")
        }
    }

}
