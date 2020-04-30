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

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.filters.SmallTest
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME
import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.testutil.OC_SHARE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test the migration from database to version 30.
 */
@SmallTest
class MigrationToDB30 : MigrationTest() {

    @Test
    fun migrationFrom27To30_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_27,
            currentVersion = DB_VERSION_30,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo30(database) },
            listOfMigrations = OwncloudDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun migrationFrom28To30_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_28,
            currentVersion = DB_VERSION_30,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo30(database) },
            listOfMigrations = OwncloudDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun startInVersion30_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_30,
            currentVersion = DB_VERSION_30,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { Unit },
            listOfMigrations = arrayOf()
        )
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.run {
            insert(
                CAPABILITIES_TABLE_NAME,
                SQLiteDatabase.CONFLICT_NONE,
                MigrationToDB28.cvWithDefaultValues
            )
            close()
        }
    }

    private fun validateMigrationTo30(database: SupportSQLiteDatabase) {
        val capabilitiesCount = getCount(database, CAPABILITIES_TABLE_NAME)
        assertEquals(1, capabilitiesCount)
        database.close()
    }
}
