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
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity.Companion.toContentValues
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.Companion.capabilityBooleanTypeUnknownInt
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@SmallTest
class MigrationToDB28 {

    @Rule
    @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OwncloudDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate27To28() {
        with(
            helper.createDatabase(
                TEST_DB_NAME,
                DB_VERSION_27
            )
        ) {
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cvWithDefaultValues)
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            DB_VERSION_28,
            true,
            OwncloudDatabase.MIGRATION_27_28
        )

        validateMigrationTo28()
    }

    @Test
    fun startInVersion28_containsCorrectData() {
        with(
            helper.createDatabase(
                TEST_DB_NAME,
                DB_VERSION_28
            )
        ) {
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cv)
            insert(CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, cvWithDefaultValues)
            close()
        }

        validateMigrationTo28()
    }

    private fun getMigratedRoomDatabase(): OwncloudDatabase {
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OwncloudDatabase::class.java,
            TEST_DB_NAME
        )
            .addMigrations(OwncloudDatabase.MIGRATION_27_28)
            .build()

        helper.closeWhenFinished(database)
        return database
    }

    private fun validateMigrationTo28() {
        val dbCapability =
            getMigratedRoomDatabase().capabilityDao().getCapabilitiesForAccount(OC_CAPABILITY.accountName!!)
        with(dbCapability) {
            assertEquals(OC_CAPABILITY.accountName, accountName)
            assertEquals(OC_CAPABILITY.versionMayor, versionMayor)
            assertEquals(OC_CAPABILITY.versionMinor, versionMinor)
            assertEquals(OC_CAPABILITY.versionMicro, versionMicro)
            assertEquals(OC_CAPABILITY.versionString, versionString)
            assertEquals(OC_CAPABILITY.versionEdition, versionEdition)
            assertEquals(OC_CAPABILITY.corePollInterval, corePollInterval)
            assertEquals(OC_CAPABILITY.filesSharingApiEnabled.value, filesSharingApiEnabled)
            assertEquals(null, filesSharingSearchMinLength)
            assertEquals(OC_CAPABILITY.filesSharingPublicEnabled.value, filesSharingPublicEnabled)
            assertEquals(OC_CAPABILITY.filesSharingPublicPasswordEnforced.value, filesSharingPublicPasswordEnforced)
            assertEquals(
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedReadOnly.value,
                filesSharingPublicPasswordEnforcedReadOnly
            )
            assertEquals(
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedReadWrite.value,
                filesSharingPublicPasswordEnforcedReadWrite
            )
            assertEquals(
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedUploadOnly.value,
                filesSharingPublicPasswordEnforcedUploadOnly
            )
            assertEquals(OC_CAPABILITY.filesSharingPublicExpireDateEnabled.value, filesSharingPublicExpireDateEnabled)
            assertEquals(OC_CAPABILITY.filesSharingPublicExpireDateDays, filesSharingPublicExpireDateDays)
            assertEquals(OC_CAPABILITY.filesSharingPublicExpireDateEnforced.value, filesSharingPublicExpireDateEnforced)
            assertEquals(OC_CAPABILITY.filesSharingPublicSendMail.value, filesSharingPublicSendMail)
            assertEquals(OC_CAPABILITY.filesSharingPublicUpload.value, filesSharingPublicUpload)
            assertEquals(OC_CAPABILITY.filesSharingPublicMultiple.value, filesSharingPublicMultiple)
            assertEquals(OC_CAPABILITY.filesSharingPublicSupportsUploadOnly.value, filesSharingPublicSupportsUploadOnly)
            assertEquals(OC_CAPABILITY.filesSharingResharing.value, filesSharingResharing)
            assertEquals(OC_CAPABILITY.filesSharingFederationOutgoing.value, filesSharingFederationOutgoing)
            assertEquals(OC_CAPABILITY.filesSharingFederationIncoming.value, filesSharingFederationIncoming)
            assertEquals(OC_CAPABILITY.filesBigFileChunking.value, filesBigFileChunking)
            assertEquals(OC_CAPABILITY.filesUndelete.value, filesUndelete)
            assertEquals(OC_CAPABILITY.filesVersioning.value, filesVersioning)
        }

        val capabilityDefaultValue =
            getMigratedRoomDatabase().capabilityDao().getCapabilitiesForAccount("accountWithDefaultValues")
        with(capabilityDefaultValue) {
            assertEquals("accountWithDefaultValues", accountName)
            assertEquals(OC_CAPABILITY.versionMayor, versionMayor)
            assertEquals(OC_CAPABILITY.versionMinor, versionMinor)
            assertEquals(OC_CAPABILITY.versionMicro, versionMicro)
            assertEquals(null, versionString)
            assertEquals(null, versionEdition)
            assertEquals(OC_CAPABILITY.corePollInterval, corePollInterval)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingApiEnabled)
            assertEquals(null, filesSharingSearchMinLength)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicEnabled)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicPasswordEnforced)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicPasswordEnforcedReadOnly)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicPasswordEnforcedReadWrite)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicPasswordEnforcedUploadOnly)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicExpireDateEnabled)
            assertEquals(0, filesSharingPublicExpireDateDays)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicExpireDateEnforced)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicSendMail)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicUpload)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicMultiple)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingPublicSupportsUploadOnly)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingResharing)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingFederationOutgoing)
            assertEquals(capabilityBooleanTypeUnknownInt, filesSharingFederationIncoming)
            assertEquals(capabilityBooleanTypeUnknownInt, filesBigFileChunking)
            assertEquals(capabilityBooleanTypeUnknownInt, filesUndelete)
            assertEquals(capabilityBooleanTypeUnknownInt, filesVersioning)
        }
    }

    companion object {

        private const val TEST_DB_NAME = "migration-test"

        private const val DB_VERSION_27 = 27
        private const val DB_VERSION_28 = 28

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
