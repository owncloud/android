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
import android.util.Base64
import com.owncloud.android.R
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import timber.log.Timber
import java.io.File
import java.net.URLEncoder

class OAuthUtils {
    companion object {

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

        fun getClientAuth(
            clientSecret: String,
            clientId: String
        ): String {
            // From the OAuth2 RFC, client ID and secret should be encoded prior to concatenation and
            // conversion to Base64: https://tools.ietf.org/html/rfc6749#section-2.3.1
            val encodedClientId = URLEncoder.encode(clientId, "utf-8")
            val encodedClientSecret = URLEncoder.encode(clientSecret, "utf-8")
            val credentials = "$encodedClientId:$encodedClientSecret"
            return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        }
    }
}
