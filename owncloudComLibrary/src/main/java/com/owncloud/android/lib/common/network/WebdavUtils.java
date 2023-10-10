/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
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

import android.net.Uri;

import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebdavUtils {

    private static final SimpleDateFormat[] DATETIME_FORMATS = {
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
        Date returnDate;
        SimpleDateFormat format;
        for (SimpleDateFormat datetimeFormat : DATETIME_FORMATS) {
            try {
                format = datetimeFormat;
                synchronized (format) {
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
     * <p>
     * If the received path doesn't start with "/", the method adds it.
     *
     * @param remoteFilePath Path
     * @return Encoded path according to RFC 2396, always starting with "/"
     */
    public static String encodePath(String remoteFilePath) {
        String encodedPath = Uri.encode(remoteFilePath, "/");
        if (!encodedPath.startsWith("/")) {
            encodedPath = "/" + encodedPath;
        }
        return encodedPath;
    }

    /**
     * @param httpBaseMethod from which to get the etag
     * @return etag from response
     */
    public static String getEtagFromResponse(HttpBaseMethod httpBaseMethod) {
        String eTag = httpBaseMethod.getResponseHeader("OC-ETag");
        if (eTag == null) {
            eTag = httpBaseMethod.getResponseHeader("oc-etag");
        }
        if (eTag == null) {
            eTag = httpBaseMethod.getResponseHeader("ETag");
        }
        if (eTag == null) {
            eTag = httpBaseMethod.getResponseHeader("etag");
        }
        String result = "";
        if (eTag != null) {
            result = eTag;
        }
        return result;
    }

    public static String normalizeProtocolPrefix(String url, boolean isSslConn) {
        if (!url.toLowerCase().startsWith("http://") &&
                !url.toLowerCase().startsWith("https://")) {
            if (isSslConn) {
                return "https://" + url;
            } else {
                return "http://" + url;
            }
        }
        return url;
    }

}