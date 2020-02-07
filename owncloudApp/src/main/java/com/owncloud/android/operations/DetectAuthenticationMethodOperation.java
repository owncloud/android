/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
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
import com.owncloud.android.lib.resources.server.CheckPathExistenceRemoteOperation;
import timber.log.Timber;

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
@Deprecated
// TODO: Remove this operation. Get AuthenticationMethods from GetServerInfoUseCase
public class DetectAuthenticationMethodOperation extends RemoteOperation<AuthenticationMethod> {

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
    protected RemoteOperationResult<AuthenticationMethod> run(OwnCloudClient client) {
        AuthenticationMethod authenticationMethod = null;

        CheckPathExistenceRemoteOperation operation = new CheckPathExistenceRemoteOperation("",
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
            String authenticateHeaders = resultFromExistenceCheck.getAuthenticateHeaders();
            if (authenticateHeaders.contains("basic")) {
                authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH;
            } else if (authenticateHeaders.contains("bearer")) {
                authenticationMethod = AuthenticationMethod.BEARER_TOKEN;
            }
        } else if (resultFromExistenceCheck.isSuccess()) {
            authenticationMethod = AuthenticationMethod.NONE;
        }

        // Step 3: prepare result with available authentication methods
        RemoteOperationResult<AuthenticationMethod> result = new RemoteOperationResult<>(resultFromExistenceCheck);

        if (authenticationMethod == null) {
            Timber.d("Authentication method not found: ");
        } else {
            Timber.d("Authentication method:%s", authenticationMethod);

            result = new RemoteOperationResult<>(result.getHttpCode(), result.getHttpPhrase(), null);
            result.setSuccess(true);
        }

        result.setData(authenticationMethod);

        return result;  // same result instance, so that other errors
        // can be handled by the caller transparently
    }
}
