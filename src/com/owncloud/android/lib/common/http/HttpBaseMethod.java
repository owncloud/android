package com.owncloud.android.lib.common.http;

import okhttp3.Request;
import okhttp3.Response;

public abstract class HttpBaseMethod {
    public abstract Response execute() throws Exception;
    protected Request mRequest;

    public Request getRequest() {
        return mRequest;
    }
}