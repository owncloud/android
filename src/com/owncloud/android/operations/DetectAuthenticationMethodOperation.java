/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.operations;

import android.net.Uri;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;

import org.apache.commons.httpclient.HttpStatus;

import java.util.ArrayList;

/**
 * Operation to find out what authentication method requires
 * the server to access files.
 *
 * Basically, tries to access to the root folder without authorization
 * and analyzes the response.
 *
 * When successful, the instance of {@link RemoteOperationResult} passed
 * through {@link OnRemoteOperationListener#onRemoteOperationFinish(RemoteOperation,
 * RemoteOperationResult)} returns in {@link RemoteOperationResult#getData()}
 * a value of {@link AuthenticationMethod}.
 */
public class DetectAuthenticationMethodOperation extends RemoteOperation {

    private static final String TAG = DetectAuthenticationMethodOperation.class.getSimpleName();

    public enum AuthenticationMethod {
        UNKNOWN,
        NONE,
        BASIC_HTTP_AUTH,
        SAML_WEB_SSO,
        BEARER_TOKEN;

        public int getValue() {
            return ordinal();
        }

        public static AuthenticationMethod fromValue(int value) {
            if (value > -1 && value < values().length) {
                return values()[value];
            } else {
                return null;
            }
        }
    }

    /**
     *  Performs the operation.
     *
     *  Triggers a check of existence on the root folder of the server, granting
     *  that the request is not authenticated.
     *
     *  Analyzes the result of check to find out what authentication method, if
     *  any, is requested by the server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        ArrayList<AuthenticationMethod> allAvailableAuthMethods = new ArrayList<>();

        RemoteOperation operation = new ExistenceCheckRemoteOperation("", false);
        client.clearCredentials();
        client.setFollowRedirects(false);

        // try to access the root folder, following redirections but not SAML SSO redirections
        result = operation.execute(client);
        String redirectedLocation = result.getRedirectedLocation();
        while (redirectedLocation != null && redirectedLocation.length() > 0 &&
            !result.isIdPRedirection()) {
            client.setBaseUri(Uri.parse(result.getRedirectedLocation()));
            result = operation.execute(client);
            redirectedLocation = result.getRedirectedLocation();
        }

        // analyze response  
        if (result.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
            ArrayList<String> authHeaders = result.getAuthenticateHeaders();

            for (String authHeader: authHeaders) {

                if (authHeader.startsWith("basic")) {

                    allAvailableAuthMethods.add(AuthenticationMethod.BASIC_HTTP_AUTH);

                } else if (authHeader.startsWith("bearer")) {

                    allAvailableAuthMethods.add(AuthenticationMethod.BEARER_TOKEN);
                }
            }

        } else if (result.isSuccess()) {

            allAvailableAuthMethods.add(AuthenticationMethod.NONE);

        } else if (result.isIdPRedirection()) {

            allAvailableAuthMethods.add(AuthenticationMethod.SAML_WEB_SSO);
        }

        if (allAvailableAuthMethods.isEmpty()) {

            Log_OC.d(TAG, "Authentication method not found: ");

            allAvailableAuthMethods.add(AuthenticationMethod.UNKNOWN);

        } else {

            Log_OC.d(TAG, "Authentication methods found:");

            for (AuthenticationMethod authMetod: allAvailableAuthMethods) {

                Log_OC.d(TAG, " " + authenticationMethodToString(authMetod));
            }
        }

        if (allAvailableAuthMethods.indexOf(authenticationMethodToString(AuthenticationMethod.UNKNOWN)) == -1) {

            result = new RemoteOperationResult(result.getHttpCode(), result.getHttpPhrase(),null);
        }

        ArrayList<Object> data = new ArrayList<>();
        data.add(allAvailableAuthMethods);
        result.setData(data);
        return result;  // same result instance, so that other errors
        // can be handled by the caller transparently
    }

    private String authenticationMethodToString(AuthenticationMethod value) {
        switch (value) {
            case NONE:
                return "NONE";
            case BASIC_HTTP_AUTH:
                return "BASIC_HTTP_AUTH";
            case BEARER_TOKEN:
                return "BEARER_TOKEN";
            case SAML_WEB_SSO:
                return "SAML_WEB_SSO";
            default:
                return "UNKNOWN";
        }
    }
}