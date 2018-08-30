/**
 *   ownCloud Android client application
 *
 *   @author masensio on 09/02/2015.
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

import java.lang.ref.WeakReference;


/**
 * Async Task to verify the credentials of a user
 */
public class AuthenticatorAsyncTask  extends AsyncTask<Object, Void, RemoteOperationResult> {

    private static String REMOTE_PATH = "/";
    private static boolean SUCCESS_IF_ABSENT = false;

    private Context mContext;
    private final WeakReference<OnAuthenticatorTaskListener> mListener;

    public AuthenticatorAsyncTask(Activity activity) {
        mContext = activity.getApplicationContext();
        mListener = new WeakReference<>((OnAuthenticatorTaskListener)activity);
    }

    @Override
    protected RemoteOperationResult doInBackground(Object... params) {

        RemoteOperationResult result;
        if (params!= null && params.length==2) {
            String url = (String)params[0];
            OwnCloudCredentials credentials = (OwnCloudCredentials)params[1];

            // Client
            Uri uri = Uri.parse(url);
            OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(uri, mContext, true);
            client.setCredentials(credentials);

            // Operation - try credentials
            ExistenceCheckRemoteOperation existenceCheckRemoteOperation = new ExistenceCheckRemoteOperation(
                    REMOTE_PATH,
                    SUCCESS_IF_ABSENT
            );
            result = existenceCheckRemoteOperation.execute(client);

            String targetUrlAfterPermanentRedirection = null;
            if (existenceCheckRemoteOperation.wasRedirected()) {
                RedirectionPath redirectionPath = existenceCheckRemoteOperation.getRedirectionPath();
                targetUrlAfterPermanentRedirection = redirectionPath.getLastPermanentLocation();
            }

            // Operation - get display name
            if (result.isSuccess()) {
                GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();
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

        if (result!= null)
        {
            OnAuthenticatorTaskListener listener = mListener.get();
            if (listener!= null)
            {
                listener.onAuthenticatorTaskCallback(result);
            }
        }
    }
    /*
     * Interface to retrieve data from recognition task
     */
    public interface OnAuthenticatorTaskListener{

        void onAuthenticatorTaskCallback(RemoteOperationResult result);
    }
}