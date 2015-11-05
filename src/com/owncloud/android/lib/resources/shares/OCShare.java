/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;


/**
 * Contains the data of a Share from the Share API
 * 
 * @author masensio
 *
 */
public class OCShare implements Parcelable, Serializable {
	
	/** Generated - should be refreshed every time the class changes!! */
	private static final long serialVersionUID = 4124975224281327921L;

	private static final String TAG = OCShare.class.getSimpleName();

    public static final int DEFAULT_PERMISSION = -1;

    private long mId;
    private long mFileSource;
    private long mItemSource;
    private ShareType mShareType;
    private String mShareWith;
    private String mPath;
    private int mPermissions;
    private long mSharedDate;
    private long mExpirationDate;
    private String mToken;
    private String mSharedWithDisplayName;
    private boolean mIsFolder;
    private long mUserId;
    private long mRemoteId;
    private String mShareLink;
    
    public OCShare() {
    	super();
    	resetData();
    }
    
	public OCShare(String path) {
		resetData();
        if (path == null || path.length() <= 0 || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            Log_OC.e(TAG, "Trying to create a OCShare with a non valid path");
            throw new IllegalArgumentException("Trying to create a OCShare with a non valid path: " + path);
        }
        mPath = path;
	}

	/**
     * Used internally. Reset all file properties
     */
    private void resetData() {
    	mId = -1;
        mFileSource = 0;
        mItemSource = 0;
        mShareType = ShareType.NO_SHARED; 
        mShareWith = "";
        mPath = "";
        mPermissions = -1;
        mSharedDate = 0;
        mExpirationDate = 0;
        mToken = "";
        mSharedWithDisplayName = "";
        mIsFolder = false;
        mUserId = -1;
        mRemoteId = -1;
        mShareLink = "";
    }	
    
    /// Getters and Setters
    
    public long getId() {
        return mId;
    }
    
    public void setId(long id){
        mId = id;
    }
    
    public long getFileSource() {
        return mFileSource;
    }

    public void setFileSource(long fileSource) {
        this.mFileSource = fileSource;
    }

    public long getItemSource() {
        return mItemSource;
    }

    public void setItemSource(long itemSource) {
        this.mItemSource = itemSource;
    }

    public ShareType getShareType() {
        return mShareType;
    }

    public void setShareType(ShareType shareType) {
        this.mShareType = shareType;
    }

    public String getShareWith() {
        return mShareWith;
    }

    public void setShareWith(String shareWith) {
        this.mShareWith = (shareWith != null) ? shareWith : "";
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = (path != null) ? path : "";
    }

    public int getPermissions() {
        return mPermissions;
    }

    public void setPermissions(int permissions) {
        this.mPermissions = permissions;
    }

    public long getSharedDate() {
        return mSharedDate;
    }

    public void setSharedDate(long sharedDate) {
        this.mSharedDate = sharedDate;
    }

    public long getExpirationDate() {
        return mExpirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.mExpirationDate = expirationDate;
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        this.mToken = (token != null) ? token : "";
    }

    public String getSharedWithDisplayName() {
        return mSharedWithDisplayName;
    }

    public void setSharedWithDisplayName(String sharedWithDisplayName) {
        this.mSharedWithDisplayName = (sharedWithDisplayName != null) ? sharedWithDisplayName : "";
    }

    public boolean isFolder() {
        return mIsFolder;
    }

    public void setIsFolder(boolean isFolder) {
        this.mIsFolder = isFolder;
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        this.mUserId = userId;
    }

    public long getRemoteId() {
        return mRemoteId;
    }

    public void setIdRemoteShared(long remoteId) {
        this.mRemoteId = remoteId;
    }
    
    public String getShareLink() {
    	return this.mShareLink;
    }
    
    public void setShareLink(String shareLink) {
        this.mShareLink = (shareLink != null) ? shareLink : "";
    }

    public boolean isPasswordProtected() {
        return ShareType.PUBLIC_LINK.equals(mShareType) && mShareWith.length() > 0;
    }
    
    /** 
     * Parcelable Methods
     */
    public static final Parcelable.Creator<OCShare> CREATOR = new Parcelable.Creator<OCShare>() {
        @Override
        public OCShare createFromParcel(Parcel source) {
            return new OCShare(source);
        }

        @Override
        public OCShare[] newArray(int size) {
            return new OCShare[size];
        }
    };
    
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */    
    protected OCShare(Parcel source) {
    	readFromParcel(source);
    }
    
    public void readFromParcel(Parcel source) {
        mId = source.readLong();
    	
        mFileSource = source.readLong();
        mItemSource = source.readLong();
        try {
            mShareType = ShareType.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mShareType = ShareType.NO_SHARED;
        }
        mShareWith = source.readString();
        mPath = source.readString();
        mPermissions = source.readInt();
        mSharedDate = source.readLong();
        mExpirationDate = source.readLong();
        mToken = source.readString();
        mSharedWithDisplayName = source.readString();
        mIsFolder = source.readInt() == 0;
        mUserId = source.readLong();
        mRemoteId = source.readLong();
        mShareLink = source.readString();
    }


	@Override
	public int describeContents() {
		return this.hashCode();
	}
	
	
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeLong(mId);
        dest.writeLong(mFileSource);
        dest.writeLong(mItemSource);
        dest.writeString((mShareType == null) ? "" : mShareType.name());
        dest.writeString(mShareWith);
        dest.writeString(mPath);
        dest.writeInt(mPermissions);
        dest.writeLong(mSharedDate);
        dest.writeLong(mExpirationDate);
        dest.writeString(mToken);
        dest.writeString(mSharedWithDisplayName);
        dest.writeInt(mIsFolder ? 1 : 0);
        dest.writeLong(mUserId);
        dest.writeLong(mRemoteId);
        dest.writeString(mShareLink);
    }

}
