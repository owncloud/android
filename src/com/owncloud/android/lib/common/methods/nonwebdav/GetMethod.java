package com.owncloud.android.lib.common.methods.nonwebdav;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetMethod extends HttpMethod {

    public GetMethod(OkHttpClient okHttpClient, Request requestBase) {
        super(okHttpClient, requestBase);
    }

    @Override
    public int execute() throws IOException {
        final Request request = mBaseRequest
                .newBuilder()
                .get()
                .build();

        mResponse = mOkHttpClient.newCall(request).execute();
        return mResponse.code();
    }
}