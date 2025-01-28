/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.domain.server.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.NOT_HTTP_ALLOWED_MESSAGE
import com.owncloud.android.domain.exceptions.SSLErrorCode
import com.owncloud.android.domain.exceptions.SSLErrorException
import com.owncloud.android.domain.server.ServerInfoRepository
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.model.ServerInfo.Companion.HTTPS_PREFIX
import com.owncloud.android.domain.server.model.ServerInfo.Companion.HTTP_PREFIX
import java.util.Locale

class GetServerInfoAsyncUseCase(
    private val serverInfoRepository: ServerInfoRepository,
) : BaseUseCaseWithResult<ServerInfo, GetServerInfoAsyncUseCase.Params>() {
    override fun run(params: Params): ServerInfo {
        val normalizedServerUrl = normalizeProtocolPrefix(params.serverPath).trimEnd(TRAILING_SLASH)
        val serverInfo = serverInfoRepository.getServerInfo(normalizedServerUrl, params.creatingAccount, params.enforceOIDC)
        if (!serverInfo.isSecureConnection && params.secureConnectionEnforced) {
            throw SSLErrorException(NOT_HTTP_ALLOWED_MESSAGE, SSLErrorCode.NOT_HTTP_ALLOWED)
        }
        return serverInfo
    }

    data class Params(
        val serverPath: String,
        val creatingAccount: Boolean,
        val enforceOIDC: Boolean,
        val secureConnectionEnforced: Boolean,
    )

    /**
     * In case the user introduces a server url without prefix, we will try to connect to https
     */
    private fun normalizeProtocolPrefix(url: String): String =
        if (!url.lowercase(Locale.getDefault()).startsWith(HTTP_PREFIX) &&
            !url.lowercase(Locale.getDefault()).startsWith(HTTPS_PREFIX)
        ) {
            "$HTTPS_PREFIX$url"
        } else {
            url
        }

    companion object {
        const val TRAILING_SLASH = '/'
    }
}
