package com.owncloud.android.lib.common.methods.nonwebdav;

import com.owncloud.android.lib.common.methods.HttpBaseMethod;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class HttpMethod extends HttpBaseMethod {

    protected OkHttpClient mOkHttpClient;
    protected Request mBaseRequest;

    public HttpMethod (OkHttpClient okHttpClient, Request baseRequest) {
        mOkHttpClient = okHttpClient;
        mBaseRequest = baseRequest;
    }
}