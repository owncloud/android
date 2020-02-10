/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.authentication.oauth

import android.content.Context
import android.net.Uri
import com.owncloud.android.R
import com.owncloud.android.lib.common.authentication.oauth.OAuthConnectionBuilder
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import timber.log.Timber

class OAuthServiceConfiguration {
    companion object {
        fun buildAuthorizationServiceConfiguration(
            context: Context,
            onGetAuthorizationServiceConfiguration: RetrieveConfigurationCallback
        ) {
            val serviceDiscoveryLocation =
                Uri.parse(context.getString(R.string.oauth2_service_discovery_location)).buildUpon()
                    .appendPath(AuthorizationServiceConfiguration.WELL_KNOWN_PATH)
                    .appendPath(AuthorizationServiceConfiguration.OPENID_CONFIGURATION_RESOURCE)
                    .build()

            if (context.resources.getBoolean(R.bool.use_oauth2_service_discovery_location)) {
                Timber.d("Let's get the auth and token endpoints from the discovery document (well-known)")
                AuthorizationServiceConfiguration.fetchFromUrl(
                    serviceDiscoveryLocation,
                    onGetAuthorizationServiceConfiguration,
                    OAuthConnectionBuilder(context)
                )
            } else {
                val authorizationServiceConfiguration = AuthorizationServiceConfiguration(
                    Uri.parse(context.getString(R.string.oauth2_url_endpoint_auth)),  // auth endpoint
                    Uri.parse(context.getString(R.string.oauth2_url_endpoint_access)) // token endpoint
                )

                onGetAuthorizationServiceConfiguration.onFetchConfigurationCompleted(
                    authorizationServiceConfiguration, null
                )
            }
        }
    }
}
