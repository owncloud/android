/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.data.roles.repository

import com.owncloud.android.data.roles.datasources.RemoteRolesDataSource
import com.owncloud.android.domain.roles.RolesRepository
import com.owncloud.android.domain.roles.model.OCRole

class OCRolesRepository(
    private val remoteRolesDataSource: RemoteRolesDataSource
): RolesRepository {

    override fun getRoles(accountName: String): List<OCRole> =
        remoteRolesDataSource.getRoles(accountName)

}
