package com.owncloud.android.lib.common.http.interceptors;

import java.io.IOException;

import okhttp3.Request;

public class SamlAuthInterceptor implements HttpInterceptor.RequestInterceptor {

    private final String mSessionCookie;

    public SamlAuthInterceptor(String sessionCookie) {
        this.mSessionCookie = sessionCookie;
    }

    @Override
    public Request intercept(Request request) throws IOException {
        return request.newBuilder()
                .addHeader("Cookie", mSessionCookie)
                .build();
    }
}
