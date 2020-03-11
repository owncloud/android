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
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME
import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.data.sharing.shares.db.OCShareEntity.Companion.toContentValues
import com.owncloud.android.testutil.OC_SHARE
import org.junit.Assert
import org.junit.Test

/**
 * Test the migration from database to version 29.
 */
@SmallTest
class MigrationToDB29 : MigrationTest() {
    private val shareMapper: OCShareMapper = OCShareMapper()

    @Test
    fun migrationFrom27To29_containsCorrectData() {
        helper.createDatabase(TEST_DB_NAME, DB_VERSION_27).run {
            insertDataToTest(this)
        }

        helper.runMigrationsAndValidate(
            TEST_DB_NAME, DB_VERSION_29, true, *OwncloudDatabase.ALL_MIGRATIONS
        ).also { validateMigrationTo29(it) }
    }

    @Test
    fun migrationFrom28To29_containsCorrectData() {
        helper.createDatabase(TEST_DB_NAME, DB_VERSION_28).run {
            insertDataToTest(this)
        }
        helper.runMigrationsAndValidate(
            TEST_DB_NAME, DB_VERSION_29, true, *OwncloudDatabase.ALL_MIGRATIONS
        ).also { validateMigrationTo29(it) }
    }

    @Test
    fun startInVersion29_containsCorrectData() {
        helper.createDatabase(
            TEST_DB_NAME,
            DB_VERSION_29
        ).also { validateMigrationTo29(it) }
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.run {
            insert(OCSHARES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, toContentValues(shareMapper.toEntity(OC_SHARE)!!))
            insert(
                OCSHARES_TABLE_NAME,
                SQLiteDatabase.CONFLICT_NONE,
                toContentValues(shareMapper.toEntity(OC_SHARE.copy(id = 499))!!)
            )
            close()
        }
    }

    private fun validateMigrationTo29(database: SupportSQLiteDatabase) {
        val count = getCount(database, OCSHARES_TABLE_NAME)
        Assert.assertEquals(0, count)
        database.close()
    }
}
