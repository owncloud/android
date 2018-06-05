package com.owncloud.android.lib.common.methods.nonwebdav;

import com.owncloud.android.lib.common.methods.HttpBaseMethod;

import okhttp3.OkHttpClient;

public abstract class HttpMethod implements HttpBaseMethod {

    protected OkHttpClient mOkHttpClient;
    protected String mUrl;

    public HttpMethod (OkHttpClient okHttpClient, String url) {
        mOkHttpClient = okHttpClient;
        mUrl = url;
    }
}