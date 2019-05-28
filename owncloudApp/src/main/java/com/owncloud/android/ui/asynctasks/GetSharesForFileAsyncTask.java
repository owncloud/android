/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 * <p>
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

package com.owncloud.android.ui.asynctasks;

import android.accounts.Account;
import android.os.AsyncTask;
import android.util.Pair;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.GetSharesForFileOperation;

import java.lang.ref.WeakReference;

/**
 * Async Task to get the users and groups which a file is shared with
 */
public class GetSharesForFileAsyncTask extends AsyncTask<Object, Void, Pair<RemoteOperation, RemoteOperationResult>> {

    private final String TAG = GetSharesForFileAsyncTask.class.getSimpleName();
    private final WeakReference<OnRemoteOperationListener> mListener;

    public GetSharesForFileAsyncTask(OnRemoteOperationListener listener) {
        mListener = new WeakReference<>(listener);
    }

    @Override
    protected Pair<RemoteOperation, RemoteOperationResult> doInBackground(Object... params) {

        GetSharesForFileOperation operation = null;
        RemoteOperationResult result;

        if (params != null && params.length == 3) {
            OCFile file = (OCFile) params[0];
            Account account = (Account) params[1];
            FileDataStorageManager fileDataStorageManager = (FileDataStorageManager) params[2];

            try {
                // Get shares request
                operation = new GetSharesForFileOperation(file.getRemotePath(), true, false);
                OwnCloudAccount ocAccount = new OwnCloudAccount(
                        account,
                        MainApp.Companion.getAppContext()
                );
                OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, MainApp.Companion.getAppContext());
                result = operation.execute(client, fileDataStorageManager);

            } catch (Exception e) {
                result = new RemoteOperationResult<>(e);
                Log_OC.e(TAG, "Exception while getting shares", e);
            }
        } else {
            result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
        }

        return new Pair(operation, result);
    }

    @Override
    protected void onPostExecute(Pair<RemoteOperation, RemoteOperationResult> result) {
        // a cancelled task shouldn't call the listener, even if the reference exists;
        // the Activity responsible could be stopped, and its abilities to do things constrained
        if (result != null && !isCancelled()) {
            OnRemoteOperationListener listener = mListener.get();
            if (listener != null) {
                listener.onRemoteOperationFinish(result.first, result.second);
            }
        }
    }
}