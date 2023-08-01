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

package com.owncloud.android.data.appregistry.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.appregistry.datasources.RemoteAppRegistryDataSource
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.domain.appregistry.model.AppRegistryProvider
import com.owncloud.android.lib.resources.appregistry.responses.AppRegistryResponse

class OCRemoteAppRegistryDataSource(
    private val clientManager: ClientManager
) : RemoteAppRegistryDataSource {
    override fun getAppRegistryForAccount(accountName: String, appUrl: String?): AppRegistry =
        executeRemoteOperation {
            clientManager.getAppRegistryService(accountName).getAppRegistry(appUrl)
        }.toModel(accountName)

    override fun getUrlToOpenInWeb(
        accountName: String,
        openWebEndpoint: String,
        fileId: String,
        appName: String,
    ): String =
        executeRemoteOperation {
            clientManager.getAppRegistryService(accountName).getUrlToOpenInWeb(
                openWebEndpoint = openWebEndpoint,
                fileId = fileId,
                appName = appName,
            )
        }

    override fun createFileWithAppProvider(
        accountName: String,
        createFileWithAppProviderEndpoint: String,
        parentContainerId: String,
        filename: String,
    ): String =
        executeRemoteOperation {
            clientManager.getAppRegistryService(accountName).createFileWithAppProvider(
                createFileWithAppProviderEndpoint = createFileWithAppProviderEndpoint,
                parentContainerId = parentContainerId,
                filename = filename,
            )
        }

    private fun AppRegistryResponse.toModel(accountName: String) =
        AppRegistry(
            accountName = accountName,
            mimetypes = value.map { appRegistryMimeTypeResponse ->
                AppRegistryMimeType(
                    mimeType = appRegistryMimeTypeResponse.mimeType,
                    ext = appRegistryMimeTypeResponse.ext,
                    appProviders = appRegistryMimeTypeResponse.appProviders.map { appRegistryProviderResponse ->
                        AppRegistryProvider(
                            name = appRegistryProviderResponse.name,
                            icon = appRegistryProviderResponse.icon
                        )
                    },
                    name = appRegistryMimeTypeResponse.name,
                    icon = appRegistryMimeTypeResponse.icon,
                    description = appRegistryMimeTypeResponse.description,
                    allowCreation = appRegistryMimeTypeResponse.allowCreation,
                    defaultApplication = appRegistryMimeTypeResponse.defaultApplication,
                )
            }
        )
}
