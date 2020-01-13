/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

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
public class DetectAuthenticationMethodOperation extends RemoteOperation<List<AuthenticationMethod>> {

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
    protected RemoteOperationResult<List<AuthenticationMethod>> run(OwnCloudClient client) {
        ArrayList<AuthenticationMethod> allAvailableAuthMethods = new ArrayList<>();

        ExistenceCheckRemoteOperation operation = new ExistenceCheckRemoteOperation("", false,
                false);
        client.clearCredentials();

        client.setFollowRedirects(false);

        // Step 1: check whether the root folder exists, following redirections
        RemoteOperationResult resultFromExistenceCheck = operation.execute(client);
        String redirectedLocation = resultFromExistenceCheck.getRedirectedLocation();
        while (redirectedLocation != null && redirectedLocation.length() > 0) {
            client.setBaseUri(Uri.parse(resultFromExistenceCheck.getRedirectedLocation()));
            resultFromExistenceCheck = operation.execute(client);
            redirectedLocation = resultFromExistenceCheck.getRedirectedLocation();
        }

        // Step 2: look for authentication methods
        if (resultFromExistenceCheck.getHttpCode() == HttpConstants.HTTP_UNAUTHORIZED) {
            ArrayList<String> authHeaders = resultFromExistenceCheck.getAuthenticateHeaders();
            for (String authHeader : authHeaders) {
                if (authHeader.startsWith("basic")) {
                    allAvailableAuthMethods.add(AuthenticationMethod.BASIC_HTTP_AUTH);
                } else if (authHeader.startsWith("bearer")) {
                    allAvailableAuthMethods.add(AuthenticationMethod.BEARER_TOKEN);
                }
            }
        } else if (resultFromExistenceCheck.isSuccess()) {
            allAvailableAuthMethods.add(AuthenticationMethod.NONE);
        }

        // Step 3: prepare result with available authentication methods
        RemoteOperationResult<List<AuthenticationMethod>> result =
                new RemoteOperationResult<>(resultFromExistenceCheck);

        if (allAvailableAuthMethods.isEmpty()) {
            Timber.d("Authentication method not found: ");
        } else {
            Timber.d("Authentication methods found:");
            for (AuthenticationMethod authMetod : allAvailableAuthMethods) {
                Timber.d(authenticationMethodToString(authMetod));
            }

            result = new RemoteOperationResult<>(result.getHttpCode(), result.getHttpPhrase(), null);
            result.setSuccess(true);
        }

        result.setData(allAvailableAuthMethods);

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
            default:
                return "UNKNOWN";
        }
    }
}
