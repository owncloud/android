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

package com.owncloud.android.data.roommigrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.filters.SmallTest
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME
import com.owncloud.android.testutil.OC_SHARE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test the migration from database to version 33.
 */
@SmallTest
class MigrationToDB33Test : MigrationTest() {

    @Test
    fun migrationFrom32to33_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_32,
            currentVersion = DB_VERSION_33,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo33(database) },
            listOfMigrations = OwncloudDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun startInVersion33_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_33,
            currentVersion = DB_VERSION_33,
            recoverPreviousData = false,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { },
            listOfMigrations = arrayOf()
        )
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO `$OCSHARES_TABLE_NAME`" +
                    "(" +
                    "share_type, " +
                    "shate_with, " +
                    "path, " +
                    "permissions, " +
                    "shared_date, " +
                    "expiration_date, " +
                    "token, " +
                    "shared_with_display_name, " +
                    "share_with_additional_info, " +
                    "is_directory, " +
                    "id_remote_shared, " +
                    "owner_share, " +
                    "name, " +
                    "url, " +
                    "user_id, " +
                    "item_source, " +
                    "file_source)" +
                    " VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                OC_SHARE.shareType,
                OC_SHARE.shareWith,
                OC_SHARE.path,
                OC_SHARE.permissions,
                OC_SHARE.sharedDate,
                OC_SHARE.expirationDate,
                OC_SHARE.token,
                OC_SHARE.sharedWithDisplayName,
                OC_SHARE.sharedWithAdditionalInfo,
                OC_SHARE.isFolder,
                OC_SHARE.remoteId,
                OC_SHARE.accountOwner,
                OC_SHARE.name,
                OC_SHARE.shareLink,
                1,
                1,
                1
            )
        )
    }

    private fun validateMigrationTo33(database: SupportSQLiteDatabase) {
        val sharesCount = getCount(database, OCSHARES_TABLE_NAME)
        assertEquals(1, sharesCount)
        database.close()
    }
}
