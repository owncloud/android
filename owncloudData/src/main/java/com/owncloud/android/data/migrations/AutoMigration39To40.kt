/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAJOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME

@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "version_mayor",
    toColumnName = CAPABILITIES_VERSION_MAJOR
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "enabled",
    toColumnName = "enabledAppProviders"
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "version",
    toColumnName = "versionAppProviders"
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "appsUrl",
    toColumnName = "appsUrlAppProviders"
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "openUrl",
    toColumnName = "openUrlAppProviders"
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "openWebUrl",
    toColumnName = "openWebUrlAppProviders"
)
@RenameColumn(
    tableName = CAPABILITIES_TABLE_NAME,
    fromColumnName = "newUrl",
    toColumnName = "newUrlAppProviders"
)
@DeleteColumn(
    tableName = FILES_TABLE_NAME,
    columnName = "lastSyncDateForProperties"
)
class AutoMigration39To40 : AutoMigrationSpec
