package com.owncloud.android.lib.common.methods;

import okhttp3.Request;
import okhttp3.Response;

public abstract class HttpBaseMethod {
    public abstract int execute() throws Exception;
    protected Request mRequest;
    protected Response mResponse;

    public Request getRequest() {
        return mRequest;
    }

    public Response getResponse() {
        return mResponse;
    }
}