package com.owncloud.android.lib.refactor;

import android.net.Uri;

import com.owncloud.android.lib.refactor.authentication.OCCredentials;


public class OCContext {
    private static final String TAG = OCContext.class.toString();

    public static final String WEBDAV_PATH_4_0 = "/remote.php/dav";
    public static final String STATUS_PATH = "/status.php";
    public static final String FILES_WEB_PATH = "/index.php/apps/files";

    private static final int MAX_REDIRECTIONS_COUNT = 3;
    private static final int MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS = 1;
    private static final String PARAM_SINGLE_COOKIE_HEADER = "http.protocol.single-cookie-header";
    private static final boolean PARAM_SINGLE_COOKIE_HEADER_VALUE = true;
    private static final String PARAM_PROTOCOL_VERSION = "http.protocol.version";

    private OCCredentials mCredentials = null;
    private Uri mBaseUri;

    public class Builder {
        OCContext ocContext = new OCContext();

        public Builder setCredentials(OCCredentials credentials) {
            ocContext.mCredentials = credentials;
            return this;
        }

        public Builder setBaseUri(Uri baseUri) {
            ocContext.mBaseUri = baseUri;
            return this;
        }

        public OCContext build() {
            return ocContext;
        }
    }


    public OCCredentials getCredentials() {
        return mCredentials;
    }

    public Uri getBaseUri() {
        return mBaseUri;
    }
}
