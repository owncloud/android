package com.owncloud.android.lib.common.http.methods.webdav;

import com.owncloud.android.lib.common.http.HttpConstants;

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
