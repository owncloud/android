/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.appregistry.datasources.implementation

import com.owncloud.android.data.appregistry.datasources.LocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.db.AppRegistryDao
import com.owncloud.android.data.appregistry.db.AppRegistryEntity
import com.owncloud.android.domain.appregistry.AppRegistry
import com.owncloud.android.domain.appregistry.AppRegistryMimeType
import com.owncloud.android.domain.appregistry.AppRegistryProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

class OCLocalAppRegistryDataSource(
    private val appRegistryDao: AppRegistryDao,
) : LocalAppRegistryDataSource {
    override fun getAppRegistryForAccountAsStream(accountName: String): Flow<AppRegistry?> =
        appRegistryDao.getAppRegistryForAccount(accountName).map {
            it.toModel()
        }

    override fun saveAppRegistryForAccount(appRegistry: AppRegistry) {
        val appRegistryEntitiesToInsert = mutableListOf<AppRegistryEntity>()

        val newAppRegistryEntities = appRegistry.toEntities(appRegistry.accountName)
        newAppRegistryEntities.forEach { appRegistryEntity ->
            appRegistryEntitiesToInsert.add(appRegistryEntity)
        }

        appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName)
        appRegistryDao.upsertAppRegistries(appRegistryEntitiesToInsert)
    }

    override fun deleteAppRegistryForAccount(accountName: String) {
        appRegistryDao.deleteAppRegistryForAccount(accountName)
    }

    private fun AppRegistry.toEntities(accountName: String): List<AppRegistryEntity> {
        return mimetypes.map { appRegistryMimeTypes ->
            AppRegistryEntity(
                accountName = accountName,
                mimeType = appRegistryMimeTypes.mimeType,
                ext = appRegistryMimeTypes.ext,
                appProviders = appRegistryMimeTypes.appProviders.toJsonString(),
                name = appRegistryMimeTypes.name,
                icon = appRegistryMimeTypes.icon,
                description = appRegistryMimeTypes.description,
                allowCreation = appRegistryMimeTypes.allowCreation,
                defaultApplication = appRegistryMimeTypes.defaultApplication,
            )
        }
    }

    private fun List<AppRegistryEntity>.toModel(): AppRegistry? {
        if (this.isEmpty()) return null

        return AppRegistry(
            accountName = this.first().accountName,
            mimetypes = map { mimeTypeEntity ->
                AppRegistryMimeType(
                    mimeType = mimeTypeEntity.mimeType,
                    ext = mimeTypeEntity.ext,
                    appProviders = mimeTypeEntity.appProviders.toAppRegistryProvider(),
                    name = mimeTypeEntity.name,
                    icon = mimeTypeEntity.icon,
                    description = mimeTypeEntity.description,
                    allowCreation = mimeTypeEntity.allowCreation,
                    defaultApplication = mimeTypeEntity.defaultApplication,
                )
            }
        )
    }

    private fun List<AppRegistryProvider>.toJsonString(): String {
        val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type: Type = Types.newParameterizedType(List::class.java, AppRegistryProvider::class.java)

        val jsonAdapter: JsonAdapter<List<AppRegistryProvider>> = moshi.adapter(type)

        return jsonAdapter.toJson(this)
    }

    private fun String.toAppRegistryProvider(): List<AppRegistryProvider> {
        val moshi: Moshi = Moshi.Builder().build()
        val type: Type = Types.newParameterizedType(List::class.java, AppRegistryProvider::class.java)

        val jsonAdapter: JsonAdapter<List<AppRegistryProvider>> = moshi.adapter(type)

        return jsonAdapter.fromJson(this) ?: emptyList()
    }
}
