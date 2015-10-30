/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2012 Bartek Przybylski
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.xml.Namespace;

public class WebdavUtils {
    public static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat(
            "dd.MM.yyyy hh:mm");
    
    private static final SimpleDateFormat DATETIME_FORMATS[] = {
    	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
    	new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),	
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.US),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
		new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US),
		new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
		new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US),
		new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US)
	};

    public static Date parseResponseDate(String date) {
        Date returnDate = null;
        SimpleDateFormat format = null;
        for (int i = 0; i < DATETIME_FORMATS.length; ++i) {
            try {
            	format = DATETIME_FORMATS[i];
            	synchronized(format) {
            		returnDate = format.parse(date);
            	}
                return returnDate;
            } catch (ParseException e) {
            	// this is not the format
            }
        }
        return null;
    }

    /**
     * Encodes a path according to URI RFC 2396. 
     * 
     * If the received path doesn't start with "/", the method adds it.
     * 
     * @param remoteFilePath    Path
     * @return                  Encoded path according to RFC 2396, always starting with "/"
     */
    public static String encodePath(String remoteFilePath) {
        String encodedPath = Uri.encode(remoteFilePath, "/");
        if (!encodedPath.startsWith("/"))
            encodedPath = "/" + encodedPath;
        return encodedPath;
    }

    /**
     * Builds a DavPropertyNameSet with all prop
     * For using instead of DavConstants.PROPFIND_ALL_PROP
     * @return
     */
    public static DavPropertyNameSet getAllPropSet(){
        DavPropertyNameSet propSet = new DavPropertyNameSet();
        propSet.add(DavPropertyName.DISPLAYNAME);
        propSet.add(DavPropertyName.GETCONTENTTYPE);
        propSet.add(DavPropertyName.RESOURCETYPE);
        propSet.add(DavPropertyName.GETCONTENTLENGTH);
        propSet.add(DavPropertyName.GETLASTMODIFIED);
        propSet.add(DavPropertyName.CREATIONDATE);
        propSet.add(DavPropertyName.GETETAG);
        propSet.add(DavPropertyName.create(WebdavEntry.PROPERTY_QUOTA_USED_BYTES));
        propSet.add(DavPropertyName.create(WebdavEntry.PROPERTY_QUOTA_AVAILABLE_BYTES));
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_PERMISSIONS,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_REMOTE_ID,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_SIZE,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));

        return propSet;
    }

    /**
     * Builds a DavPropertyNameSet with properties for files
     * @return
     */
    public static DavPropertyNameSet getFilePropSet(){
        DavPropertyNameSet propSet = new DavPropertyNameSet();
        propSet.add(DavPropertyName.DISPLAYNAME);
        propSet.add(DavPropertyName.GETCONTENTTYPE);
        propSet.add(DavPropertyName.RESOURCETYPE);
        propSet.add(DavPropertyName.GETCONTENTLENGTH);
        propSet.add(DavPropertyName.GETLASTMODIFIED);
        propSet.add(DavPropertyName.CREATIONDATE);
        propSet.add(DavPropertyName.GETETAG);
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_PERMISSIONS,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_REMOTE_ID,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));
        propSet.add(WebdavEntry.EXTENDED_PROPERTY_NAME_SIZE,
                Namespace.getNamespace(WebdavEntry.NAMESPACE_OC));

        return propSet;
    }

    /**
     *
     * @param rawEtag
     * @return
     */
    public static String parseEtag(String rawEtag) {
        if (rawEtag == null || rawEtag.length() == 0) {
            return "";
        }
        if (rawEtag.endsWith("-gzip")) {
            rawEtag = rawEtag.substring(0, rawEtag.length() - 5);
        }
        if (rawEtag.length() >= 2 && rawEtag.startsWith("\"") && rawEtag.endsWith("\"")) {
            rawEtag = rawEtag.substring(1, rawEtag.length() - 1);
        }
        return rawEtag;
    }


    /**
     *
     * @param method
     * @return
     */
    public static String getEtagFromResponse(HttpMethod method) {
        Header eTag = method.getResponseHeader("OC-ETag");
        if (eTag == null) {
            eTag = method.getResponseHeader("oc-etag");
        }
        if (eTag == null) {
            eTag = method.getResponseHeader("ETag");
        }
        if (eTag == null) {
            eTag = method.getResponseHeader("etag");
        }
        String result = "";
        if (eTag != null) {
            result = parseEtag(eTag.getValue());
        }
        return result;
    }

}
