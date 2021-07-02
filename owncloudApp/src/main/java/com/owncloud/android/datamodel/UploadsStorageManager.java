/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
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
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import timber.log.Timber;
import com.owncloud.android.domain.files.model.OCFile;

import java.util.Calendar;
import java.util.Observable;

/**
 * Database helper for storing list of files to be uploaded, including status
 * information for each file.
 */
public class UploadsStorageManager extends Observable {

    private ContentResolver mContentResolver;

    public enum UploadStatus {

        /**
         * Upload currently in progress or scheduled to be executed.
         */
        UPLOAD_IN_PROGRESS(0),

        /**
         * Last upload failed.
         */
        UPLOAD_FAILED(1),

        /**
         * Upload was successful.
         */
        UPLOAD_SUCCEEDED(2);

        private final int value;

        UploadStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static UploadStatus fromValue(int value) {
            switch (value) {
                case 0:
                    return UPLOAD_IN_PROGRESS;
                case 1:
                    return UPLOAD_FAILED;
                case 2:
                    return UPLOAD_SUCCEEDED;
            }
            return null;
        }

    }

    public UploadsStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an upload object in DB.
     *
     * @param ocUpload Upload object to store
     * @return upload id, -1 if the insert process fails.
     */
    public long storeUpload(OCUpload ocUpload) {
        Timber.v("Inserting " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_FILE_SIZE, ocUpload.getFileSize());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR, ocUpload.getLocalAction());
        cv.put(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE, ocUpload.isForceOverwrite() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER, ocUpload.createsRemoteFolder() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_CREATED_BY, ocUpload.getCreatedBy());
        cv.put(ProviderTableMeta.UPLOADS_TRANSFER_ID, ocUpload.getTransferId());

        Uri result = getDB().insert(ProviderTableMeta.CONTENT_URI_UPLOADS, cv);

        Timber.d("storeUpload returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result == null) {
            Timber.e("Failed to insert item " + ocUpload.getLocalPath() + " into upload db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocUpload.setUploadId(new_id);
            notifyObserversNow();
            return new_id;
        }
    }

