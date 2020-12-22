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

package com.owncloud.android.data.oauth.mapper

import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.oauth.responses.TokenResponse as RemoteTokenResponse

class RemoteTokenResponseMapper : RemoteMapper<TokenResponse, RemoteTokenResponse> {
    override fun toModel(remote: RemoteTokenResponse?): TokenResponse? {
        remote ?: return null

        return TokenResponse(
            accessToken = remote.accessToken,
            expiresIn = remote.expiresIn,
            refreshToken = remote.refreshToken,
            tokenType = remote.tokenType,
            userId = remote.userId,
            scope = remote.scope,
            additionalParameters = remote.additionalParameters
        )
    }

    // Not needed
    override fun toRemote(model: TokenResponse?): RemoteTokenResponse? = null

}
