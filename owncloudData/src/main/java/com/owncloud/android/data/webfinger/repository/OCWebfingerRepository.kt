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

import com.owncloud.android.data.webfinger.datasources.WebfingerRemoteDatasource
import com.owncloud.android.domain.webfinger.WebfingerRepository
import com.owncloud.android.domain.webfinger.model.WebfingerRel

class OCWebfingerRepository(
    private val webfingerRemoteDatasource: WebfingerRemoteDatasource,
) : WebfingerRepository {

    override fun getInstancesFromWebFinger(
        server: String,
        rel: WebfingerRel,
        resource: String
    ): List<String> =
        webfingerRemoteDatasource.getInstancesFromWebFinger(
            lookupServer = server,
            rel = rel,
            username = resource
        )

    override fun getInstancesFromAuthenticatedWebFinger(
        server: String,
        rel: WebfingerRel,
        username: String,
        accessToken: String,
    ): List<String> =
        webfingerRemoteDatasource.getInstancesFromAuthenticatedWebfinger(
            lookupServer = server,
            rel = rel,
            username = username,
            accessToken = accessToken,
        )
}
