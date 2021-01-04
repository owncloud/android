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

package com.owncloud.android.lib.resources.files.chunks;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.methods.webdav.PutMethod;
import com.owncloud.android.lib.common.network.ChunkFromFileRequestBody;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import okhttp3.MediaType;
import timber.log.Timber;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the chunked upload of a remote file to the ownCloud server.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class ChunkedUploadRemoteFileOperation extends UploadRemoteFileOperation {

    public static final long CHUNK_SIZE = 1024000;
    private static final int LAST_CHUNK_TIMEOUT = 900000; //15 mins.

    private String mTransferId;

    public ChunkedUploadRemoteFileOperation(String transferId, String localPath, String remotePath, String mimeType,
                                            String requiredEtag, String fileLastModifTimestamp) {
        super(localPath, remotePath, mimeType, requiredEtag, fileLastModifTimestamp);
        mTransferId = transferId;
    }

    @Override
    protected RemoteOperationResult uploadFile(OwnCloudClient client) throws Exception {
        int status;
        RemoteOperationResult result = null;
        FileChannel channel;
        RandomAccessFile raf;

        File fileToUpload = new File(mLocalPath);
        MediaType mediaType = MediaType.parse(mMimeType);

        raf = new RandomAccessFile(fileToUpload, "r");
        channel = raf.getChannel();

        mFileRequestBody = new ChunkFromFileRequestBody(fileToUpload, mediaType, channel, CHUNK_SIZE);

        synchronized (mDataTransferListeners) {
            mFileRequestBody.addDatatransferProgressListeners(mDataTransferListeners);
        }

        long offset = 0;
        String uriPrefix = client.getUploadsWebDavUri() + File.separator + mTransferId;
        long totalLength = fileToUpload.length();
        long chunkCount = (long) Math.ceil((double) totalLength / CHUNK_SIZE);

        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++, offset += CHUNK_SIZE) {

            ((ChunkFromFileRequestBody) mFileRequestBody).setOffset(offset);

            if (mCancellationRequested.get()) {
                result = new RemoteOperationResult<>(new OperationCancelledException());
                break;
            } else {
                mPutMethod = new PutMethod(client,
                        new URL(uriPrefix + File.separator + chunkIndex), mFileRequestBody);

                if (chunkIndex == chunkCount - 1) {
                    // Added a high timeout to the last chunk due to when the last chunk
                    // arrives to the server with the last PUT, all chunks get assembled
                    // within that PHP request, so last one takes longer.
                    mPutMethod.setReadTimeout(LAST_CHUNK_TIMEOUT, TimeUnit.MILLISECONDS);
                }

                status = client.executeHttpMethod(mPutMethod);

                Timber.d("Upload of " + mLocalPath + " to " + mRemotePath +
                        ", chunk index " + chunkIndex + ", count " + chunkCount +
                        ", HTTP result status " + status);

                if (isSuccess(status)) {
                    result = new RemoteOperationResult<>(OK);
                } else {
                    result = new RemoteOperationResult<>(mPutMethod);
                    break;
                }
            }
        }

        if (channel != null) {
            channel.close();
        }

        if (raf != null) {
            raf.close();
        }

        return result;
    }
}