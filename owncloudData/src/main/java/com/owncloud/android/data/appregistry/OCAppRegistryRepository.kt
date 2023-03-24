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
package com.owncloud.android.data.appregistry

import com.owncloud.android.data.appregistry.datasources.LocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.datasources.RemoteAppRegistryDataSource
import com.owncloud.android.domain.appregistry.AppRegistryRepository

class OCAppRegistryRepository(
    private val localAppRegistryDataSource: LocalAppRegistryDataSource,
    private val remoteAppRegistryDataSource: RemoteAppRegistryDataSource,
) : AppRegistryRepository {
    override fun refreshAppRegistryForAccount(accountName: String) {
        remoteAppRegistryDataSource.getAppRegistryForAccount(accountName).also {
            localAppRegistryDataSource.saveAppRegistryForAccount(it)
        }
    }

    override fun getUrlToOpenInWeb(accountName: String, openWebEndpoint: String, fileId: String): String =
        remoteAppRegistryDataSource.getUrlToOpenInWeb(accountName = accountName, openWebEndpoint = openWebEndpoint, fileId = fileId)
}
