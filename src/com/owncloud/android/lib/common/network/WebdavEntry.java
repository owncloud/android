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

package com.owncloud.android.lib.common.network;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;

import android.net.Uri;

import com.owncloud.android.lib.common.utils.Log_OC;

public class WebdavEntry {

    private static final String TAG = WebdavEntry.class.getSimpleName();

	public static final String NAMESPACE_OC = "http://owncloud.org/ns";
	public static final String EXTENDED_PROPERTY_NAME_PERMISSIONS = "permissions";
	public static final String EXTENDED_PROPERTY_NAME_REMOTE_ID = "id";
    public static final String EXTENDED_PROPERTY_NAME_SIZE = "size";

    public static final String PROPERTY_QUOTA_USED_BYTES = "quota-used-bytes";
    public static final String PROPERTY_QUOTA_AVAILABLE_BYTES = "quota-available-bytes";

    private static final int CODE_PROP_NOT_FOUND = 404;

	private String mName, mPath, mUri, mContentType, mEtag, mPermissions, mRemoteId;
	private long mContentLength, mCreateTimestamp, mModifiedTimestamp, mSize;
    private BigDecimal mQuotaUsedBytes, mQuotaAvailableBytes;

	public WebdavEntry(MultiStatusResponse ms, String splitElement) {
        resetData();
        if (ms.getStatus().length != 0) {
            mUri = ms.getHref();

            mPath = mUri.split(splitElement, 2)[1];

            int status = ms.getStatus()[0].getStatusCode();
            if ( status == CODE_PROP_NOT_FOUND ) {
                status = ms.getStatus()[1].getStatusCode();
            }
            DavPropertySet propSet = ms.getProperties(status);
            @SuppressWarnings("rawtypes")
            DavProperty prop = propSet.get(DavPropertyName.DISPLAYNAME);
            if (prop != null) {
                mName = (String) prop.getName().toString();
                mName = mName.substring(1, mName.length()-1);
            }
            else {
                String[] tmp = mPath.split("/");
                if (tmp.length > 0)
                    mName = tmp[tmp.length - 1];
            }

            // use unknown mimetype as default behavior
            // {DAV:}getcontenttype
            mContentType = "application/octet-stream";
            prop = propSet.get(DavPropertyName.GETCONTENTTYPE);
            if (prop != null) {
                mContentType = (String) prop.getValue();
                // dvelasco: some builds of ownCloud server 4.0.x added a trailing ';'
                // to the MIME type ; if looks fixed, but let's be cautious
                if (mContentType.indexOf(";") >= 0) {
                    mContentType = mContentType.substring(0, mContentType.indexOf(";"));
                }
            }
            
            // check if it's a folder in the standard way: see RFC2518 12.2 . RFC4918 14.3
            // {DAV:}resourcetype
            prop = propSet.get(DavPropertyName.RESOURCETYPE);
            if (prop!= null) {
                Object value = prop.getValue();
                if (value != null) {
                    mContentType = "DIR";   // a specific attribute would be better,
                                            // but this is enough;
                                            // unless while we have no reason to distinguish
                                            // MIME types for folders
                }
            }

            // {DAV:}getcontentlength
            prop = propSet.get(DavPropertyName.GETCONTENTLENGTH);
            if (prop != null)
                mContentLength = Long.parseLong((String) prop.getValue());

            // {DAV:}getlastmodified
            prop = propSet.get(DavPropertyName.GETLASTMODIFIED);
            if (prop != null) {
                Date d = WebdavUtils
                        .parseResponseDate((String) prop.getValue());
                mModifiedTimestamp = (d != null) ? d.getTime() : 0;
            }

            prop = propSet.get(DavPropertyName.CREATIONDATE);
            if (prop != null) {
                Date d = WebdavUtils
                        .parseResponseDate((String) prop.getValue());
                mCreateTimestamp = (d != null) ? d.getTime() : 0;
            }

            // {DAV:}getetag
            prop = propSet.get(DavPropertyName.GETETAG);
            if (prop != null) {
                mEtag = (String) prop.getValue();
                mEtag = WebdavUtils.parseEtag(mEtag);
            }

            // {DAV:}quota-used-bytes
            prop = propSet.get(DavPropertyName.create(PROPERTY_QUOTA_USED_BYTES));
            if (prop != null) {
                String quotaUsedBytesSt = (String) prop.getValue();
                try {
                    mQuotaUsedBytes = new BigDecimal(quotaUsedBytesSt);
                } catch (NumberFormatException e) {
                    Log_OC.w(TAG, "No value for QuotaUsedBytes - NumberFormatException");
                } catch (NullPointerException e ){
                    Log_OC.w(TAG, "No value for QuotaUsedBytes - NullPointerException");
                }
                Log_OC.d(TAG , "QUOTA_USED_BYTES " + quotaUsedBytesSt );
            }

            // {DAV:}quota-available-bytes
            prop = propSet.get(DavPropertyName.create(PROPERTY_QUOTA_AVAILABLE_BYTES));
            if (prop != null) {
                String quotaAvailableBytesSt = (String) prop.getValue();
                try {
                    mQuotaAvailableBytes = new BigDecimal(quotaAvailableBytesSt);
                } catch (NumberFormatException e) {
                    Log_OC.w(TAG, "No value for QuotaAvailableBytes - NumberFormatException");
                } catch (NullPointerException e ){
                    Log_OC.w(TAG, "No value for QuotaAvailableBytes");
                }
                Log_OC.d(TAG , "QUOTA_AVAILABLE_BYTES " + quotaAvailableBytesSt );
            }

            // OC permissions property <oc:permissions>
            prop = propSet.get(
            		EXTENDED_PROPERTY_NAME_PERMISSIONS, Namespace.getNamespace(NAMESPACE_OC)
    		);
            if (prop != null) {
                mPermissions = prop.getValue().toString();
            }

            // OC remote id property <oc:id>
            prop = propSet.get(
            		EXTENDED_PROPERTY_NAME_REMOTE_ID, Namespace.getNamespace(NAMESPACE_OC)
    		);
            if (prop != null) {
                mRemoteId = prop.getValue().toString();
            }

            // TODO: is it necessary?
            // OC size property <oc:size>
            prop = propSet.get(
            		EXTENDED_PROPERTY_NAME_SIZE, Namespace.getNamespace(NAMESPACE_OC)
    		);
            if (prop != null) {
                mSize = Long.parseLong((String) prop.getValue());
            }

        } else {
            Log_OC.e("WebdavEntry",
                    "General fuckup, no status for webdav response");
        }
    }

    public String path() {
        return mPath;
    }
    
    public String decodedPath() {
        return Uri.decode(mPath);
    }

    public String name() {
        return mName;
    }

    public boolean isDirectory() {
        return mContentType.equals("DIR");
    }

    public String contentType() {
        return mContentType;
    }

    public String uri() {
        return mUri;
    }

    public long contentLength() {
        return mContentLength;
    }

    public long createTimestamp() {
        return mCreateTimestamp;
    }

    public long modifiedTimestamp() {
        return mModifiedTimestamp;
    }
    
    public String etag() {
        return mEtag;
    }

    public String permissions() {
        return mPermissions;
    }

    public String remoteId() {
        return mRemoteId;
    }

    public long size(){
        return mSize;
    }

    public BigDecimal quotaUsedBytes() {
        return mQuotaUsedBytes;
    }

    public BigDecimal quotaAvailableBytes() {
        return mQuotaAvailableBytes;
    }

    private void resetData() {
        mName = mUri = mContentType = mPermissions = null; mRemoteId = null;
        mContentLength = mCreateTimestamp = mModifiedTimestamp = 0;
        mSize = 0;
        mQuotaUsedBytes = null;
        mQuotaAvailableBytes = null;
    }
}
