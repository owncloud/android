/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.status;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

import javax.net.ssl.SSLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Checks if the server is valid and if the server supports the Share API
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */

public class GetRemoteStatusOperation extends RemoteOperation<OwnCloudVersion> {

    /**
     * Maximum time to wait for a response from the server when the connection is being tested,
     * in MILLISECONDs.
     */
    public static final long TRY_CONNECTION_TIMEOUT = 5000;

    private static final String NODE_INSTALLED = "installed";
    private static final String NODE_VERSION = "version";
    private static final String HTTPS_PREFIX = "https://";
    private static final String HTTP_PREFIX = "http://";

    private RemoteOperationResult<OwnCloudVersion> mLatestResult;
    private Context mContext;

    public GetRemoteStatusOperation(Context context) {
        mContext = context;
    }

    private boolean tryConnection(OwnCloudClient client) {
        boolean retval = false;
        String baseUrlSt = client.getBaseUri().toString();
        try {
            GetMethod getMethod = new GetMethod(new URL(baseUrlSt + OwnCloudClient.STATUS_PATH));

            getMethod.setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
            getMethod.setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

            client.setFollowRedirects(false);
            boolean isRedirectToNonSecureConnection = false;
            int status;
            try {
                status = client.executeHttpMethod(getMethod);
                mLatestResult = isSuccess(status)
                        ? new RemoteOperationResult<>(OK)
                        : new RemoteOperationResult<>(getMethod);
            } catch (SSLException sslE) {
                mLatestResult = new RemoteOperationResult(sslE);
                return false;
            }

            String redirectedLocation = mLatestResult.getRedirectedLocation();
            while (redirectedLocation != null && redirectedLocation.length() > 0
                    && !mLatestResult.isSuccess()) {

                isRedirectToNonSecureConnection |= (
                        baseUrlSt.startsWith(HTTPS_PREFIX) &&
                                redirectedLocation.startsWith(HTTP_PREFIX)
                );

                getMethod = new GetMethod(new URL(redirectedLocation));
                getMethod.setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
                getMethod.setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

                status = client.executeHttpMethod(getMethod);
                mLatestResult = new RemoteOperationResult<>(getMethod);
                redirectedLocation = mLatestResult.getRedirectedLocation();
            }

            if (isSuccess(status)) {

                JSONObject respJSON = new JSONObject(getMethod.getResponseBodyAsString());
                if (!respJSON.getBoolean(NODE_INSTALLED)) {
                    mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
                } else {
                    String version = respJSON.getString(NODE_VERSION);
                    OwnCloudVersion ocVersion = new OwnCloudVersion(version);
                    /// the version object will be returned even if the version is invalid, no error code;
                    /// every app will decide how to act if (ocVersion.isVersionValid() == false)

                    if (isRedirectToNonSecureConnection) {
                        mLatestResult = new RemoteOperationResult<>(
                                RemoteOperationResult.ResultCode.
                                        OK_REDIRECT_TO_NON_SECURE_CONNECTION);
                    } else {
                        mLatestResult = new RemoteOperationResult<>(
                                baseUrlSt.startsWith(HTTPS_PREFIX) ?
                                        RemoteOperationResult.ResultCode.OK_SSL :
                                        RemoteOperationResult.ResultCode.OK_NO_SSL);
                    }

                    mLatestResult.setData(ocVersion);
                    retval = true;
                }

            } else {
                mLatestResult = new RemoteOperationResult<>(getMethod);
            }

        } catch (JSONException e) {
            mLatestResult = new RemoteOperationResult<>(
                    RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);

        } catch (Exception e) {
            mLatestResult = new RemoteOperationResult<>(e);
        }

        if (mLatestResult.isSuccess()) {
            Timber.i("Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());

        } else if (mLatestResult.getException() != null) {
            Timber.e(mLatestResult.getException(), "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());

        } else {
            Timber.e("Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());
        }

        return retval;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    protected RemoteOperationResult<OwnCloudVersion> run(OwnCloudClient client) {
        if (!isOnline()) {
            return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        String baseUriStr = client.getBaseUri().toString();
        if (baseUriStr.startsWith(HTTP_PREFIX) || baseUriStr.startsWith(HTTPS_PREFIX)) {
            tryConnection(client);

        } else {
            client.setBaseUri(Uri.parse(HTTPS_PREFIX + baseUriStr));
            boolean httpsSuccess = tryConnection(client);
            if (!httpsSuccess && !mLatestResult.isSslRecoverableException()) {
                Timber.d("Establishing secure connection failed, trying non secure connection");
                client.setBaseUri(Uri.parse(HTTP_PREFIX + baseUriStr));
                tryConnection(client);
            }
        }
        return mLatestResult;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }
}