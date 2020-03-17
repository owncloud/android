/* ownCloud Android Library is available under MIT license
 *
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

package com.owncloud.android.lib.resources.users;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import timber.log.Timber;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Gets avatar about the user logged in, if available
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class GetRemoteUserAvatarOperation extends RemoteOperation<GetRemoteUserAvatarOperation.ResultData> {

    private static final String NON_OFFICIAL_AVATAR_PATH = "/index.php/avatar/";

    /**
     * Desired size in pixels of the squared image
     */
    private int mDimension;

    public GetRemoteUserAvatarOperation(int dimension) {
        mDimension = dimension;
    }

    @Override
    protected RemoteOperationResult<ResultData> run(OwnCloudClient client) {
        GetMethod getMethod = null;
        RemoteOperationResult<ResultData> result;
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = null;

        try {
            final String url = client.getBaseUri() + NON_OFFICIAL_AVATAR_PATH + client.getCredentials().getUsername() + "/" + mDimension;
            Timber.d("avatar URI: %s", url);

            getMethod = new GetMethod(new URL(url));

            int status = client.executeHttpMethod(getMethod);

            if (isSuccess(status)) {
                // find out size of file to read
                int totalToTransfer = 0;
                String contentLength = getMethod.getResponseHeader(HttpConstants.CONTENT_LENGTH_HEADER);

                if (contentLength != null && contentLength.length() > 0) {
                    totalToTransfer = Integer.parseInt(contentLength);
                }

                // find out MIME-type!
                String mimeType;
                String contentType = getMethod.getResponseHeader(HttpConstants.CONTENT_TYPE_HEADER);

                if (contentType == null || !contentType.startsWith("image")) {
                    Timber.w("Not an image, failing with no avatar");
                    result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.FILE_NOT_FOUND);
                    return result;
                }

                mimeType = contentType;

                /// download will be performed to a buffer
                inputStream = getMethod.getResponseBodyAsStream();
                bis = new BufferedInputStream(inputStream);
                bos = new ByteArrayOutputStream(totalToTransfer);

                byte[] bytes = new byte[4096];
                int readResult;
                while ((readResult = bis.read(bytes)) != -1) {
                    bos.write(bytes, 0, readResult);
                }
                // TODO check total bytes transferred?

                // find out etag
                String etag = WebdavUtils.getEtagFromResponse(getMethod);
                if (etag.length() == 0) {
                    Timber.w("Could not read Etag from avatar");
                }

                // Result
                result = new RemoteOperationResult<>(OK);
                result.setData(new ResultData(bos.toByteArray(), mimeType, etag));

            } else {
                result = new RemoteOperationResult<>(getMethod);
                client.exhaustResponse(getMethod.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Timber.e(e, "Exception while getting OC user avatar");

        } finally {
            if (getMethod != null) {
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
                    Timber.e(i, "Unexpected exception closing input stream");
                }
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException o) {
                    Timber.e(o, "Unexpected exception closing output stream");
                }
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
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