    /**
     * Update an upload object in DB.
     *
     * @param ocUpload Upload object with state to update
     * @return num of updated uploads.
     */
    public int updateUpload(OCUpload ocUpload) {
        Timber.v("Updating " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP, ocUpload.getUploadEndTimestamp());
        cv.put(ProviderTableMeta.UPLOADS_TRANSFER_ID, ocUpload.getTransferId());

        int result = getDB().update(ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocUpload.getUploadId())}
        );

        Timber.d("updateCameraUploadSync returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result != 1) {
            Timber.e("Failed to update item " + ocUpload.getLocalPath() + " into upload db.");
        } else {
            notifyObserversNow();
        }

        return result;
    }

    private int updateUploadInternal(Cursor c, UploadStatus status, UploadResult result, String remotePath,
                                     String localPath) {
        int r = 0;
        while (c.moveToNext()) {
            // read upload object and update
            OCUpload upload = createOCUploadFromCursor(c);

            String path = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            Timber.v("Updating " + path + " with status:" + status + " and result:" + (result == null ? "null" :
                    result.toString()) + " (old:" + upload.toFormattedString() + ")");

            upload.setUploadStatus(status);
            upload.setLastResult(result);
            upload.setRemotePath(remotePath);
            if (localPath != null) {
                upload.setLocalPath(localPath);
            }
            upload.setUploadEndTimestamp(Calendar.getInstance().getTimeInMillis());

            // store update upload object to db
            r = updateUpload(upload);
        }

        return r;
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id         upload id.
     * @param status     new status.
     * @param result     new result of upload operation
     * @param remotePath path of the file to upload in the ownCloud storage
     * @param localPath  path of the file to upload in the device storage
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(long id, UploadStatus status, UploadResult result, String remotePath,
                                  String localPath) {
        int returnValue = 0;
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (c.getCount() != 1) {
            Timber.e(c.getCount() + " items for id=" + id
                    + " available in UploadDb. Expected 1. Failed to update upload db.");
        } else {
            returnValue = updateUploadInternal(c, status, result, remotePath, localPath);
        }
        c.close();
        return returnValue;
    }

    /**
     * Should be called when some value of this DB was changed. All observers
     * are informed.
     */
    public void notifyObserversNow() {
        Timber.d("notifyObserversNow");
        setChanged();
        notifyObservers();
    }

    /**
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param upload Upload instance to remove from persisted storage.
     * @return true when the upload was stored and could be removed.
     */
    public int removeUpload(OCUpload upload) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta._ID + "=?",
                new String[]{Long.toString(upload.getUploadId())}
        );
        Timber.d("delete returns " + result + " for upload " + upload);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    /**
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param accountName Name of the OC account target of the upload to remove.
     * @param remotePath  Absolute path in the OC account target of the upload to remove.
     * @return true when one or more upload entries were removed
     */
    public int removeUpload(String accountName, String remotePath) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=? AND " + ProviderTableMeta.UPLOADS_REMOTE_PATH + "=?",
                new String[]{accountName, remotePath}
        );
        Timber.d("delete returns " + result + " for file " + remotePath + " in " + accountName);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    /**
     * Remove all the uploads of a given account from the uploads list.
     *
     * @param accountName Name of the OC account target of the uploads to remove.
     * @return true when one or more upload entries were removed
     */
    public int removeUploads(String accountName) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
                new String[]{accountName}
        );
        Timber.d("delete returns " + result + " for uploads in " + accountName);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public OCUpload[] getAllStoredUploads() {
        return getUploads(null, null, null);
    }

    public OCUpload getLastUploadFor(OCFile file, String accountName) {
        OCUpload[] uploads = getUploads(
                ProviderTableMeta.UPLOADS_REMOTE_PATH + "== ? AND " +
                        ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?",
                new String[]{
                        file.getRemotePath(),
                        accountName
                },
                ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + " desc"
        );
        return (uploads.length > 0 ? uploads[0] : null);
    }

    private OCUpload[] getUploads(String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                selection,
                selectionArgs,
                sortOrder
        );
        OCUpload[] list = new OCUpload[c.getCount()];
        if (c.moveToFirst()) {
            do {
                OCUpload upload = createOCUploadFromCursor(c);
                if (upload == null) {
                    Timber.e("OCUpload could not be created from cursor");
                } else {
                    list[c.getPosition()] = upload;
                }
            } while (c.moveToNext());
        }
        c.close();

        return list;
    }

    private OCUpload createOCUploadFromCursor(Cursor c) {
        OCUpload upload = null;
        if (c != null) {
            String localPath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            String remotePath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_REMOTE_PATH));
            String accountName = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_ACCOUNT_NAME));
            upload = new OCUpload(localPath, remotePath, accountName);

            upload.setFileSize(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_FILE_SIZE)));
            upload.setUploadId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            upload.setUploadStatus(
                    UploadStatus.fromValue(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_STATUS)))
            );
            upload.setLocalAction(c.getInt(c.getColumnIndex((ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR))));
            upload.setForceOverwrite(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE)) == 1);
            upload.setCreateRemoteFolder(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)) == 1);
            upload.setUploadEndTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP)));
            upload.setLastResult(UploadResult.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_LAST_RESULT))));
            upload.setCreatedBy(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_CREATED_BY)));
            upload.setTransferId(c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_TRANSFER_ID)));
        }
        return upload;
    }

    /**
     * Get all uploads which are currently being uploaded or waiting in the queue to be uploaded.
     */
    public OCUpload[] getCurrentAndPendingUploads() {

        return getUploads(
                ProviderTableMeta.UPLOADS_STATUS + "== ? OR " +
                        ProviderTableMeta.UPLOADS_LAST_RESULT + "== ?",
                new String[]{
                        String.valueOf(UploadStatus.UPLOAD_IN_PROGRESS.value),
                        String.valueOf(UploadResult.DELAYED_FOR_WIFI.getValue())
                },
                null
        );
    }

    /**
     * Get all failed uploads.
     */
    public OCUpload[] getFailedUploads() {
        return getUploads(
                ProviderTableMeta.UPLOADS_STATUS + "== ?",
                new String[]{
                        String.valueOf(UploadStatus.UPLOAD_FAILED.value)
                },
                null
        );
    }

    /**
     * Get all uploads which where successfully completed.
     */
    public OCUpload[] getFinishedUploads() {
        return getUploads(
                ProviderTableMeta.UPLOADS_STATUS + "== ?",
                new String[]{
                        String.valueOf(UploadStatus.UPLOAD_SUCCEEDED.value)
                },
                null
        );
    }

    /**
     * Get all failed uploads, except for those that were not performed due to lack of Wifi connection
     *
     * @return Array of failed uploads, except for those that were not performed due to lack of Wifi connection.
     */
    public OCUpload[] getFailedButNotDelayedForWifiUploads() {
        return getUploads(
                ProviderTableMeta.UPLOADS_STATUS + "== ?" + UploadStatus.UPLOAD_FAILED.value + " AND " +
                        ProviderTableMeta.UPLOADS_LAST_RESULT + "<> ?",
                new String[]{
                        String.valueOf(UploadStatus.UPLOAD_FAILED.value),
                        String.valueOf(UploadResult.DELAYED_FOR_WIFI.getValue())
                },
                null
        );
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }

    public long clearFailedButNotDelayedForWifiUploads() {
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=? AND " +
                        ProviderTableMeta.UPLOADS_LAST_RESULT + "!=?",
                new String[]{String.valueOf(UploadStatus.UPLOAD_FAILED.value),
                        String.valueOf(UploadResult.DELAYED_FOR_WIFI.getValue())}
        );
        Timber.d("delete all failed uploads but those delayed for Wifi");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public long clearSuccessfulUploads() {
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=?",
                new String[]{String.valueOf(UploadStatus.UPLOAD_SUCCEEDED.value)}
        );
        Timber.d("delete all successful uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    /**
     * Updates the persistent upload database with upload result.
     */
    public void updateDatabaseUploadResult(RemoteOperationResult uploadResult,
                                           UploadFileOperation uploadFileOperation) {
        // result: success or fail notification
        Timber.d("updateDataseUploadResult uploadResult: " + uploadResult + " upload: " + uploadFileOperation);

        if (uploadResult.isCancelled()) {
            removeUpload(
                    uploadFileOperation.getAccount().name,
                    uploadFileOperation.getRemotePath()
            );
        } else {
            String localPath = (FileUploader.LOCAL_BEHAVIOUR_MOVE == uploadFileOperation.getLocalBehaviour())
                    ? uploadFileOperation.getStoragePath() : null;

            if (uploadResult.isSuccess()) {
                updateUploadStatus(
                        uploadFileOperation.getOCUploadId(),
                        UploadStatus.UPLOAD_SUCCEEDED,
                        UploadResult.UPLOADED,
                        uploadFileOperation.getRemotePath(),
                        localPath
                );
            } else {
                updateUploadStatus(
                        uploadFileOperation.getOCUploadId(),
                        UploadStatus.UPLOAD_FAILED,
                        UploadResult.fromOperationResult(uploadResult),
                        uploadFileOperation.getRemotePath(),
                        localPath
                );
            }
        }
    }

    /**
     * Updates the persistent upload database with an upload now in progress.
     */
    public void updateDatabaseUploadStart(UploadFileOperation uploadFileOperation) {
        String localPath = (FileUploader.LOCAL_BEHAVIOUR_MOVE == uploadFileOperation.getLocalBehaviour())
                ? uploadFileOperation.getStoragePath() : null;

        updateUploadStatus(
                uploadFileOperation.getOCUploadId(),
                UploadStatus.UPLOAD_IN_PROGRESS,
                UploadResult.UNKNOWN,
                uploadFileOperation.getRemotePath(),
                localPath
        );
    }

    /**
     * Changes the status of any in progress upload from UploadStatus.UPLOAD_IN_PROGRESS
     * to UploadStatus.UPLOAD_FAILED
     *
     * @return Number of uploads which status was changed.
     */
    public int failInProgressUploads(UploadResult uploadResult) {
        Timber.v("Updating state of any killed upload");

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_STATUS, UploadStatus.UPLOAD_FAILED.getValue());
        cv.put(
                ProviderTableMeta.UPLOADS_LAST_RESULT,
                uploadResult != null ? uploadResult.getValue() : UploadResult.UNKNOWN.getValue()
        );
        cv.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        int result = getDB().update(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta.UPLOADS_STATUS + "=?",
                new String[]{String.valueOf(UploadStatus.UPLOAD_IN_PROGRESS.getValue())}
        );

        if (result == 0) {
            Timber.v("No upload was killed");
        } else {
            Timber.w("%s uploads where abruptly interrupted", result);
            notifyObserversNow();
        }
        return result;
    }
}
