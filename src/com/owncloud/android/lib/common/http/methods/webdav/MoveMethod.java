package com.owncloud.android.lib.common.http.methods.webdav;

import com.owncloud.android.lib.common.http.HttpConstants;

import java.net.URL;

import at.bitfire.dav4android.exception.UnauthorizedException;

public class MoveMethod extends DavMethod {
    final String destinationUrl;
    final boolean forceOverride;

    public MoveMethod(URL url, String destinationUrl, boolean forceOverride) {
        super(url);
        this.destinationUrl = destinationUrl;
        this.forceOverride = forceOverride;
    }

    @Override
    public int onExecute() throws Exception {
        try {
            mDavResource.move(
                    destinationUrl,
                    forceOverride,
                    super.getRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER),
                    super.getRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER)
            );

            mRequest = mDavResource.getRequest();
            mResponse = mDavResource.getResponse();

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }
        return super.getStatusCode();
    }
}
