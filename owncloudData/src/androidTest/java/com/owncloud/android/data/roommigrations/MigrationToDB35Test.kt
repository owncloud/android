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

package com.owncloud.android.data.roommigrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.filters.SmallTest
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME
import com.owncloud.android.testutil.OC_BACKUP
import org.junit.Assert
import org.junit.Test

@SmallTest
class MigrationToDB35Test : MigrationTest() {

    @Test
    fun migrationFrom34to35_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_34,
            currentVersion = DB_VERSION_35,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo35(database) },
            listOfMigrations = OwncloudDatabase.ALL_MIGRATIONS
        )
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO `$FOLDER_BACKUP_TABLE_NAME`" +
                    "(" +
                    "accountName, " +
                    "behavior, " +
                    "sourcePath, " +
                    "uploadPath, " +
                    "wifiOnly, " +
                    "name, " +
                    "lastSyncTimeStamp)" +
                    " VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                OC_BACKUP.accountName,
                OC_BACKUP.behavior,
                OC_BACKUP.sourcePath,
                OC_BACKUP.uploadPath,
                OC_BACKUP.wifiOnly,
                OC_BACKUP.name,
                OC_BACKUP.lastSyncTimestamp,
            )
        )
    }

    private fun validateMigrationTo35(database: SupportSQLiteDatabase) {
        val backUp = getCount(database, FOLDER_BACKUP_TABLE_NAME)
        Assert.assertEquals(1, backUp)
        database.close()
    }
}
