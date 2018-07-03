/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ChunkedUploadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.operations.common.SyncOperation;

import java.io.File;
import java.util.Iterator;

public class ChunkedUploadFileOperation extends UploadFileOperation {

    public ChunkedUploadFileOperation(Account account, OCFile file, OCUpload upload, boolean forceOverwrite,
                                      int localBehaviour, Context context) {
        super(account, file, upload, forceOverwrite, localBehaviour, context);
    }

    @Override
    protected RemoteOperationResult uploadRemoteFile(OwnCloudClient client, File temporalFile, File originalFile,
                                                     String expectedPath, File expectedFile, String timeStamp) {
        RemoteOperationResult result = null;

        try {
            // Step 1, create folder where we put the uploaded file chunks
            result = createChunksFolder(String.valueOf(mOCUploadId));

            if (!result.isSuccess()) return result;

            // Step 2, start to upload chunks
            mUploadOperation = new ChunkedUploadRemoteFileOperation(mOCUploadId, mFile.getStoragePath(),
                    mFile.getRemotePath(), mFile.getMimetype(), mFile.getEtagInConflict(), timeStamp);

            Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
            while (listener.hasNext()) {
                mUploadOperation.addDatatransferProgressListener(listener.next());
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

            result = mUploadOperation.execute(client);

            // File chunks not properly uploaded
            if (!result.isSuccess()) return result;

            // Step 3, move remote file to final remote destination
            moveChunksFileToFinalDestination(timeStamp, mFile.getFileLength());

            // Step 4, move local file to final local destination
            moveTemporalOriginalFiles(temporalFile, originalFile, expectedPath, expectedFile);

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
        }

        return result;
    }

    private RemoteOperationResult createChunksFolder(String remoteChunksFolder) {
        SyncOperation syncOperation = new CreateChunksFolderOperation(remoteChunksFolder);
        return syncOperation.execute(getClient(), getStorageManager());
    }

    private RemoteOperationResult moveChunksFileToFinalDestination(String fileLastModifTimestamp, long fileLength) {
        SyncOperation syncOperation = new MoveChunksFileOperation(
                String.valueOf(mOCUploadId + FileUtils.PATH_SEPARATOR + FileUtils.FINAL_CHUNKS_FILE),
                mFile.getRemotePath(),
                fileLastModifTimestamp,
                fileLength
        );
        return syncOperation.execute(getClient(), getStorageManager());
    }
}