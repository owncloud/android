/* ownCloud Android Library is available under MIT license
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.users;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * Gets avatar about the user logged in, if available
 */

public class GetRemoteUserAvatarOperation extends RemoteOperation {

    private static final String TAG = GetRemoteUserAvatarOperation.class.getSimpleName();

    private static final String NON_OFFICIAL_AVATAR_PATH = "/index.php/avatar/";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";

    /** Desired size in pixels of the squared image */
    private int mDimension;

    /**
     * Etag of current local copy of the avatar; if not null, remote avatar will be downloaded only
     * if its Etag changed.
     */
    private String mCurrentEtag;


    public GetRemoteUserAvatarOperation(int dimension, String currentEtag) {
        mDimension = dimension;
        mCurrentEtag = currentEtag;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        GetMethod get = null;
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = null;

        try {
            String uri =
                client.getBaseUri() + NON_OFFICIAL_AVATAR_PATH +
                client.getCredentials().getUsername() + "/" + mDimension;
            ;
            Log_OC.d(TAG, "avatar URI: " + uri);
            get = new GetMethod(uri);
            /*  Conditioned call is corrupting the input stream of the connection.
                Seems that response with 304 is also including the avatar in the response body,
                though it's forbidden by HTTPS specification. Besides, HTTPClient library
                assumes there is nothing in the response body, but the bytes are read
                by the next request, resulting in an exception due to a corrupt status line

                Maybe when we have a real API we can enable this again.

            if (mCurrentEtag != null && mCurrentEtag.length() > 0) {
                get.addRequestHeader(IF_NONE_MATCH_HEADER, "\"" + mCurrentEtag + "\"");
            }
            */

            //get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            int status = client.executeMethod(get);
            if (isSuccess(status)) {

                // find out size of file to read
                int totalToTransfer = 0;
                Header contentLength = get.getResponseHeader("Content-Length");
                if (contentLength != null && contentLength.getValue().length() > 0) {
                    totalToTransfer = Integer.parseInt(contentLength.getValue());
                }

                // find out MIME-type!
                String mimeType;
                Header contentType = get.getResponseHeader("Content-Type");
                if (contentType == null || !contentType.getValue().startsWith("image")) {
                    Log_OC.e(
                        TAG, "Not an image, failing with no avatar"
                    );
                    result = new RemoteOperationResult(
                        RemoteOperationResult.ResultCode.FILE_NOT_FOUND
                    );
                    return result;
                }
                mimeType = contentType.getValue();

                /// download will be performed to a buffer
                inputStream = get.getResponseBodyAsStream();
                bis = new BufferedInputStream(inputStream);
                bos = new ByteArrayOutputStream(totalToTransfer);

                long transferred = 0;
                byte[] bytes = new byte[4096];
                int readResult = 0;
                while ((readResult = bis.read(bytes)) != -1) {
                    bos.write(bytes, 0, readResult);
                    transferred += readResult;
                }
                // TODO check total bytes transferred?

                // find out etag
                String etag = WebdavUtils.getEtagFromResponse(get);
                if (etag.length() == 0) {
                    Log_OC.w(TAG, "Could not read Etag from avatar");
                }

                // Result
                result = new RemoteOperationResult(true, get);
                ResultData resultData = new ResultData(bos.toByteArray(), mimeType, etag);
                ArrayList<Object> data = new ArrayList<Object>();
                data.add(resultData);
                result.setData(data);

            } else {
                result = new RemoteOperationResult(false, get);
                client.exhaustResponse(get.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting OC user avatar", e);

        } finally {
            if (get != null) {
                try {
                    if (inputStream != null) {
                        client.exhaustResponse(inputStream);
                        if (bis != null) {
                            bis.close();
                        } else {
                            inputStream.close();
                        }
                    }
                } catch (IOException i) {
                    Log_OC.e(TAG, "Unexpected exception closing input stream ", i);
                }
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException o) {
                    Log_OC.e(TAG, "Unexpected exception closing output stream ", o);
                }
                get.releaseConnection();
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }

    public static class ResultData {
        private String mEtag;
        private String mMimeType;
        private byte[] mAvatarData;

        ResultData(byte[] avatarData, String mimeType, String etag) {
            mAvatarData = avatarData;
            mMimeType = (mimeType == null) ? "" : mimeType;
            mEtag = (etag == null) ? "" : etag;
        }

        public String getEtag() {
            return mEtag;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public byte[] getAvatarData() {
            return mAvatarData;
        }
    }

}
