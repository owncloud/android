/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.data.appregistry.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.data.appregistry.db.AppRegistryEntity.Companion.APP_REGISTRY_ACCOUNT_NAME
import com.owncloud.android.data.appregistry.db.AppRegistryEntity.Companion.APP_REGISTRY_ALLOW_CREATION
import com.owncloud.android.data.appregistry.db.AppRegistryEntity.Companion.APP_REGISTRY_MIME_TYPE
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRegistryDao {
    @Upsert
    fun upsertAppRegistries(appRegistryEntities: List<AppRegistryEntity>)

    @Query(SELECT_APP_REGISTRY_FOR_ACCOUNT_AND_MIME_TYPE)
    fun getAppRegistryForMimeType(
        accountName: String,
        mimeType: String,
    ): Flow<AppRegistryEntity?>

    @Query(SELECT_APP_REGISTRY_ALLOW_CREATION_FOR_ACCOUNT)
    fun getAppRegistryWhichAllowCreation(
        accountName: String,
    ): Flow<List<AppRegistryEntity>>

    @Query(DELETE_APP_REGISTRY_FOR_ACCOUNT)
    fun deleteAppRegistryForAccount(accountName: String)

    companion object {
        private const val SELECT_APP_REGISTRY_FOR_ACCOUNT_AND_MIME_TYPE = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.APP_REGISTRY_TABLE_NAME}
            WHERE $APP_REGISTRY_ACCOUNT_NAME = :accountName AND $APP_REGISTRY_MIME_TYPE = :mimeType
        """

        private const val SELECT_APP_REGISTRY_ALLOW_CREATION_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.APP_REGISTRY_TABLE_NAME}
            WHERE $APP_REGISTRY_ACCOUNT_NAME = :accountName AND $APP_REGISTRY_ALLOW_CREATION = 1
        """

        private const val DELETE_APP_REGISTRY_FOR_ACCOUNT = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.APP_REGISTRY_TABLE_NAME}
            WHERE $APP_REGISTRY_ACCOUNT_NAME = :accountName
        """
    }
}
