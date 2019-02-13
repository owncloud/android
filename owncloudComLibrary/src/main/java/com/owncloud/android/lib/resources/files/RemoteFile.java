/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import at.bitfire.dav4android.Property;
import at.bitfire.dav4android.Response;
import at.bitfire.dav4android.property.CreationDate;
import at.bitfire.dav4android.property.GetContentLength;
import at.bitfire.dav4android.property.GetContentType;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.dav4android.property.GetLastModified;
import at.bitfire.dav4android.property.QuotaAvailableBytes;
import at.bitfire.dav4android.property.QuotaUsedBytes;
import at.bitfire.dav4android.property.owncloud.OCId;
import at.bitfire.dav4android.property.owncloud.OCPermissions;
import at.bitfire.dav4android.property.owncloud.OCPrivatelink;
import at.bitfire.dav4android.property.owncloud.OCSize;
import okhttp3.HttpUrl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import static com.owncloud.android.lib.common.OwnCloudClient.WEBDAV_FILES_PATH_4_0;

/**
 * Contains the data of a Remote File from a WebDavEntry
 *
 * @author masensio
 * @author Christian Schabesberger
 */

public class RemoteFile implements Parcelable, Serializable {

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
     * Generated - should be refreshed every time the class changes!!
     */
    private static final long serialVersionUID = -8965995357413958539L;
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
    private String mPrivateLink;

    public RemoteFile() {
        resetData();
    }

    /**
     * Create new {@link RemoteFile} with given path.
     * <p>
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
        mCreationTimestamp = 0;
        mLength = 0;
        mMimeType = "DIR";
        mQuotaUsedBytes = BigDecimal.ZERO;
        mQuotaAvailableBytes = BigDecimal.ZERO;
        mPrivateLink = null;
    }

    public RemoteFile(final Response davResource, String userId) {
        this(getRemotePathFromUrl(davResource.getHref(), userId));
        final List<Property> properties = davResource.getProperties();

        for (Property property : properties) {
            if (property instanceof CreationDate) {
                this.setCreationTimestamp(
                        Long.parseLong(((CreationDate) property).getCreationDate()));
            }
            if (property instanceof GetContentLength) {
                this.setLength(((GetContentLength) property).getContentLength());
            }
            if (property instanceof GetContentType) {
                this.setMimeType(((GetContentType) property).getType());
            }
            if (property instanceof GetLastModified) {
                this.setModifiedTimestamp(((GetLastModified) property).getLastModified());
            }
            if (property instanceof GetETag) {
                this.setEtag(((GetETag) property).getETag());
            }
            if (property instanceof OCPermissions) {
                this.setPermissions(((OCPermissions) property).getPermission());
            }
            if (property instanceof OCId) {
                this.setRemoteId(((OCId) property).getId());
            }
            if (property instanceof OCSize) {
                this.setSize(((OCSize) property).getSize());
            }
            if (property instanceof QuotaUsedBytes) {
                this.setQuotaUsedBytes(
                        BigDecimal.valueOf(((QuotaUsedBytes) property).getQuotaUsedBytes()));
            }
            if (property instanceof QuotaAvailableBytes) {
                this.setQuotaAvailableBytes(
                        BigDecimal.valueOf(((QuotaAvailableBytes) property).getQuotaAvailableBytes()));
            }
            if (property instanceof OCPrivatelink) {
                this.setPrivateLink(((OCPrivatelink) property).getLink());
            }
        }
    }

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    protected RemoteFile(Parcel source) {
        readFromParcel(source);
    }

    /**
     * Retrieves a relative path from a remote file url
     * <p>
     * Example: url:port/remote.php/dav/files/username/Documents/text.txt => /Documents/text.txt
     *
     * @param url    remote file url
     * @param userId file owner
     * @return remote relative path of the file
     */
    private static String getRemotePathFromUrl(HttpUrl url, String userId) {
        final String davFilesPath = WEBDAV_FILES_PATH_4_0 + userId;
        final String absoluteDavPath = Uri.decode(url.encodedPath());
        final String pathToOc = absoluteDavPath.split(davFilesPath)[0];
        return absoluteDavPath.replace(pathToOc + davFilesPath, "");
    }

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

    public void setSize(long size) {
        mSize = size;
    }

    public void setQuotaUsedBytes(BigDecimal quotaUsedBytes) {
        mQuotaUsedBytes = quotaUsedBytes;
    }

    public void setQuotaAvailableBytes(BigDecimal quotaAvailableBytes) {
        mQuotaAvailableBytes = quotaAvailableBytes;
    }

    public String getPrivateLink() {
        return mPrivateLink;
    }

    public void setPrivateLink(String privateLink) {
        mPrivateLink = privateLink;
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
        mPrivateLink = null;
    }

    public void readFromParcel(Parcel source) {
        mRemotePath = source.readString();
        mMimeType = source.readString();
        mLength = source.readLong();
        mCreationTimestamp = source.readLong();
        mModifiedTimestamp = source.readLong();
        mEtag = source.readString();
        mPermissions = source.readString();
        mRemoteId = source.readString();
        mSize = source.readLong();
        mQuotaUsedBytes = (BigDecimal) source.readSerializable();
        mQuotaAvailableBytes = (BigDecimal) source.readSerializable();
        mPrivateLink = source.readString();
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
        dest.writeString(mPrivateLink);
    }
}