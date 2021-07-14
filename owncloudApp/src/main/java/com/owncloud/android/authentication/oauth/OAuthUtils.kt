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
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.authentication.QUERY_PARAMETER_CLIENT_ID
import com.owncloud.android.data.authentication.QUERY_PARAMETER_CODE_CHALLENGE
import com.owncloud.android.data.authentication.QUERY_PARAMETER_CODE_CHALLENGE_METHOD
import com.owncloud.android.data.authentication.QUERY_PARAMETER_REDIRECT_URI
import com.owncloud.android.data.authentication.QUERY_PARAMETER_RESPONSE_TYPE
import com.owncloud.android.data.authentication.QUERY_PARAMETER_SCOPE
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationRequest
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthUtils {

    fun generateRandomCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(DEFAULT_CODE_VERIFIER_ENTROPY)
        secureRandom.nextBytes(randomBytes)
        val encoding = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
        return Base64.encodeToString(randomBytes, encoding)
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray()
        val messageDigest = MessageDigest.getInstance(ALGORITHM_SHA_256)
        messageDigest.update(bytes)
        val digest = messageDigest.digest()
        val encoding = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
        return Base64.encodeToString(digest, encoding)
    }

    companion object {
        private const val ALGORITHM_SHA_256 = "SHA-256"
        private const val CODE_CHALLENGE_METHOD = "S256"
        private const val DEFAULT_CODE_VERIFIER_ENTROPY = 64

        fun buildClientRegistrationRequest(
            registrationEndpoint: String,
            context: Context
        ): ClientRegistrationRequest =
            ClientRegistrationRequest(
                registrationEndpoint = registrationEndpoint,
                clientName = MainApp.userAgent,
                redirectUris = listOf(buildRedirectUri(context).toString())
            )

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
            scope: String,
            codeChallenge: String,
        ): Uri =
            authorizationEndpoint.buildUpon()
                .appendQueryParameter(QUERY_PARAMETER_REDIRECT_URI, redirectUri)
                .appendQueryParameter(QUERY_PARAMETER_CLIENT_ID, clientId)
                .appendQueryParameter(QUERY_PARAMETER_RESPONSE_TYPE, responseType)
                .appendQueryParameter(QUERY_PARAMETER_SCOPE, scope)
                .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE, codeChallenge)
                .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD, CODE_CHALLENGE_METHOD)
                .build()

        fun buildRedirectUri(context: Context): Uri =
            Uri.Builder()
                .scheme(context.getString(R.string.oauth2_redirect_uri_scheme))
                .authority(context.getString(R.string.oauth2_redirect_uri_path))
                .build()
    }
}
