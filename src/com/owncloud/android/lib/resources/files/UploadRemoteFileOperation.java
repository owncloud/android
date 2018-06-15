/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.HttpUtils;
import com.owncloud.android.lib.common.network.FileRequestBody;
import com.owncloud.android.lib.common.network.FileRequestEntity;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.ProgressiveDataTransferer;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.http.methods.webdav.PutMethod;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the upload of a remote file to the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */

public class UploadRemoteFileOperation extends RemoteOperation {

    private static final String TAG = UploadRemoteFileOperation.class.getSimpleName();

    protected String mLocalPath;
    protected String mRemotePath;
    protected String mMimeType;
    protected String mFileLastModifTimestamp;
    protected PutMethod mPutMethod = null;
    protected String mRequiredEtag = null;

    protected final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    protected Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    protected RequestEntity mEntity = null;

    public UploadRemoteFileOperation(String localPath, String remotePath, String mimeType,
                                     String fileLastModifTimestamp) {
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mMimeType = mimeType;
        mFileLastModifTimestamp = fileLastModifTimestamp;
    }

    public UploadRemoteFileOperation(String localPath, String remotePath, String mimeType,
                                     String requiredEtag, String fileLastModifTimestamp) {
        this(localPath, remotePath, mimeType, fileLastModifTimestamp);
        mRequiredEtag = requiredEtag;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;

//        DefaultHttpMethodRetryHandler oldRetryHandler =
//            (DefaultHttpMethodRetryHandler) client.getParams().getParameter(HttpMethodParams.RETRY_HANDLER);

        try {
            // TODO
            // prevent that uploads are retried automatically by network library
//            client.getParams().setParameter(
//                HttpMethodParams.RETRY_HANDLER,
//                new DefaultHttpMethodRetryHandler(0, false)
//            );

            mPutMethod = new PutMethod(
                    HttpUtils.stringUrlToHttpUrl(client.getNewWebDavUri() + WebdavUtils.encodePath(mRemotePath)));

            if (mCancellationRequested.get()) {
                // the operation was cancelled before getting it's turn to be executed in the queue of uploads
                result = new RemoteOperationResult(new OperationCancelledException());

            } else {
                // perform the upload
                result = uploadFile(client);
            }

        } catch (Exception e) {
//            if (mPutMethod != null && mPutMethod.isAborted()) {
//                result = new RemoteOperationResult(new OperationCancelledException());
//
//            } else {
//                result = new RemoteOperationResult(e);
//            }
        } finally {
            // reset previous retry handler
//            client.getParams().setParameter(
//                HttpMethodParams.RETRY_HANDLER,
//                oldRetryHandler
//            );
        }
        return result;
    }

    protected RemoteOperationResult uploadFile(OwnCloudClient client) throws IOException {
        RemoteOperationResult result;
        try {
            // Headers
            File fileToUpload = new File(mLocalPath);
            // TODO
//            synchronized (mDataTransferListeners) {
//                ((ProgressiveDataTransferer)mEntity)
//                        .addDatatransferProgressListeners(mDataTransferListeners);
//            }

            if (mRequiredEtag != null && mRequiredEtag.length() > 0) {
                mPutMethod.addRequestHeader(HttpConstants.IF_MATCH_HEADER, "\"" + mRequiredEtag + "\"");
            }

            mPutMethod.addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, String.valueOf(fileToUpload.length()));

            mPutMethod.addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, mFileLastModifTimestamp);

            // Request body
            MediaType mediaType = MediaType.parse(mMimeType);

            mPutMethod.setRequestBody(
                    FileRequestBody.create(mediaType, fileToUpload)
            );

            int status = client.executeHttpMethod(mPutMethod);

            if (isSuccess(status)) {
                result = new RemoteOperationResult(OK);

            } else { // synchronization failed
                result = new RemoteOperationResult(mPutMethod);
            }

            client.exhaustResponse(mPutMethod.getResponseAsStream());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
        }
        return result;
    }

    public Set<OnDatatransferProgressListener> getDataTransferListeners() {
        return mDataTransferListeners;
    }
    
    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).addDatatransferProgressListener(listener);
        }
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
        if (mEntity != null) {
            ((ProgressiveDataTransferer)mEntity).removeDatatransferProgressListener(listener);
        }
    }
    
    public void cancel() {
//        synchronized (mCancellationRequested) {
//            mCancellationRequested.set(true);
//            if (mPutMethod != null)
//                mPutMethod.abort();
//        }
    }

    public boolean isSuccess(int status) {
        return ((status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_CREATED ||
                status == HttpConstants.HTTP_NO_CONTENT));
    }
}