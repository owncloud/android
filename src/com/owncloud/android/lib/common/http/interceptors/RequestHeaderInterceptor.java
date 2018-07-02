package com.owncloud.android.lib.common.http.interceptors;

import okhttp3.Request;

public class RequestHeaderInterceptor implements HttpInterceptor.RequestInterceptor {

    private String mHeaderName;
    private String mHeaderValue;

    public RequestHeaderInterceptor(String headerName, String headerValue) {
        this.mHeaderName = headerName;
        this.mHeaderValue = headerValue;
    }

    @Override
    public Request intercept(Request request) {
        return request.newBuilder().addHeader(mHeaderName, mHeaderValue).build();
    }

    public String getHeaderName() {
        return mHeaderName;
    }

    public String getHeaderValue() {
        return mHeaderValue;
    }
}