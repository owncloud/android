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
import com.owncloud.android.data.authentication.QUERY_PARAMETER_CLIENT_ID
import com.owncloud.android.data.authentication.QUERY_PARAMETER_REDIRECT_URI
import com.owncloud.android.data.authentication.QUERY_PARAMETER_RESPONSE_TYPE
import com.owncloud.android.data.authentication.QUERY_PARAMETER_SCOPE
import java.net.URLEncoder

class OAuthUtils {
    companion object {

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

        fun buildAuthorizationRequest(
            authorizationEndpoint: Uri,
            redirectUri: String,
            clientId: String,
            responseType: String,
            scope: String
        ): Uri =
            authorizationEndpoint.buildUpon()
                .appendQueryParameter(QUERY_PARAMETER_REDIRECT_URI, redirectUri)
                .appendQueryParameter(QUERY_PARAMETER_CLIENT_ID, clientId)
                .appendQueryParameter(QUERY_PARAMETER_RESPONSE_TYPE, responseType)
                .appendQueryParameter(QUERY_PARAMETER_SCOPE, scope)
                .build()

        fun buildRedirectUri(context: Context): Uri =
            Uri.Builder()
                .scheme(context.getString(R.string.oauth2_redirect_uri_scheme))
                .authority(context.getString(R.string.oauth2_redirect_uri_path))
                .build()
    }
}
