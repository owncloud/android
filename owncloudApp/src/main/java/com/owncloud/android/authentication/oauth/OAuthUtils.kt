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
import com.owncloud.android.lib.common.authentication.oauth.OAuthConnectionBuilder
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.ResponseTypeValues
import timber.log.Timber

class OAuthUtils {
    companion object {
        fun createAuthorizationService(context: Context): AuthorizationService {
            val appAuthConfigurationBuilder = AppAuthConfiguration.Builder()
            appAuthConfigurationBuilder.setConnectionBuilder(OAuthConnectionBuilder(context))
            return AuthorizationService(context, appAuthConfigurationBuilder.build())
        }

        fun buildOIDCAuthorizationServiceConfig(
            context: Context,
            onGetAuthorizationServiceConfiguration: RetrieveConfigurationCallback
        ) {
            Timber.d("OIDC, getting the auth and token endpoints from the discovery document (well-known)")

            val serviceDiscoveryLocation =
                Uri.parse(context.getString(R.string.oidc_service_discovery_location)).buildUpon()
                    .appendPath(AuthorizationServiceConfiguration.WELL_KNOWN_PATH)
                    .appendPath(AuthorizationServiceConfiguration.OPENID_CONFIGURATION_RESOURCE)
                    .build()

            AuthorizationServiceConfiguration.fetchFromUrl(
                serviceDiscoveryLocation,
                onGetAuthorizationServiceConfiguration,
                OAuthConnectionBuilder(context)
            )
        }

        fun buildOAuthorizationServiceConfig(
            context: Context,
            onGetAuthorizationServiceConfiguration: RetrieveConfigurationCallback,
            baseUrl: String = ""
        ) {
            Timber.d("Trying normal OAuth instead")

            val authorizationServiceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse(
                    "$baseUrl/${context.getString(R.string.oauth2_url_endpoint_auth)}" // auth endpoint
                ),
                Uri.parse(
                    "$baseUrl/${context.getString(R.string.oauth2_url_endpoint_access)}" // token endpoint
                )
            )

            onGetAuthorizationServiceConfiguration.onFetchConfigurationCompleted(
                authorizationServiceConfiguration, null
            )
        }

        fun createAuthorizationRequest(
            authEndPoint: String,
            tokenEndPoint: String,
            clientId: String,
            redirectUri: String
        ): AuthorizationRequest {
            val serviceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse(authEndPoint),  // auth endpoint
                Uri.parse(tokenEndPoint)  // token endpoint
            )

            val builder = AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                ResponseTypeValues.CODE,
                Uri.parse(redirectUri)
            )

            return builder.build()
        }

        fun createClientSecretBasic(clientSecret: String) = ClientSecretBasic(clientSecret)
    }
}
