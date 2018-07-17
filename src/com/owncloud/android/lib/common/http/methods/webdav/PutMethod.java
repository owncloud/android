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

import com.owncloud.android.lib.common.http.HttpConstants;

import java.io.IOException;
import java.net.URL;

import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.UnauthorizedException;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Put calls wrapper
 * @author David González Verdugo
 */
public class PutMethod extends DavMethod {

    public PutMethod(URL url) {
        super(url);
    };

    @Override
    public int onExecute() throws IOException, HttpException {
        try {
            mDavResource.put(
                    mRequestBody,
                    super.getRequestHeader(HttpConstants.IF_MATCH_HEADER),
                    super.getRequestHeader(HttpConstants.CONTENT_TYPE_HEADER),
                    super.getRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER),
                    super.getRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER), response -> {
                        mResponse = response;
                        return Unit.INSTANCE;
                    });

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }

        return super.getStatusCode();
    }
}