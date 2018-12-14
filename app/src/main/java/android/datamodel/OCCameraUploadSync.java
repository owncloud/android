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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stores all information in order to check pictures or videos to upload when Camera uploads feature
 * is enabled.
 */
public class OCCameraUploadSync implements Parcelable {

    private long mId;

    // Timestamp (milliseconds) of last pictures synchronization
    private long mPicturesLastSync;

    // Timestamp (milliseconds) of last videos synchronization
    private long mVideosLastSync;

    /**
     * Main constructor
     *
     * @param picturesLastSync
     * @param videosLastSync
     */
    public OCCameraUploadSync(long picturesLastSync, long videosLastSync) {

        if (picturesLastSync < 0) {
            throw new IllegalArgumentException("Pictures last sync must be a positive long");
        }

        if (videosLastSync < 0) {
            throw new IllegalArgumentException("Videos last sync must be a positive long");

        }

        this.mPicturesLastSync = picturesLastSync;
        this.mVideosLastSync = videosLastSync;
    }

    protected OCCameraUploadSync(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mPicturesLastSync = source.readLong();
        mVideosLastSync = source.readLong();
    }

    public static final Creator<OCCameraUploadSync> CREATOR = new Creator<OCCameraUploadSync>() {
        @Override
        public OCCameraUploadSync createFromParcel(Parcel source) {
            return new OCCameraUploadSync(source);
        }

        @Override
        public OCCameraUploadSync[] newArray(int size) {
            return new OCCameraUploadSync[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeLong(mPicturesLastSync);
        dest.writeLong(mVideosLastSync);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getPicturesLastSync() {
        return mPicturesLastSync;
    }

    public void setPicturesLastSync(long picturesLastSync) {
        this.mPicturesLastSync = picturesLastSync;
    }

    public long getVideosLastSync() {
        return mVideosLastSync;
    }

    public void setVideosLastSync(long videosLastSync) {
        this.mVideosLastSync = videosLastSync;
    }
}