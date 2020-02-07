/**
 * ownCloud Android client application
 *
 * @author masensio on 09/02/2015.
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
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.server.CheckPathExistenceRemoteOperation;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.common.UseCaseHelper;
import timber.log.Timber;

import java.lang.ref.WeakReference;

/**
 * Async Task to verify the credentials of a user
 */
@Deprecated
//TODO: Move logic to Viewmodel
public class AuthenticatorAsyncTask extends AsyncTask<Object, Void, RemoteOperationResult> {

    private Context mContext;
    private UseCaseHelper mUseCaseHelper;
    private final WeakReference<OnAuthenticatorTaskListener> mListener;

    AuthenticatorAsyncTask(Activity activity) {
        mContext = activity.getApplicationContext();
        mListener = new WeakReference<>((OnAuthenticatorTaskListener) activity);
        mUseCaseHelper = new UseCaseHelper();
    }

    @Override
    protected RemoteOperationResult doInBackground(Object... params) {

        OwnCloudCredentials credentials = null;
        if (params != null && params.length == 3) {
            String username = (String) params[1];
            String password = (String) params[2];
            Timber.d("Server info : " + mUseCaseHelper.getServerInfo((String)params[0]));
            /// validate credentials accessing the root folder
            credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    username,
                    password
            );
        }

            RemoteOperationResult result;
        if (credentials!= null || (params != null && params.length == 2)) {
            String url = (String) params[0];
             if(params.length == 2) {
                 credentials = (OwnCloudCredentials) params[1];
             }

            // Client
            Uri uri = Uri.parse(url);
            OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(uri, mContext, true);
            client.setCredentials(credentials);

            // Operation - try credentials
            CheckPathExistenceRemoteOperation checkPathExistenceRemoteOperation = new CheckPathExistenceRemoteOperation(
                    OCFile.ROOT_PATH,
                    true
            );
            result = checkPathExistenceRemoteOperation.execute(client);

            String targetUrlAfterPermanentRedirection = null;
            if (checkPathExistenceRemoteOperation.wasRedirected()) {
                RedirectionPath redirectionPath = checkPathExistenceRemoteOperation.getRedirectionPath();
                targetUrlAfterPermanentRedirection = redirectionPath.getLastPermanentLocation();
            }

            // Operation - get display name
            if (result.isSuccess()) {
                GetUserInfoRemoteOperation remoteUserNameOperation = new GetUserInfoRemoteOperation();
                if (targetUrlAfterPermanentRedirection != null) {
                    // we can't assume that any subpath of the domain is correctly redirected; ugly stuff
                    client = OwnCloudClientFactory.createOwnCloudClient(
                            Uri.parse(AccountUtils.trimWebdavSuffix(
                                    targetUrlAfterPermanentRedirection
                            )),
                            mContext,
                            true
                    );
                    client.setCredentials(credentials);
                }
                result = remoteUserNameOperation.execute(client);
            }

            // let the caller knows what is real URL that should be accessed for the account
            // being authenticated if the initial URL is being redirected permanently (HTTP code 301)
            result.setLastPermanentLocation(targetUrlAfterPermanentRedirection);

        } else {
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
        }

        return result;
    }

    @Override
    protected void onPostExecute(RemoteOperationResult result) {

        if (result != null) {
            OnAuthenticatorTaskListener listener = mListener.get();
            if (listener != null) {
                listener.onAuthenticatorTaskCallback(result);
            }
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnAuthenticatorTaskListener {

        void onAuthenticatorTaskCallback(RemoteOperationResult result);
    }
}
