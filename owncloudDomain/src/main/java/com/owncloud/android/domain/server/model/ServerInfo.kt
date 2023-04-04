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

package com.owncloud.android.domain.server.model

import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration

sealed class ServerInfo(
    val ownCloudVersion: String,
    var baseUrl: String,
) {
    val isSecureConnection get() = baseUrl.startsWith(HTTPS_PREFIX, ignoreCase = true)

    override fun equals(other: Any?): Boolean {
        if (other !is ServerInfo) return false
        if (ownCloudVersion != other.ownCloudVersion) return false
        if (baseUrl != other.baseUrl) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    class OIDCServer(
        ownCloudVersion: String,
        baseUrl: String,
        val oidcServerConfiguration: OIDCServerConfiguration,
    ) : ServerInfo(ownCloudVersion = ownCloudVersion, baseUrl = baseUrl) {
        override fun equals(other: Any?): Boolean {
            if (other !is OIDCServer) return false
            if (oidcServerConfiguration != other.oidcServerConfiguration) return false
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    class OAuth2Server(
        ownCloudVersion: String,
        baseUrl: String,
    ) : ServerInfo(ownCloudVersion = ownCloudVersion, baseUrl = baseUrl)

    class BasicServer(
        ownCloudVersion: String,
        baseUrl: String,
    ) : ServerInfo(ownCloudVersion = ownCloudVersion, baseUrl = baseUrl)

    companion object {
        const val HTTP_PREFIX = "http://"
        const val HTTPS_PREFIX = "https://"
    }
}
