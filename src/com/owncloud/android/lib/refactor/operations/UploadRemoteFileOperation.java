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

package com.owncloud.android.lib.refactor.operations;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.RemoteOperation;
import com.owncloud.android.lib.refactor.RemoteOperationResult;

import java.io.File;

import at.bitfire.dav4android.DavResource;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.owncloud.android.lib.refactor.RemoteOperationResult.ResultCode.OK;

/**
 * @author David González Verdugo
 */
public class UploadRemoteFileOperation extends RemoteOperation<Void> {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String OC_TOTAL_LENGTH_HEADER = "OC-Total-Length";
    private static final String OC_X_OC_MTIME_HEADER = "X-OC-Mtime";
    private static final String IF_MATCH_HEADER = "If-Match";

    private File mFileToUpload;
    private String mRemotePath;
    private String mMimeType;
    private String mFileLastModifTimestamp;


    public UploadRemoteFileOperation(OCContext context, String localPath, String remotePath, String mimetype,
                                     String fileLastModifTimestamp) {
        super(context);

        mFileToUpload = new File(localPath);
        mRemotePath = remotePath.replaceAll("^/+", ""); //Delete leading slashes
        mMimeType = mimetype;
        mFileLastModifTimestamp = fileLastModifTimestamp;
    }

    @Override
    public Result exec() {

        try {

            MediaType mediaType = MediaType.parse(mMimeType);
            RequestBody requestBody = RequestBody.create(mediaType, mFileToUpload);

            DavResource davResource = new DavResource(
                    getClient()
                            .newBuilder()
                            .addInterceptor(chain ->
                                    chain.proceed(
                                            addUploadHeaders(chain.request())
                                            .build()))
                            .followRedirects(false)
                            .build(),
                    getWebDavHttpUrl(mRemotePath));

            davResource.put(requestBody,
                    null,
                    false);

        } catch (Exception e) {
            return new Result(e);
        }

        return new Result(OK);
    }

    /**
     * Add headers needed to upload a file
     * @param request request in which include the headers
     * @return request with upload headers
     */
    private Request.Builder addUploadHeaders (Request request) {

        Request.Builder builder =  request.newBuilder();
        builder.addHeader(CONTENT_TYPE_HEADER, "multipart/form-data");
        builder.addHeader(OC_TOTAL_LENGTH_HEADER, String.valueOf(mFileToUpload.length()));
        builder.addHeader(OC_X_OC_MTIME_HEADER, mFileLastModifTimestamp);

        return builder;
    }
}