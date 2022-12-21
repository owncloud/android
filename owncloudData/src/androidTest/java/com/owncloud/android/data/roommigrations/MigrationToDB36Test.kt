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
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.testutil.OC_CAPABILITY
import org.junit.Assert
import org.junit.Test

@SmallTest
class MigrationToDB36Test : MigrationTest() {

    @Test
    fun migrationFrom35to36_containsCorrectData() {
        performMigrationTest(
            previousVersion = DB_VERSION_35,
            currentVersion = DB_VERSION_36,
            insertData = { database -> insertDataToTest(database) },
            validateMigration = { database -> validateMigrationTo36(database) },
            listOfMigrations = OwncloudDatabase.ALL_MIGRATIONS
        )
    }

    private fun insertDataToTest(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO `$CAPABILITIES_TABLE_NAME`" +
                    "(" +
                    "account, " +
                    "version_mayor, " +
                    "version_minor, " +
                    "version_micro, " +
                    "version_string, " +
                    "version_edition, " +
                    "core_pollinterval, " +
                    "dav_chunking_version, " +
                    "sharing_api_enabled, " +
                    "sharing_public_enabled, " +
                    "sharing_public_password_enforced, " +
                    "sharing_public_password_enforced_read_only, " +
                    "sharing_public_password_enforced_read_write, " +
                    "sharing_public_password_enforced_public_only, " +
                    "sharing_public_expire_date_enabled, " +
                    "sharing_public_expire_date_days, " +
                    "sharing_public_expire_date_enforced, " +
                    "sharing_public_upload, " +
                    "sharing_public_multiple, " +
                    "supports_upload_only, " +
                    "sharing_resharing, " +
                    "sharing_federation_outgoing, " +
                    "sharing_federation_incoming, " +
                    "files_bigfilechunking, " +
                    "files_undelete, " +
                    "files_versioning)" +
                    " VALUES " +
                    "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            arrayOf(
                OC_CAPABILITY.accountName,
                OC_CAPABILITY.versionMajor,
                OC_CAPABILITY.versionMinor,
                OC_CAPABILITY.versionMicro,
                OC_CAPABILITY.versionString,
                OC_CAPABILITY.versionEdition,
                OC_CAPABILITY.corePollInterval,
                OC_CAPABILITY.davChunkingVersion,
                OC_CAPABILITY.filesSharingApiEnabled,
                OC_CAPABILITY.filesSharingPublicEnabled,
                OC_CAPABILITY.filesSharingPublicPasswordEnforced,
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedReadOnly,
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedReadWrite,
                OC_CAPABILITY.filesSharingPublicPasswordEnforcedUploadOnly,
                OC_CAPABILITY.filesSharingPublicExpireDateEnabled,
                OC_CAPABILITY.filesSharingPublicExpireDateDays,
                OC_CAPABILITY.filesSharingPublicExpireDateEnforced,
                OC_CAPABILITY.filesSharingPublicUpload,
                OC_CAPABILITY.filesSharingPublicMultiple,
                OC_CAPABILITY.filesSharingPublicSupportsUploadOnly,
                OC_CAPABILITY.filesSharingResharing,
                OC_CAPABILITY.filesSharingFederationOutgoing,
                OC_CAPABILITY.filesSharingFederationIncoming,
                OC_CAPABILITY.filesBigFileChunking,
                OC_CAPABILITY.filesUndelete,
                OC_CAPABILITY.filesVersioning
            )
        )
    }

    private fun validateMigrationTo36(database: SupportSQLiteDatabase) {
        val capabilityCount = getCount(database, CAPABILITIES_TABLE_NAME)
        Assert.assertEquals(1, capabilityCount)
        database.close()
    }
}
