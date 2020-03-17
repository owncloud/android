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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.filters.SmallTest
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity.Companion.toContentValues
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test the migration from database to version 28.
 */
@SmallTest
class MigrationToDB28 : MigrationTest() {

    @Test
    fun migrate27To28() {
        performMigrationTest(
            previousVersion = DB_VERSION_27,
            currentVersion = DB_VERSION_28,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo28(database) },
            listOfMigrations = arrayOf(OwncloudDatabase.MIGRATION_27_28)
        )
    }

    @Test
    fun startInVersion28_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_28,
            currentVersion = DB_VERSION_28,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo28(database) },
            listOfMigrations = arrayOf(OwncloudDatabase.MIGRATION_27_28)
        )
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.run {
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cvWithDefaultValues)
            close()
        }
    }

    private fun validateMigrationTo28(database: SupportSQLiteDatabase) {
        val count = getCount(database, CAPABILITIES_TABLE_NAME)
        assertEquals(2, count)
        database.close()
    }

    companion object {
        private val cvWithDefaultValues = ContentValues().apply {
            put(CAPABILITIES_ACCOUNT_NAME, "accountWithDefaultValues")
            put(CAPABILITIES_VERSION_MAYOR, OC_CAPABILITY.versionMayor)
            put(CAPABILITIES_VERSION_MINOR, OC_CAPABILITY.versionMinor)
            put(CAPABILITIES_VERSION_MICRO, OC_CAPABILITY.versionMicro)
            put(CAPABILITIES_CORE_POLLINTERVAL, OC_CAPABILITY.corePollInterval)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS, OC_CAPABILITY.filesSharingPublicExpireDateDays)
        }

        private val cv = toContentValues(OC_CAPABILITY.copy(filesSharingSearchMinLength = null))
    }
}
