package com.owncloud.android.lib.common.methods.nonwebdav;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetMethod extends HttpMethod {

    public GetMethod(OkHttpClient okHttpClient, String url) {
        super(okHttpClient, url);
    }

    @Override
    public int execute() throws IOException {
        final Request request =
                new Request.Builder()
                .url(mUrl)
                .get()
                .build();

        Response response = mOkHttpClient.newCall(request).execute();
        return response.code();
    }
}