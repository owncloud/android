package com.owncloud.android.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.owncloud.android.db.shares.Share
import com.owncloud.android.db.shares.ShareDao

@Database(entities = [Share::class], version = 1)
abstract class OwncloudDatabase : RoomDatabase() {
    abstract fun shareDao(): ShareDao

    companion object {
        @Volatile
        private var INSTANCE: OwncloudDatabase? = null
        const val DATABASE_NAME = "owncloud_database"

        fun getDatabase(
            context: Context
        ): OwncloudDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OwncloudDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}