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

package com.owncloud.android.lib.resources.files;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.CopyMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import timber.log.Timber;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Remote operation moving a remote file or folder in the ownCloud server to a different folder
 * in the same account.
 *
 * Allows renaming the moving file/folder at the same time.
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González V.
 */
public class CopyRemoteFileOperation extends RemoteOperation<String> {

    private static final int COPY_READ_TIMEOUT = 600000;
    private static final int COPY_CONNECTION_TIMEOUT = 5000;

    private String mSrcRemotePath;
    private String mTargetRemotePath;

    private boolean mOverwrite;

    /**
     * Constructor.
     * <p/>
     * TODO Paths should finish in "/" in the case of folders. ?
     *
     * @param srcRemotePath    Remote path of the file/folder to move.
     * @param targetRemotePath Remove path desired for the file/folder after moving it.
     */
    public CopyRemoteFileOperation(String srcRemotePath, String targetRemotePath, boolean overwrite
    ) {
        mSrcRemotePath = srcRemotePath;
        mTargetRemotePath = targetRemotePath;
        mOverwrite = overwrite;
    }

    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult<String> run(OwnCloudClient client) {

        if (mTargetRemotePath.equals(mSrcRemotePath)) {
            // nothing to do!
            return new RemoteOperationResult<>(ResultCode.OK);
        }

        if (mTargetRemotePath.startsWith(mSrcRemotePath)) {
            return new RemoteOperationResult<>(ResultCode.INVALID_COPY_INTO_DESCENDANT);
        }

        /// perform remote operation
        RemoteOperationResult result;
        try {
            CopyMethod copyMethod =
                    new CopyMethod(
                            new URL(client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mSrcRemotePath)),
                    client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mTargetRemotePath),
                    mOverwrite);

            copyMethod.setReadTimeout(COPY_READ_TIMEOUT, TimeUnit.SECONDS);
            copyMethod.setConnectionTimeout(COPY_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

            final int status = client.executeHttpMethod(copyMethod);

            if (status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT) {
                String fileRemoteId = copyMethod.getResponseHeader(HttpConstants.OC_FILE_REMOTE_ID);
                result = new RemoteOperationResult<>(ResultCode.OK);
                result.setData(fileRemoteId);
            } else if (status == HttpConstants.HTTP_PRECONDITION_FAILED && !mOverwrite) {
                result = new RemoteOperationResult<>(ResultCode.INVALID_OVERWRITE);
                client.exhaustResponse(copyMethod.getResponseBodyAsStream());

                /// for other errors that could be explicitly handled, check first:
                /// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4
            } else {

                result = new RemoteOperationResult<>(copyMethod);
                client.exhaustResponse(copyMethod.getResponseBodyAsStream());
            }

            Timber.i("Copy " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Timber.e(e, "Copy " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + result.getLogMessage());
        }

        return result;
    }
}
