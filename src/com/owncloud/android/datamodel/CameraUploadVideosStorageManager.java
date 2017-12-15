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

import com.owncloud.android.db.OCCameraUploadVideosSync;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

public class CameraUploadVideosStorageManager {

    private ContentResolver mContentResolver;

    static private final String TAG = CameraUploadVideosStorageManager.class.getSimpleName();

    public CameraUploadVideosStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an camera upload sync object in DB
     *
     * @param ocCameraUploadVideosSync Camera upload sync object to store
     * @return camera upload sync id, -1 if the insert process fails
     */
    public long storeCameraUploadVideosSync(OCCameraUploadVideosSync ocCameraUploadVideosSync) {
        Log_OC.v(TAG, "Inserting camera upload sync with timestamp from which start videos " +
                "synchronization" + ocCameraUploadVideosSync.getStartVideosSyncMs());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_VIDEOS_SYNC_MS, ocCameraUploadVideosSync.getStartVideosSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS, ocCameraUploadVideosSync.getFinishVideosSyncMs());

        Uri result = getDB().insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_VIDEOS_SYNC,
                cv);

        Log_OC.d(TAG, "storeUpload returns with: " + result + " for camera upload videos sync " +
                ocCameraUploadVideosSync.getId());

        if (result == null) {
            Log_OC.e(TAG, "Failed to insert camera upload videos sync " + ocCameraUploadVideosSync.getId()
                    + " into camera uploads sync db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocCameraUploadVideosSync.setId(new_id);
            return new_id;
        }
    }

    /**
     * Update a camera upload videos sync object in DB.
     *
     * @param ocCameraUploadVideosSync Camera upload videos sync object with state to update
     * @return num of updated camera upload videos sync
     */
    public int updateCameraUploadVideosSync(OCCameraUploadVideosSync ocCameraUploadVideosSync) {
        Log_OC.v(TAG, "Updating " + ocCameraUploadVideosSync.getId());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.START_VIDEOS_SYNC_MS, ocCameraUploadVideosSync.getStartVideosSyncMs());
        cv.put(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS, ocCameraUploadVideosSync.getFinishVideosSyncMs());

        int result = getDB().update(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_VIDEOS_SYNC,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocCameraUploadVideosSync.getId())}
        );

        Log_OC.d(TAG, "updateCameraUploadPicturesSync returns with: " + result + " for camera upload videos " +
                "sync: " + ocCameraUploadVideosSync.getId());
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + ocCameraUploadVideosSync.getId() + " into " +
                    "camera upload videos sync db.");
        }

        return result;
    }

    /**
     * Retrieves a camera upload videos sync object from DB
     * @param selection filter declaring which rows to return, formatted as an SQL WHERE clause
     * @param selectionArgs include ?s in selection, which will be replaced by the values from here
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause
     * @return camera upload sync object
     */
    public OCCameraUploadVideosSync getCameraUploadVideosSync(String selection, String[] selectionArgs,
                                                              String sortOrder) {
        Cursor c = getDB().query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_VIDEOS_SYNC,
                null,
                selection,
                selectionArgs,
                sortOrder
        );

        OCCameraUploadVideosSync ocCameraUploadVideosSync = null;

        if (c.moveToFirst()) {
            ocCameraUploadVideosSync = createOCCameraUploadVideosSyncFromCursor(c);
            if (ocCameraUploadVideosSync == null) {
                Log_OC.e(TAG, "Camera upload sync could not be created from cursor");
            }
        }

        c.close();

        return ocCameraUploadVideosSync;
    }

    private OCCameraUploadVideosSync createOCCameraUploadVideosSyncFromCursor(Cursor c) {
        OCCameraUploadVideosSync ocCameraUploadVideosSync = null;
        if (c != null) {
            long startVIdeosSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    START_VIDEOS_SYNC_MS));
            long finishVideosSyncMs = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.FINISH_VIDEOS_SYNC_MS));

            ocCameraUploadVideosSync = new OCCameraUploadVideosSync(startVIdeosSyncMs);

            ocCameraUploadVideosSync.setFinishVideosSyncMs(finishVideosSyncMs);

            ocCameraUploadVideosSync.setId(c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta._ID)));
        }
        return ocCameraUploadVideosSync;
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }
}