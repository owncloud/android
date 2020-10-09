/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;

import com.owncloud.android.datamodel.OCUpload;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.chunks.ChunkedUploadRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.io.File;
import java.util.Iterator;

public class ChunkedUploadFileOperation extends UploadFileOperation {

    private String mTransferId;

    public ChunkedUploadFileOperation(Account account, OCFile file, OCUpload upload, boolean forceOverwrite,
                                      int localBehaviour, Context context) {
        super(account, file, upload, forceOverwrite, localBehaviour, context);
        mTransferId = upload.getTransferId();
    }

    @Override
    protected RemoteOperationResult uploadRemoteFile(OwnCloudClient client, File temporalFile, File originalFile,
                                                     String expectedPath, File expectedFile, String timeStamp) {
        try {
            RemoteOperationResult result;

            // Step 1, create folder where we put the uploaded file chunks
            result = createChunksFolder(String.valueOf(mTransferId));

            if (!result.isSuccess()) {
                return result;
            }

            // Step 2, start to upload chunks
            mUploadOperation = new ChunkedUploadRemoteFileOperation(mTransferId, mFile.getStoragePath(),
                    mFile.getRemotePath(), mFile.getMimeType(), mFile.getEtagInConflict(), timeStamp);

            Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            result = mUploadOperation.execute(client);

            // File chunks not properly uploaded
            if (!result.isSuccess()) {
                return result;
            }

            // Step 3, move remote file to final remote destination
            moveChunksFileToFinalDestination(timeStamp, mFile.getLength());

            // Step 4, move local file to final local destination
            moveTemporalOriginalFiles(temporalFile, originalFile, expectedPath, expectedFile);

            return result;
        } catch (Exception e) {
            return new RemoteOperationResult(e);
        }
    }

    private RemoteOperationResult createChunksFolder(String remoteChunksFolder) {
        SyncOperation syncOperation = new CreateChunksFolderOperation(remoteChunksFolder);
        return syncOperation.execute(getClient(), getStorageManager());
    }

    private RemoteOperationResult moveChunksFileToFinalDestination(String fileLastModifTimestamp, long fileLength) {
        SyncOperation syncOperation = new MoveChunksFileOperation(
                String.valueOf(mTransferId + File.separator + FileUtils.FINAL_CHUNKS_FILE),
                mFile.getRemotePath(),
                fileLastModifTimestamp,
                fileLength
        );
        return syncOperation.execute(getClient(), getStorageManager());
    }
}