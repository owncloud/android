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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stores all information in order to check available offline files to be synchronized
 */
public class OCAvailableOfflineSync implements Parcelable {

    private long mId;

    // Timestamp (milliseconds) of last available offline synchronization
    private long mAvailableOfflineLastSync;

    /**
     * Main constructor
     *
     * @param availableOfflineLastSync
     */
    public OCAvailableOfflineSync(long availableOfflineLastSync) {

        if (availableOfflineLastSync < 0) {
            throw new IllegalArgumentException("Available offline last sync must be a positive long");
        }

        this.mAvailableOfflineLastSync = availableOfflineLastSync;
    }

    protected OCAvailableOfflineSync(Parcel source) {
        readFromParcel(source);
    }

    public static final Creator<OCAvailableOfflineSync> CREATOR = new Creator<OCAvailableOfflineSync>() {
        @Override
        public OCAvailableOfflineSync createFromParcel(Parcel in) {
            return new OCAvailableOfflineSync(in);
        }

        @Override
        public OCAvailableOfflineSync[] newArray(int size) {
            return new OCAvailableOfflineSync[size];
        }
    };

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mAvailableOfflineLastSync = source.readLong();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeLong(mAvailableOfflineLastSync);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getAvailableOfflineLastSync() {
        return mAvailableOfflineLastSync;
    }

    public void setAvailableOfflineLastSync(long avOfflineLastSync) {
        this.mAvailableOfflineLastSync = avOfflineLastSync;
    }
}