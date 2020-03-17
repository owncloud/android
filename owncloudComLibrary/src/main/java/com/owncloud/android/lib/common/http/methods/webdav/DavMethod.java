/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

import at.bitfire.dav4android.Constants;
import at.bitfire.dav4android.DavOCResource;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.exception.RedirectException;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper to perform WebDAV (dav4android) calls
 *
 * @author David González Verdugo
 */
public abstract class DavMethod extends HttpBaseMethod {

    protected DavOCResource mDavResource;

    protected DavMethod(URL url) {
        super(url);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(url.toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public void abort() {
        mDavResource.cancelCall();
    }

    @Override
    public int execute() throws Exception {
        try {
            return onExecute();
        } catch (HttpException httpException) {
            // Modify responses with information gathered from exceptions
            if (httpException instanceof RedirectException) {
                mResponse = new Response.Builder()
                        .header(
                                HttpConstants.LOCATION_HEADER, ((RedirectException) httpException).getRedirectLocation()
                        )
                        .code(httpException.getCode())
                        .request(mRequest)
                        .message(httpException.getMessage())
                        .protocol(Protocol.HTTP_1_1)
                        .build();

            } else if (mResponse != null) {
                ResponseBody responseBody = ResponseBody.create(
                        mResponse.body().contentType(),
                        httpException.getResponseBody()
                );

                mResponse = mResponse.newBuilder()
                        .body(responseBody)
                        .build();
            }

            return httpException.getCode();
        }
    }

    //////////////////////////////
    //         Setter
    //////////////////////////////

    // Connection parameters
    @Override
    public void setReadTimeout(long readTimeout, TimeUnit timeUnit) {
        super.setReadTimeout(readTimeout, timeUnit);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(mRequest.url().toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public void setConnectionTimeout(long connectionTimeout, TimeUnit timeUnit) {
        super.setConnectionTimeout(connectionTimeout, timeUnit);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(mRequest.url().toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) {
        super.setFollowRedirects(followRedirects);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(mRequest.url().toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public void setUrl(HttpUrl url) {
        super.setUrl(url);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(mRequest.url().toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public boolean getRetryOnConnectionFailure() {
        return false; //TODO: implement me
    }

    //////////////////////////////
    //         Getter
    //////////////////////////////

    @Override
    public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure) {
        super.setRetryOnConnectionFailure(retryOnConnectionFailure);
        mDavResource = new DavOCResource(
                mOkHttpClient,
                HttpUrl.parse(mRequest.url().toString()),
                Constants.INSTANCE.getLog());
    }

    @Override
    public boolean isAborted() {
        return mDavResource.isCallAborted();
    }
}