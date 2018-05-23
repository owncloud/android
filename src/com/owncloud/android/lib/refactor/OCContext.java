package com.owncloud.android.lib.refactor;

import com.owncloud.android.lib.refactor.account.OCAccount;

public class OCContext {
    private static final String TAG = OCContext.class.toString();
    private static final int MAX_REDIRECTIONS_COUNT = 3;
    private static final int MAX_REPEAT_COUNT_WITH_FRESH_CREDENTIALS = 1;
    private static final String PARAM_SINGLE_COOKIE_HEADER = "http.protocol.single-cookie-header";
    private static final boolean PARAM_SINGLE_COOKIE_HEADER_VALUE = true;
    private static final String PARAM_PROTOCOL_VERSION = "http.protocol.version";

    private OCAccount mOCAccount;
    private String mUserAgent;

    public OCContext(OCAccount account, String userAgent) {
        mOCAccount = account;
        mUserAgent = userAgent;
    }

    public OCAccount getOCAccount() {
        return mOCAccount;
    }

    public String getUserAgent() {
        return mUserAgent;
    }
}