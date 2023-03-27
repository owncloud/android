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
package com.owncloud.android.domain.appregistry.model

data class AppRegistry(
    val accountName: String,
    val mimetypes: List<AppRegistryMimeType>
)

data class AppRegistryMimeType(
    val mimeType: String,
    val ext: String? = null,
    val appProviders: List<AppRegistryProvider>,
    val name: String? = null,
    val icon: String? = null,
    val description: String? = null,
    val allowCreation: Boolean? = null,
    val defaultApplication: String? = null
)

data class AppRegistryProvider(
    val name: String,
    val icon: String,
)
