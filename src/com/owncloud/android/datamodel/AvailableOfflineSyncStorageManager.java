/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
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

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Observable;

public class AvailableOfflineSyncStorageManager extends Observable {
    private ContentResolver mContentResolver;

    static private final String TAG = AvailableOfflineSyncStorageManager.class.getSimpleName();

    public AvailableOfflineSyncStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an available offline sync object in DB
     *
     * @param ocAvailableOfflineSync      Available offline sync object to store
     * @return available offline sync id, -1 if the insert process fails
     */
    public long storeAvailableOfflineSync(OCAvailableOfflineSync ocAvailableOfflineSync) {
        Log_OC.v(TAG, "Inserting available offline sync with timestamp of last synchronization " +
                ocAvailableOfflineSync.getAvailableOfflineLastSync());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.AVAILABLE_OFFLINE_LAST_SYNC_TIMESTAMP, ocAvailableOfflineSync.
                getAvailableOfflineLastSync());

        Uri result = getDB().insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_AVAILABLE_OFFLINE_SYNC, cv);

        Log_OC.d(TAG, "Store available offline returns with: " + result + " for available offline sync " +
                ocAvailableOfflineSync.getId());

        if (result == null) {
            Log_OC.e(TAG, "Failed to insert available offline sync " + ocAvailableOfflineSync.getId()
                    + " into available offline sync db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocAvailableOfflineSync.setId(new_id);
            notifyObserversNow();
            return new_id;
        }
    }

    /**
     * Update an available offline sync object in DB.
     *
     * @param ocAvailableOfflineSync Available offline sync object with state to update
     * @return num of updated available offline sync
     */
    public int updateAvailableOfflineSync(OCAvailableOfflineSync ocAvailableOfflineSync) {
        Log_OC.v(TAG, "Updating " + ocAvailableOfflineSync.getId());

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.AVAILABLE_OFFLINE_LAST_SYNC_TIMESTAMP, ocAvailableOfflineSync.
                getAvailableOfflineLastSync());

        int result = getDB().update(ProviderMeta.ProviderTableMeta.CONTENT_URI_AVAILABLE_OFFLINE_SYNC,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocAvailableOfflineSync.getId())}
        );

        Log_OC.d(TAG, "updateAvailableOfflineSync returns with: " + result + " for available offline sync: " +
                ocAvailableOfflineSync.getId());
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + ocAvailableOfflineSync.getId() + " into " +
                    "available offline sync db.");
        } else {
            notifyObserversNow();
        }

        return result;
    }

    /**
     * Retrieves an available offline sync object from DB
     * @param selection filter declaring which rows to return, formatted as an SQL WHERE clause
     * @param selectionArgs include ?s in selection, which will be replaced by the values from here
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause
     * @return available offline sync object
     */
    public OCAvailableOfflineSync getAvailableOfflineSync(String selection, String[] selectionArgs,
                                                          String sortOrder) {
        Cursor c = getDB().query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_AVAILABLE_OFFLINE_SYNC,
                null,
                selection,
                selectionArgs,
                sortOrder
        );

        OCAvailableOfflineSync ocAvailableOfflineSync = null;

        if (c.moveToFirst()) {
            ocAvailableOfflineSync = createOCAvailableOfflineSyncFromCursor(c);
            if (ocAvailableOfflineSync == null) {
                Log_OC.e(TAG, "Available offline sync could not be created from cursor");
            }
        }

        c.close();

        return ocAvailableOfflineSync;
    }

    private OCAvailableOfflineSync createOCAvailableOfflineSyncFromCursor(Cursor c) {
        OCAvailableOfflineSync ocAvailableOfflineSync = null;
        if (c != null) {
            long availableOfflineLastSync = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta.
                    AVAILABLE_OFFLINE_LAST_SYNC_TIMESTAMP));

            ocAvailableOfflineSync = new OCAvailableOfflineSync(availableOfflineLastSync);

            ocAvailableOfflineSync.setId(c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta._ID)));
        }
        return ocAvailableOfflineSync;
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