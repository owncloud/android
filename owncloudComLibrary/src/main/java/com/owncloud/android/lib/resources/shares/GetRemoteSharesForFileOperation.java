/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.net.URL;

/**
 * Provide a list shares for a specific file.
 * The input is the full path of the desired file.
 * The output is a list of everyone who has the file shared with them.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class GetRemoteSharesForFileOperation extends RemoteOperation<ShareParserResult> {

    private static final String TAG = GetRemoteSharesForFileOperation.class.getSimpleName();

    private static final String PARAM_PATH = "path";
    private static final String PARAM_RESHARES = "reshares";
    private static final String PARAM_SUBFILES = "subfiles";

    private String mRemoteFilePath;
    private boolean mReshares;
    private boolean mSubfiles;

    /**
     * Constructor
     *
     * @param remoteFilePath Path to file or folder
     * @param reshares       If set to false (default), only shares owned by the current user are
     *                       returned.
     *                       If set to true, shares owned by any user from the given file are returned.
     * @param subfiles       If set to false (default), lists only the folder being shared
     *                       If set to true, all shared files within the folder are returned.
     */
    public GetRemoteSharesForFileOperation(String remoteFilePath, boolean reshares,
                                           boolean subfiles) {
        mRemoteFilePath = remoteFilePath;
        mReshares = reshares;
        mSubfiles = subfiles;
    }

    @Override
    protected RemoteOperationResult<ShareParserResult> run(OwnCloudClient client) {
        RemoteOperationResult<ShareParserResult> result;

        try {

            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH);
            uriBuilder.appendQueryParameter(PARAM_PATH, mRemoteFilePath);
            uriBuilder.appendQueryParameter(PARAM_RESHARES, String.valueOf(mReshares));
            uriBuilder.appendQueryParameter(PARAM_SUBFILES, String.valueOf(mSubfiles));

            GetMethod getMethod = new GetMethod(new URL(uriBuilder.build().toString()));

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeHttpMethod(getMethod);

            if (isSuccess(status)) {
                // Parse xml response and obtain the list of shares
                ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                        new ShareXMLParser()
                );
                parser.setOwnCloudVersion(client.getOwnCloudVersion());
                parser.setServerBaseUri(client.getBaseUri());
                result = parser.parse(getMethod.getResponseBodyAsString());

                if (result.isSuccess()) {
                    Log_OC.d(TAG, "Got " + result.getData().getShares().size() + " shares");
                }
            } else {
                result = new RemoteOperationResult<>(getMethod);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while getting shares", e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }
}