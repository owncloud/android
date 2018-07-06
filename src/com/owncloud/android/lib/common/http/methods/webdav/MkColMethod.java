package com.owncloud.android.lib.common.http.methods.webdav;

import java.net.URL;

import at.bitfire.dav4android.exception.UnauthorizedException;

public class MkColMethod extends DavMethod {
    public MkColMethod(URL url) {
        super(url);
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
