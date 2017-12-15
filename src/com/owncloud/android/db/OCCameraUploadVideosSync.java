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

package com.owncloud.android.db;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stores all information in order to check videos to upload when Camera uploads feature
 * is enabled.
 */
public class OCCameraUploadVideosSync implements Parcelable {

    private static final String TAG = OCUpload.class.getSimpleName();

    private long mId;

    // Unix timestamp (milliseconds) from which start videos synchronization
    private long mStartVideosSyncMs;

    // Unix timestamp (milliseconds) till which continue videos synchronization
    private long mFinishVideosSyncMs;

    /**
     * Main constructor
     * @param startVideosSyncMs
     */
    public OCCameraUploadVideosSync(long startVideosSyncMs) {

        if (startVideosSyncMs < 0) {
            throw new IllegalArgumentException("Videos start sync must be a positive long");
        }
        this.mStartVideosSyncMs = startVideosSyncMs;
    }

    protected OCCameraUploadVideosSync(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mStartVideosSyncMs = source.readLong();
        mFinishVideosSyncMs = source.readLong();
    }

    public static final Creator<OCCameraUploadVideosSync> CREATOR = new Creator<OCCameraUploadVideosSync>() {
        @Override
        public OCCameraUploadVideosSync createFromParcel(Parcel source) {
            return new OCCameraUploadVideosSync(source);
        }

        @Override
        public OCCameraUploadVideosSync[] newArray(int size) {
            return new OCCameraUploadVideosSync[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(mId);
        dest.writeLong(mStartVideosSyncMs);
        dest.writeLong(mFinishVideosSyncMs);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getStartVideosSyncMs() {
        return mStartVideosSyncMs;
    }

    public long getFinishVideosSyncMs() {
        return mFinishVideosSyncMs;
    }

    public void setStartVideosSyncMs(long startVideosSyncMs) {
        this.mStartVideosSyncMs = startVideosSyncMs;
    }

    public void setFinishVideosSyncMs(long finishVideosSyncMs) {
        this.mFinishVideosSyncMs = finishVideosSyncMs;
    }
}