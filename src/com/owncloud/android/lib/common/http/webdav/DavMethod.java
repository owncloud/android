package com.owncloud.android.lib.common.http.webdav;

import com.owncloud.android.lib.common.http.HttpBaseMethod;

import at.bitfire.dav4android.DavOCResource;
import at.bitfire.dav4android.DavResource;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public abstract class DavMethod extends HttpBaseMethod {

    protected DavResource mDavResource;

    public DavMethod(OkHttpClient okHttpClient, HttpUrl httpUrl) {
        mDavResource = new DavOCResource(okHttpClient, httpUrl);
    }

    public DavResource getDavResource() {
        return mDavResource;
    }
}