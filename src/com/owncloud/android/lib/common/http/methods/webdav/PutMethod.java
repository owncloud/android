/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.lib.common.http.methods.webdav;

import java.io.IOException;

import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.UnauthorizedException;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;

/**
 * Put calls wrapper
 * @author David González Verdugo
 */
public class PutMethod extends DavMethod {

    private RequestBody mRequestBody;
    private String mIfMatchETag;
    private boolean mIfNoneMatch;
    private String mContentType;
    private String mOcTotalLength;
    private String mOcXOcMtimeHeader;

    public PutMethod(HttpUrl httpUrl) {
        super(httpUrl);
    };

    @Override
    public int execute() throws IOException, HttpException, DavException {
        try {
            mDavResource.put(
                    mRequestBody,
                    mIfMatchETag,
                    mIfNoneMatch,
                    mContentType,
                    mOcTotalLength,
                    mOcXOcMtimeHeader
            );

            mRequest = mDavResource.getRequest();
            mResponse = mDavResource.getResponse();

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }

        return super.getStatusCode();
    }
}
