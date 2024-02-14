/**
 * ownCloud Android client application
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
package com.owncloud.android.data.webfinger.repository

import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.domain.webfinger.WebFingerRepository
import com.owncloud.android.domain.webfinger.model.WebFingerRel

class OCWebFingerRepository(
    private val remoteWebFingerDatasource: RemoteWebFingerDataSource,
) : WebFingerRepository {

    override fun getInstancesFromWebFinger(
        server: String,
        rel: WebFingerRel,
        resource: String
    ): List<String> =
        remoteWebFingerDatasource.getInstancesFromWebFinger(
            lookupServer = server,
            rel = rel,
            resource = resource
        )

    override fun getInstancesFromAuthenticatedWebFinger(
        server: String,
        rel: WebFingerRel,
        resource: String,
        username: String,
        accessToken: String,
    ): List<String> =
        remoteWebFingerDatasource.getInstancesFromAuthenticatedWebFinger(
            lookupServer = server,
            rel = rel,
            resource = resource,
            username = username,
            accessToken = accessToken,
        )
}
