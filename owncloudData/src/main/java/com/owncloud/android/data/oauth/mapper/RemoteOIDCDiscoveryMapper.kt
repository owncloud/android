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

import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse

class RemoteOIDCDiscoveryMapper : RemoteMapper<OIDCServerConfiguration, OIDCDiscoveryResponse> {

    override fun toModel(remote: OIDCDiscoveryResponse?): OIDCServerConfiguration? {
        remote ?: return null

        return OIDCServerConfiguration(
            authorization_endpoint = remote.authorization_endpoint,
            check_session_iframe = remote.check_session_iframe,
            end_session_endpoint = remote.end_session_endpoint,
            issuer = remote.issuer,
            registration_endpoint = remote.registration_endpoint,
            response_types_supported = remote.response_types_supported,
            scopes_supported = remote.scopes_supported,
            token_endpoint = remote.token_endpoint,
            token_endpoint_auth_methods_supported = remote.token_endpoint_auth_methods_supported,
            userinfo_endpoint = remote.userinfo_endpoint
        )
    }

    // Not needed
    override fun toRemote(model: OIDCServerConfiguration?): OIDCDiscoveryResponse? = null

}
