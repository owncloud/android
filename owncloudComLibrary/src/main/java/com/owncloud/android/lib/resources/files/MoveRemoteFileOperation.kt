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

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.MoveMethod;
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
 * <p>
 * Allows renaming the moving file/folder at the same time.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class MoveRemoteFileOperation extends RemoteOperation {

    private static final int MOVE_READ_TIMEOUT = 600000;
    private static final int MOVE_CONNECTION_TIMEOUT = 5000;

    private String mSrcRemotePath;
    private String mTargetRemotePath;
    private boolean mOverwrite;

    protected boolean moveChunkedFile = false;
    protected String mFileLastModifTimestamp;
    protected long mFileLength;

    /**
     * Constructor.
     * <p>
     * TODO Paths should finish in "/" in the case of folders. ?
     *
     * @param srcRemotePath    Remote path of the file/folder to move.
     * @param targetRemotePath Remote path desired for the file/folder after moving it.
     */
    public MoveRemoteFileOperation(String srcRemotePath,
                                   String targetRemotePath,
                                   boolean overwrite) {

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
    protected RemoteOperationResult run(OwnCloudClient client) {
        if (mTargetRemotePath.equals(mSrcRemotePath)) {
            // nothing to do!
            return new RemoteOperationResult<>(ResultCode.OK);
        }

        if (mTargetRemotePath.startsWith(mSrcRemotePath)) {
            return new RemoteOperationResult<>(ResultCode.INVALID_MOVE_INTO_DESCENDANT);
        }

        /// perform remote operation
        RemoteOperationResult result;
        try {
            // After finishing a chunked upload, we have to move the resulting file from uploads folder to files one,
            // so this uri has to be customizable
            Uri srcWebDavUri = moveChunkedFile ? client.getUploadsWebDavUri() : client.getUserFilesWebDavUri();

            final MoveMethod move = new MoveMethod(
                    new URL(srcWebDavUri + WebdavUtils.encodePath(mSrcRemotePath)),
                    client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mTargetRemotePath),
                    mOverwrite);

            if (moveChunkedFile) {
                move.addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, mFileLastModifTimestamp);
                move.addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, String.valueOf(mFileLength));
            }

            move.setReadTimeout(MOVE_READ_TIMEOUT, TimeUnit.SECONDS);
            move.setConnectionTimeout(MOVE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

            final int status = client.executeHttpMethod(move);
            /// process response
            if (isSuccess(status)) {
                result = new RemoteOperationResult<>(ResultCode.OK);
            } else if (status == HttpConstants.HTTP_PRECONDITION_FAILED && !mOverwrite) {

                result = new RemoteOperationResult<>(ResultCode.INVALID_OVERWRITE);
                client.exhaustResponse(move.getResponseBodyAsStream());

                /// for other errors that could be explicitly handled, check first:
                /// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4

            } else {
                result = new RemoteOperationResult<>(move);
                client.exhaustResponse(move.getResponseBodyAsStream());
            }

            Timber.i("Move " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Timber.e(e, "Move " + mSrcRemotePath + " to " + mTargetRemotePath + ": " + result.getLogMessage());
        }

        return result;
    }

    protected boolean isSuccess(int status) {
        return status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT;
    }
}
