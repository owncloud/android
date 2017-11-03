/**
 *  ownCloud Android client application
 *
 *  @author David González Verdugo
 *  Copyright (C) 2017 ownCloud GmbH.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.owncloud.android.db.OCCameraUploadSync;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Observable;

public class CameraUploadsSyncStorageManager extends Observable{

    private ContentResolver mContentResolver;

    static private final String TAG = CameraUploadsSyncStorageManager.class.getSimpleName();

    public CameraUploadsSyncStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an camera upload sync object in DB.
     *
     * @param ocCameraUploadSync      Camera upload sync object to store
     * @return camera upload sync id, -1 if the insert process fails.
     */
    public long storeCameraUploadSync(OCCameraUploadSync ocCameraUploadSync) {
        Log_OC.v(TAG, "Inserting camera upload sync with timestamp of last pictures synchronization "
                + ocCameraUploadSync.getPicturesLastSync() + " and timestamp of last videos " +
                "synchronzization" + ocCameraUploadSync.getVideosLastSync());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.PICTURES_LAST_SYNC_TIMESTAMP, ocCameraUploadSync.
                getPicturesLastSync());
        cv.put(ProviderMeta.ProviderTableMeta.VIDEOS_LAST_SYNC_TIMESTAMP, ocCameraUploadSync.
                getVideosLastSync());

        Uri result = getDB().insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS, cv);

        Log_OC.d(TAG, "storeUpload returns with: " + result + " for camera upload sync " +
                ocCameraUploadSync.getId());
        if (result == null) {
            Log_OC.e(TAG, "Failed to insert camera upload sync " + ocCameraUploadSync.getId()
                    + " into camera uploads sync db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocCameraUploadSync.setId(new_id);
            notifyObserversNow();
            return new_id;
        }
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }

    /**
     * Should be called when some value of this DB was changed. All observers
     * are informed.
     */
    public void notifyObserversNow() {
        Log_OC.d(TAG, "notifyObserversNow");
        setChanged();
        notifyObservers();
    }
}