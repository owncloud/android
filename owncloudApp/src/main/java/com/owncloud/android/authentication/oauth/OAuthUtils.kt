/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH
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
import com.owncloud.android.lib.common.http.HttpClient
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.connectivity.ok.OkConnectionBuilder
import net.openid.appauth.connectivity.ok.OkHttpConnectionImpl
import timber.log.Timber
import java.io.File

class OAuthUtils {
    companion object {
        fun buildOIDCAuthorizationServiceConfig(
            context: Context,
            serverBaseUrl: String,
            onGetAuthorizationServiceConfiguration: RetrieveConfigurationCallback
        ) {
            Timber.d("OIDC, getting the auth and token endpoints from the discovery document (well-known)")

            val urlPathAfterProtocolIndex = serverBaseUrl.indexOf(
                File.separator, serverBaseUrl.indexOf(File.separator) + 2
            )

            // OIDC Service Discovery Location is placed in urls like https://whatever and not https://whatever/others,
            // so remove those subpaths
            val urlToGetServiceDiscoveryLocation = if (urlPathAfterProtocolIndex != -1) {
                serverBaseUrl.substring(0, urlPathAfterProtocolIndex)
            } else {
                serverBaseUrl
            }

            val serviceDiscoveryLocation =
                Uri.parse(urlToGetServiceDiscoveryLocation).buildUpon()
                    .appendPath(AuthorizationServiceConfiguration.WELL_KNOWN_PATH)
                    .appendPath(AuthorizationServiceConfiguration.OPENID_CONFIGURATION_RESOURCE)
                    .build()

            AuthorizationServiceConfiguration.fetchFromUrl(
                serviceDiscoveryLocation,
                onGetAuthorizationServiceConfiguration,
                OkConnectionBuilder(HttpClient.getOkHttpClient())
            )
        }

        fun buildOAuthorizationServiceConfig(
            context: Context,
            serverBaseUrl: String = "",
            onGetAuthorizationServiceConfiguration: RetrieveConfigurationCallback
        ) {
            Timber.d("Trying normal OAuth instead")

            val authorizationServiceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse( // auth endpoint
                    "$serverBaseUrl${File.separator}${context.getString(R.string.oauth2_url_endpoint_auth)}"
                ),
                Uri.parse( // token endpoint
                    "$serverBaseUrl${File.separator}${context.getString(R.string.oauth2_url_endpoint_access)}"
                )
            )

            onGetAuthorizationServiceConfiguration.onFetchConfigurationCompleted(
                authorizationServiceConfiguration, null
            )
        }

        fun createClientSecretBasic(clientSecret: String) = ClientSecretBasic(clientSecret)
    }
}
