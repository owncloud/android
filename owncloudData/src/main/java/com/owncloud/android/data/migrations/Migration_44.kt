/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
 */

package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME

val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.run {
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_max_characters` INTEGER")
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_min_characters` INTEGER")
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_min_digits` INTEGER")
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_min_lowercase_characters` INTEGER")
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_min_special_characters` INTEGER")
            execSQL("ALTER TABLE $CAPABILITIES_TABLE_NAME ADD COLUMN `password_policy_min_uppercase_characters` INTEGER")

        }
    }
}