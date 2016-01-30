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

package com.owncloud.android.lib.resources.files;

import java.io.Serializable;
import java.math.BigDecimal;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.lib.common.network.WebdavEntry;

/**
 *  Contains the data of a Remote File from a WebDavEntry
 * 
 *  @author masensio
 */

public class RemoteFile implements Parcelable, Serializable {

    /** Generated - should be refreshed every time the class changes!! */
    private static final long serialVersionUID = 3130865437811248451L;

	private String mRemotePath;
	private String mMimeType;
	private long mLength;
	private long mCreationTimestamp;
	private long mModifiedTimestamp;
	private String mEtag;
	private String mPermissions;
	private String mRemoteId;
    private long mSize;
    private BigDecimal mQuotaUsedBytes;
    private BigDecimal mQuotaAvailableBytes;

	/** 
	 * Getters and Setters
	 */
	
    public String getRemotePath() {
		return mRemotePath;
	}

	public void setRemotePath(String remotePath) {
		this.mRemotePath = remotePath;
	}

	public String getMimeType() {
		return mMimeType;
	}

	public void setMimeType(String mimeType) {
		this.mMimeType = mimeType;
	}

	public long getLength() {
		return mLength;
	}

	public void setLength(long length) {
		this.mLength = length;
	}

	public long getCreationTimestamp() {
		return mCreationTimestamp;
	}

	public void setCreationTimestamp(long creationTimestamp) {
		this.mCreationTimestamp = creationTimestamp;
	}

	public long getModifiedTimestamp() {
		return mModifiedTimestamp;
	}

	public void setModifiedTimestamp(long modifiedTimestamp) {
		this.mModifiedTimestamp = modifiedTimestamp;
	}

	public String getEtag() {
		return mEtag;
	}

	public void setEtag(String etag) {
		this.mEtag = etag;
	}
	
	public String getPermissions() {
		return mPermissions;
	}

	public void setPermissions(String permissions) {
		this.mPermissions = permissions;
	}

	public String getRemoteId() {
		return mRemoteId;
	}

	public void setRemoteId(String remoteId) {
		this.mRemoteId = remoteId;
	}

    public long getSize() {
        return mSize;
    }

    public void setSize (long size){
        mSize = size;
    }

    public void setQuotaUsedBytes (BigDecimal quotaUsedBytes) {
        mQuotaUsedBytes = quotaUsedBytes;
    }

    public void setQuotaAvailableBytes (BigDecimal quotaAvailableBytes) {
        mQuotaAvailableBytes = quotaAvailableBytes;
    }

	public RemoteFile() {
		resetData();
	}

	/**
     * Create new {@link RemoteFile} with given path.
     * 
     * The path received must be URL-decoded. Path separator must be OCFile.PATH_SEPARATOR, and it must be the first character in 'path'.
     * 
     * @param path The remote path of the file.
     */
	public RemoteFile(String path) {
		resetData();
        if (path == null || path.length() <= 0 || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Trying to create a OCFile with a non valid remote path: " + path);
        }
        mRemotePath = path;
	}
	
	public RemoteFile(WebdavEntry we) {
        this(we.decodedPath());
        this.setCreationTimestamp(we.createTimestamp());
        this.setLength(we.contentLength());
        this.setMimeType(we.contentType());
        this.setModifiedTimestamp(we.modifiedTimestamp());
        this.setEtag(we.etag());
        this.setPermissions(we.permissions());
        this.setRemoteId(we.remoteId());
        this.setSize(we.size());
        this.setQuotaUsedBytes(we.quotaUsedBytes());
        this.setQuotaAvailableBytes(we.quotaAvailableBytes());
	}

	/**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        mRemotePath = null;
        mMimeType = null;
        mLength = 0;
        mCreationTimestamp = 0;
        mModifiedTimestamp = 0;
        mEtag = null;
        mPermissions = null;
        mRemoteId = null;
        mSize = 0;
        mQuotaUsedBytes = null;
        mQuotaAvailableBytes = null;
    }

    /** 
     * Parcelable Methods
     */
    public static final Parcelable.Creator<RemoteFile> CREATOR = new Parcelable.Creator<RemoteFile>() {
        @Override
        public RemoteFile createFromParcel(Parcel source) {
            return new RemoteFile(source);
        }

        @Override
        public RemoteFile[] newArray(int size) {
            return new RemoteFile[size];
        }
    };
    
    
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */
    protected RemoteFile(Parcel source) {
    	readFromParcel(source);
    }
    
    public void readFromParcel (Parcel source) {
        mRemotePath = source.readString();
        mMimeType = source.readString();
        mLength = source.readLong();
        mCreationTimestamp = source.readLong();
        mModifiedTimestamp = source.readLong();
        mEtag = source.readString();
        mPermissions= source.readString();
        mRemoteId = source.readString();
        mSize = source.readLong();
        mQuotaUsedBytes = (BigDecimal) source.readSerializable();
        mQuotaAvailableBytes = (BigDecimal) source.readSerializable();
    }
    
	@Override
	public int describeContents() {
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mRemotePath);
		dest.writeString(mMimeType);    
		dest.writeLong(mLength);
		dest.writeLong(mCreationTimestamp);
		dest.writeLong(mModifiedTimestamp);
		dest.writeString(mEtag);
		dest.writeString(mPermissions);
		dest.writeString(mRemoteId);
        dest.writeLong(mSize);
        dest.writeSerializable(mQuotaUsedBytes);
        dest.writeSerializable(mQuotaAvailableBytes);
	}

}
