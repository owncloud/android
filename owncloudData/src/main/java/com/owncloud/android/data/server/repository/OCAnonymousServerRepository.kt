/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.data.server.repository

import com.owncloud.android.data.server.datasources.RemoteAnonymousDatasource
import com.owncloud.android.domain.server.AnonymousServerRepository
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.lib.common.accounts.AccountUtils
import java.util.Locale

class OCAnonymousServerRepository(
    private val remoteAnonymousDatasource: RemoteAnonymousDatasource
) : AnonymousServerRepository {

    override fun getServerInfo(path: String): ServerInfo {
        // First step: check the status of the server (including its version)
        val pairRemoteStatus = remoteAnonymousDatasource.getRemoteStatus(path)

        // Second step: get authentication method required by the server
        val authenticationMethod = remoteAnonymousDatasource.getAuthenticationMethod(
            normalizeProtocolPrefix(
                trimWebdavSuffix(path),
                pairRemoteStatus.second
            )
        )

        return ServerInfo(
            ownCloudVersion = pairRemoteStatus.first.version,
            baseUrl = normalizeProtocolPrefix(trimWebdavSuffix(path), pairRemoteStatus.second),
            authenticationMethod = authenticationMethod,
            isSecureConnection = pairRemoteStatus.second
        )
    }

    private fun trimWebdavSuffix(url: String): String {
        var newUrl = url
        if (newUrl.isNotBlank()) {
            if (url.endsWith("/")) {
                newUrl = url.substring(0, url.length - 1)
            }
            if (url.toLowerCase(Locale.getDefault()).endsWith(AccountUtils.Constants.WEBDAV_PATH_4_0_AND_LATER)) {
                newUrl = url.substring(0, url.length - AccountUtils.Constants.WEBDAV_PATH_4_0_AND_LATER.length)
            }
        }
        return newUrl
    }

    private fun normalizeProtocolPrefix(url: String, isSslConnection: Boolean): String {
        if (!url.toLowerCase(Locale.getDefault()).startsWith("http://") &&
            !url.toLowerCase(Locale.getDefault()).startsWith("https://")
        ) {
            return if (isSslConnection) {
                "https://$url"
            } else {
                "http://$url"
            }
        }
        return url
    }
}
