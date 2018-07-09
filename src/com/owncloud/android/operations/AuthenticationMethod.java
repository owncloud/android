package com.owncloud.android.operations;

public enum AuthenticationMethod {
    UNKNOWN,
    NONE,
    BASIC_HTTP_AUTH,
    SAML_WEB_SSO,
    BEARER_TOKEN;

    public int getValue() {
        return ordinal();
    }

    public static AuthenticationMethod fromValue(int value) {
        if (value > -1 && value < values().length) {
            return values()[value];
        } else {
            return null;
        }
    }
}