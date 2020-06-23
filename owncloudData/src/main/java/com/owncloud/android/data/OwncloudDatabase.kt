/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
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

package com.owncloud.android.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.owncloud.android.data.capabilities.db.OCCapabilityDao
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.data.migrations.MIGRATION_27_28
import com.owncloud.android.data.migrations.MIGRATION_28_29
import com.owncloud.android.data.migrations.MIGRATION_29_30
import com.owncloud.android.data.migrations.MIGRATION_30_31
import com.owncloud.android.data.migrations.MIGRATION_31_32
import com.owncloud.android.data.migrations.MIGRATION_32_33
import com.owncloud.android.data.sharing.shares.db.OCShareDao
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.data.user.db.UserDao
import com.owncloud.android.data.user.db.UserQuotaEntity

@Database(
    entities = [
        OCCapabilityEntity::class,
        OCFileEntity::class,
        OCShareEntity::class,
        UserQuotaEntity::class
    ],
    version = ProviderMeta.DB_VERSION,
    exportSchema = true
)
abstract class OwncloudDatabase : RoomDatabase() {
    abstract fun capabilityDao(): OCCapabilityDao
    abstract fun fileDao(): FileDao
    abstract fun shareDao(): OCShareDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: OwncloudDatabase? = null

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_27_28,
            MIGRATION_28_29,
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
            MIGRATION_32_33
        )

        fun getDatabase(
            context: Context
        ): OwncloudDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OwncloudDatabase::class.java,
                    ProviderMeta.NEW_DB_NAME
                ).addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        @VisibleForTesting
        fun switchToInMemory(context: Context, vararg migrations: Migration) {
            INSTANCE = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                OwncloudDatabase::class.java
            ).addMigrations(*migrations)
                .build()
        }
    }
}
