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
 */

package com.owncloud.android.lib.refactor.operations.files;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.operations.RemoteOperation;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import at.bitfire.dav4android.DavOCResource;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.owncloud.android.lib.refactor.operations.RemoteOperationResult.ResultCode.OK;

/**
 * @author David González Verdugo
 */
public class UploadRemoteFileOperation extends RemoteOperation<Void> {

    private File mFileToUpload;
    private String mRemotePath;
    private String mMimeType;
    private String mFileLastModifTimestamp;
    private String mRequiredEtag;

    protected final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);


    public UploadRemoteFileOperation(OCContext ocContext, String localPath, String remotePath, String mimeType,
                                     String fileLastModifTimestamp) {
        super(ocContext);

        mFileToUpload = new File(localPath);
        mRemotePath = remotePath.replaceAll("^/+", ""); //Delete leading slashes
        mMimeType = mimeType;
        mFileLastModifTimestamp = fileLastModifTimestamp;
    }

    public UploadRemoteFileOperation(OCContext ocContext, String localPath, String remotePath, String mimeType,
                                     String requiredEtag, String fileLastModifTimestamp) {
        this(ocContext, localPath, remotePath, mimeType, fileLastModifTimestamp);
        mRequiredEtag = requiredEtag;
    }

    @Override
    public Result exec() {

        try {

            MediaType mediaType = MediaType.parse(mMimeType);
            RequestBody requestBody = RequestBody.create(mediaType, mFileToUpload);

            DavOCResource davOCResource = new DavOCResource(
                    getClient(),
                    getWebDavHttpUrl(mRemotePath));

            davOCResource.put(
                    requestBody,
                    null,
                    false,
                    "multipart/form-data",
                    String.valueOf(mFileToUpload.length()),
                    mFileLastModifTimestamp
            );

            return new Result(OK);

        } catch (Exception e) {
            return new Result(e);
        }
    }

    public void cancel() {
        synchronized (mCancellationRequested) {
            mCancellationRequested.set(true);
            if (mPutMethod != null)
                mPutMethod.abort();
        }
    }
}