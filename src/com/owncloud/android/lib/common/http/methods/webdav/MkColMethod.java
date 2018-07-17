package com.owncloud.android.lib.common.http.methods.webdav;

import java.net.URL;

import at.bitfire.dav4android.exception.UnauthorizedException;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import okhttp3.Response;

public class MkColMethod extends DavMethod {
    public MkColMethod(URL url) {
        super(url);
    }

    @Override
    public int onExecute() throws Exception {
        try {
            mDavResource.mkCol(null, response -> {
                mResponse = response;
                return Unit.INSTANCE;
            });

        } catch (UnauthorizedException davException) {
            // Do nothing, we will use the 401 code to handle the situation
        }
        return super.getStatusCode();
    }
}
