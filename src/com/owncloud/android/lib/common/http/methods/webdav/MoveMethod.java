package com.owncloud.android.lib.common.http.methods.webdav;

import at.bitfire.dav4android.exception.UnauthorizedException;
import okhttp3.HttpUrl;

public class MoveMethod extends DavMethod {
    final String destinationUrl;
    final boolean forceOverride;

    public MoveMethod(HttpUrl httpUrl, String destinationUrl, boolean forceOverride) {
        super(httpUrl);
        this.destinationUrl = destinationUrl;
        this.forceOverride = forceOverride;
    }

    @Override
    public int execute() throws Exception {
        try {
            mDavResource.move(destinationUrl, forceOverride);

            mRequest = mDavResource.getRequest();
            mResponse = mDavResource.getResponse();

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }
        return super.getStatusCode();
    }
}
