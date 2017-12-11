/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.OCCameraUploadSync;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

public class CameraUploadsSyncStorageManager {

    private ContentResolver mContentResolver;

    static private final String TAG = CameraUploadsSyncStorageManager.class.getSimpleName();

    public CameraUploadsSyncStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an camera upload sync object in DB
     *
     * @param ocCameraUploadSync Camera upload sync object to store
     * @return camera upload sync id, -1 if the insert process fails
     */
    public long storeCameraUploadSync(OCCameraUploadSync ocCameraUploadSync) {
        Log_OC.v(TAG, "Inserting camera upload sync with timestamp from which start pictures synchronization "
                + ocCameraUploadSync.getStartPicturesSyncMs() + " and timestamp from which start videos " +
                "synchronization" + ocCameraUploadSync.getStartVideosSyncMs());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_PICTURES_SYNC_MS, ocCameraUploadSync.getStartPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.START_VIDEOS_SYNC_MS, ocCameraUploadSync.getStartVideosSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_PICTURES_SYNC_MS, ocCameraUploadSync.getFinishPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS, ocCameraUploadSync.getFinishVideosSyncMs());

        Uri result = getDB().insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_SYNC,
                cv);

        Log_OC.d(TAG, "storeUpload returns with: " + result + " for camera upload sync " +
                ocCameraUploadSync.getId());

        if (result == null) {
            Log_OC.e(TAG, "Failed to insert camera upload sync " + ocCameraUploadSync.getId()
                    + " into camera uploads sync db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocCameraUploadSync.setId(new_id);
            return new_id;
        }
    }

    /**
     * Update a camera upload sync object in DB.
     *
     * @param ocCameraUploadSync Camera upload sync object with state to update
     * @return num of updated camera upload sync
     */
    public int updateCameraUploadSync(OCCameraUploadSync ocCameraUploadSync) {
        Log_OC.v(TAG, "Updating " + ocCameraUploadSync.getId());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_PICTURES_SYNC_MS, ocCameraUploadSync.getStartPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.START_VIDEOS_SYNC_MS, ocCameraUploadSync.getStartVideosSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_PICTURES_SYNC_MS, ocCameraUploadSync.getFinishPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS, ocCameraUploadSync.getFinishVideosSyncMs());

        int result = getDB().update(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_SYNC,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocCameraUploadSync.getId())}
        );

        Log_OC.d(TAG, "updateCameraUploadSync returns with: " + result + " for camera upload sync: " +
                ocCameraUploadSync.getId());
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + ocCameraUploadSync.getId() + " into " +
                    "camera upload sync db.");
        }

        return result;
    }

    /**
     * Retrieves a camera upload sync object from DB
     * @param selection filter declaring which rows to return, formatted as an SQL WHERE clause
     * @param selectionArgs include ?s in selection, which will be replaced by the values from here
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause
     * @return camera upload sync object
     */
    public OCCameraUploadSync getCameraUploadSync(String selection, String[] selectionArgs,
                                                  String sortOrder) {
        Cursor c = getDB().query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_SYNC,
                null,
                selection,
                selectionArgs,
                sortOrder
        );

        OCCameraUploadSync ocCameraUploadSync = null;

        if (c.moveToFirst()) {
            ocCameraUploadSync = createOCCameraUploadSyncFromCursor(c);
            if (ocCameraUploadSync == null) {
                Log_OC.e(TAG, "Camera upload sync could not be created from cursor");
            }
        }

        c.close();

        return ocCameraUploadSync;
    }

    private OCCameraUploadSync createOCCameraUploadSyncFromCursor(Cursor c) {
        OCCameraUploadSync cameraUploadSync = null;
        if (c != null) {
            long startPicturesSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    START_PICTURES_SYNC_MS));
            long startVIdeosSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    START_VIDEOS_SYNC_MS));
            long finishPicturesSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    FINISH_PICTURES_SYNC_MS));
            long finishVideosSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS));

            cameraUploadSync = new OCCameraUploadSync(startPicturesSyncMs, startVIdeosSyncMs);

            cameraUploadSync.setFinishPicturesSyncMs(finishPicturesSyncMs);
            cameraUploadSync.setFinishVideosSyncMs(finishVideosSyncMs);

            cameraUploadSync.setId(c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta._ID)));
        }
        return cameraUploadSync;
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }
}