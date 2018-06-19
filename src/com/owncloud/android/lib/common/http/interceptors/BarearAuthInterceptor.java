package com.owncloud.android.lib.common.http.interceptors;

import okhttp3.Request;

public class BarearAuthInterceptor implements HttpInterceptor.RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final String mBarearToken;

    public BarearAuthInterceptor(String barearToken) {
        this.mBarearToken = barearToken;
    }

    @Override
    public Request intercept(Request request) {
        return request
                .newBuilder()
                .addHeader(AUTHORIZATION_HEADER, "Bearer " + mBarearToken)
                .build();
    }
}
