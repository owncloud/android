package com.owncloud.android.lib.common.http.methods.webdav;

import at.bitfire.dav4android.exception.UnauthorizedException;
import okhttp3.HttpUrl;

public class MkColMethod extends DavMethod {
    public MkColMethod(HttpUrl httpUrl) {
        super(httpUrl);
    }

    @Override
    public int onExecute() throws Exception {
        try {
            mDavResource.mkCol(null);

            mRequest = mDavResource.getRequest();
            mResponse = mDavResource.getResponse();

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }
        return super.getStatusCode();
    }
}
