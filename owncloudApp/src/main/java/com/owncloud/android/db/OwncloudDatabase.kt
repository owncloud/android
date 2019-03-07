/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.db.OCShareDao

@Database(entities = [OCShare::class], version = ProviderMeta.DB_VERSION, exportSchema = false)
abstract class OwncloudDatabase : RoomDatabase() {
    abstract fun shareDao(): OCShareDao

    companion object {
        @Volatile
        private var INSTANCE: OwncloudDatabase? = null

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
                ).build()
                INSTANCE = instance
                instance
            }
        }

        @VisibleForTesting
        fun switchToInMemory(context: Context) {
            INSTANCE = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                OwncloudDatabase::class.java
            ).build()
        }
    }
}
