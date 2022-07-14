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
import timber.log.Timber;
import com.owncloud.android.domain.files.model.OCFile;

import java.util.Calendar;
import java.util.Observable;

import static com.owncloud.android.extensions.CursorExtKt.getIntFromColumnOrThrow;
import static com.owncloud.android.extensions.CursorExtKt.getLongFromColumnOrThrow;
import static com.owncloud.android.extensions.CursorExtKt.getStringFromColumnOrThrow;

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

    /**
     * Should be called when some value of this DB was changed. All observers
     * are informed.
     */
    public void notifyObserversNow() {
        Timber.d("notifyObserversNow");
        setChanged();
        notifyObservers();
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
            String localPath = getStringFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_LOCAL_PATH);
            String remotePath = getStringFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_REMOTE_PATH);
            String accountName = getStringFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_ACCOUNT_NAME);
            upload = new OCUpload(localPath, remotePath, accountName);

            upload.setFileSize(getLongFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_FILE_SIZE));
            upload.setUploadId(getLongFromColumnOrThrow(c, ProviderTableMeta._ID));
            upload.setUploadStatus(UploadStatus.fromValue(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_STATUS)));
            upload.setLocalAction(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR));
            upload.setForceOverwrite(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_FORCE_OVERWRITE) == 1);
            upload.setCreateRemoteFolder(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER) == 1);
            upload.setUploadEndTimestamp(getLongFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP));
            upload.setLastResult(UploadResult.fromValue(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_LAST_RESULT)));
            upload.setCreatedBy(getIntFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_CREATED_BY));
            upload.setTransferId(getStringFromColumnOrThrow(c, ProviderTableMeta.UPLOADS_TRANSFER_ID));
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
}
