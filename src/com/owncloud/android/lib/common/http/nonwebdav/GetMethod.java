package com.owncloud.android.lib.common.http.nonwebdav;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetMethod extends HttpMethod {

    public GetMethod(OkHttpClient okHttpClient, Request baseRequest) {
        super(okHttpClient, baseRequest);
    }

    @Override
    public Response execute() throws IOException {
        mRequest = mBaseRequest
                .newBuilder()
                .get()
                .build();

        return mOkHttpClient.newCall(mRequest).execute();
    }
}