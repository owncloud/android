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

import com.owncloud.android.db.OCCameraUploadPicturesSync;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

public class CameraUploadPicturesStorageManager {

    private ContentResolver mContentResolver;

    static private final String TAG = CameraUploadPicturesStorageManager.class.getSimpleName();

    public CameraUploadPicturesStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an camera upload pictures sync object in DB
     *
     * @param ocCameraUploadPicturesSync Camera upload pictures sync object to store
     * @return camera upload pictures sync id, -1 if the insert process fails
     */
    public long storeCameraUploadPicturesSync(OCCameraUploadPicturesSync ocCameraUploadPicturesSync) {
        Log_OC.v(TAG, "Inserting camera upload pictures sync with timestamp from which start pictures " +
                "synchronization " + ocCameraUploadPicturesSync.getStartPicturesSyncMs());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_PICTURES_SYNC_MS, ocCameraUploadPicturesSync.getStartPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_PICTURES_SYNC_MS, ocCameraUploadPicturesSync.getFinishPicturesSyncMs());

        Uri result = getDB().insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_PICTURES_SYNC,
                cv);

        Log_OC.d(TAG, "storeUpload returns with: " + result + " for camera upload pictures sync " +
                ocCameraUploadPicturesSync.getId());

        if (result == null) {
            Log_OC.e(TAG, "Failed to insert camera upload pictures sync " + ocCameraUploadPicturesSync.getId()
                    + " into camera uploads pictures sync db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocCameraUploadPicturesSync.setId(new_id);
            return new_id;
        }
    }

    /**
     * Update a camera upload pictures sync object in DB.
     *
     * @param ocCameraUploadPicturesSync Camera upload pictures sync object with state to update
     * @return num of updated camera upload pictures sync
     */
    public int updateCameraUploadPicturesSync(OCCameraUploadPicturesSync ocCameraUploadPicturesSync) {
        Log_OC.v(TAG, "Updating " + ocCameraUploadPicturesSync.getId());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_PICTURES_SYNC_MS, ocCameraUploadPicturesSync.
                getStartPicturesSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_PICTURES_SYNC_MS, ocCameraUploadPicturesSync.
                getFinishPicturesSyncMs());

        int result = getDB().update(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_PICTURES_SYNC,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocCameraUploadPicturesSync.getId())}
        );

        Log_OC.d(TAG, "updateCameraUploadPicturesSync returns with: " + result + " for camera upload pictures" +
                " sync: " + ocCameraUploadPicturesSync.getId());
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + ocCameraUploadPicturesSync.getId() + " into " +
                    "camera upload pictures sync db.");
        }

        return result;
    }

    /**
     * Retrieves a camera upload pictures sync object from DB
     * @param selection filter declaring which rows to return, formatted as an SQL WHERE clause
     * @param selectionArgs include ?s in selection, which will be replaced by the values from here
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause
     * @return camera upload sync object
     */
    public OCCameraUploadPicturesSync getCameraUploadPicturesSync(String selection, String[] selectionArgs,
                                                          String sortOrder) {
        Cursor c = getDB().query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_PICTURES_SYNC,
                null,
                selection,
                selectionArgs,
                sortOrder
        );

        OCCameraUploadPicturesSync ocCameraUploadPicturesSync = null;

        if (c.moveToFirst()) {
            ocCameraUploadPicturesSync = createOCCameraUploadPicturesSyncFromCursor(c);
            if (ocCameraUploadPicturesSync == null) {
                Log_OC.e(TAG, "Camera upload pictures sync could not be created from cursor");
            }
        }

        c.close();

        return ocCameraUploadPicturesSync;
    }

    private OCCameraUploadPicturesSync createOCCameraUploadPicturesSyncFromCursor(Cursor c) {
        OCCameraUploadPicturesSync ocCameraUploadPicturesSync = null;
        if (c != null) {
            long startPicturesSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    START_PICTURES_SYNC_MS));
            long finishPicturesSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    FINISH_PICTURES_SYNC_MS));

            ocCameraUploadPicturesSync = new OCCameraUploadPicturesSync(startPicturesSyncMs);

            ocCameraUploadPicturesSync.setId(c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta._ID)));

            ocCameraUploadPicturesSync.setFinishPicturesSyncMs(finishPicturesSyncMs);
        }
        return ocCameraUploadPicturesSync;
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }
}