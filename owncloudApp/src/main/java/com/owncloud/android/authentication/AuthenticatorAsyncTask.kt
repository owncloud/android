/**
 * ownCloud Android client application
 *
 * @author masensio on 09/02/2015.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.authentication

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import java.lang.ref.WeakReference

/**
 * Async Task to verify the credentials of a user
 */
class AuthenticatorAsyncTask internal constructor(activity: Activity) :
    AsyncTask<Any, Void, RemoteOperationResult<*>>() {

    private val mContext: Context
    private val mListener: WeakReference<OnAuthenticatorTaskListener>

    init {
        mContext = activity.applicationContext
        mListener = WeakReference(activity as OnAuthenticatorTaskListener)
    }

    override fun doInBackground(vararg params: Any): RemoteOperationResult<*> {

        var result: RemoteOperationResult<*>
        if (params != null && params.size == 2) {
            val url = params[0] as String
            val credentials = params[1] as OwnCloudCredentials

            // Client
            val uri = Uri.parse(url)
            var client = OwnCloudClientFactory.createOwnCloudClient(uri, mContext, true)
            client.credentials = credentials

            // Operation - try credentials
            val REMOTE_PATH = "/"
            val SUCCESS_IF_ABSENT = false
            val existenceCheckRemoteOperation = ExistenceCheckRemoteOperation(
                REMOTE_PATH,
                SUCCESS_IF_ABSENT,
                true
            )
            result = existenceCheckRemoteOperation.execute(client)

            var targetUrlAfterPermanentRedirection: String? = null
            if (existenceCheckRemoteOperation.wasRedirected()) {
                val redirectionPath = existenceCheckRemoteOperation.redirectionPath
                targetUrlAfterPermanentRedirection = redirectionPath.lastPermanentLocation
            }

            // Operation - get display name
            if (result.isSuccess) {
                val remoteUserNameOperation = GetRemoteUserInfoOperation()
                if (targetUrlAfterPermanentRedirection != null) {
                    // we can't assume that any subpath of the domain is correctly redirected; ugly stuff
                    client = OwnCloudClientFactory.createOwnCloudClient(
                        Uri.parse(
                            AccountUtils.trimWebdavSuffix(
                                targetUrlAfterPermanentRedirection
                            )
                        ),
                        mContext,
                        true
                    )
                    client.credentials = credentials
                }
                result = remoteUserNameOperation.execute(client)
            }

            // let the caller knows what is real URL that should be accessed for the account
            // being authenticated if the initial URL is being redirected permanently (HTTP code 301)
            result.setLastPermanentLocation(targetUrlAfterPermanentRedirection)

        } else {
            result = RemoteOperationResult<Any>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
        }

        return result
    }

    override fun onPostExecute(result: RemoteOperationResult<*>?) {

        if (result != null) {
            val listener = mListener.get()
            listener?.onAuthenticatorTaskCallback(result)
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    interface OnAuthenticatorTaskListener {

        fun onAuthenticatorTaskCallback(result: RemoteOperationResult<*>)
    }
}
