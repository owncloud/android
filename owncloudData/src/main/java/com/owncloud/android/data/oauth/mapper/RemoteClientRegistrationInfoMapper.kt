/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.oauth.responses.ClientRegistrationResponse

class RemoteClientRegistrationInfoMapper : RemoteMapper<ClientRegistrationInfo, ClientRegistrationResponse> {
    override fun toModel(remote: ClientRegistrationResponse?): ClientRegistrationInfo? {
        remote ?: return null

        return ClientRegistrationInfo(
            clientId = remote.clientId,
            clientSecret = remote.clientSecret,
            clientIdIssuedAt = remote.clientIdIssuedAt,
            clientSecretExpiration = remote.clientSecretExpiration
        )
    }

    // Not needed
    override fun toRemote(model: ClientRegistrationInfo?): ClientRegistrationResponse? = null

}
