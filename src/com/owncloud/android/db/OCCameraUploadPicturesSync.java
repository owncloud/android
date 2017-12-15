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
 * Stores all information in order to check pictures to upload when Camera uploads feature is enabled.
 */
public class OCCameraUploadPicturesSync implements Parcelable {

    private static final String TAG = OCUpload.class.getSimpleName();

    private long mId;

    // Unix timestamp (milliseconds) from which start pictures synchronization
    private long mStartPicturesSyncMs;

    // Unix timestamp (milliseconds) till which continue pictures synchronization
    private long mFinishPicturesSyncMs;

    /**
     * Main constructor
     * @param startPicturesSyncMs
     */
    public OCCameraUploadPicturesSync(long startPicturesSyncMs) {

        if (startPicturesSyncMs < 0) {
            throw new IllegalArgumentException("Pictures start sync must be a positive long");
        }
        this.mStartPicturesSyncMs = startPicturesSyncMs;
    }

    protected OCCameraUploadPicturesSync(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mStartPicturesSyncMs = source.readLong();
        mFinishPicturesSyncMs = source.readLong();
    }

    public static final Creator<OCCameraUploadPicturesSync> CREATOR = new Creator<OCCameraUploadPicturesSync>() {
        @Override
        public OCCameraUploadPicturesSync createFromParcel(Parcel source) {
            return new OCCameraUploadPicturesSync(source);
        }

        @Override
        public OCCameraUploadPicturesSync[] newArray(int size) {
            return new OCCameraUploadPicturesSync[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(mId);
        dest.writeLong(mStartPicturesSyncMs);
        dest.writeLong(mFinishPicturesSyncMs);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getStartPicturesSyncMs() {
        return mStartPicturesSyncMs;
    }

    public long getFinishPicturesSyncMs() {
        return mFinishPicturesSyncMs;
    }

    public void setStartPicturesSyncMs(long startPicturesSyncMs) {
        this.mStartPicturesSyncMs = startPicturesSyncMs;
    }

    public void setFinishPicturesSyncMs(long finishPicturesSyncMs) {
        this.mFinishPicturesSyncMs = finishPicturesSyncMs;
    }
}