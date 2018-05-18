package com.owncloud.android.lib.refactor.authentication.credentials;

import java.util.HashMap;
import java.util.Map;
public class OCAnonymousCredentials implements OCCredentials {

    @Override
    public Map<String, String> getCredentialHeaders() {
        return new HashMap<>(0);
    }

    @Override
    public String getCredentialCookie() {
        return null;
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getAuthToken() {
        return null;
    }

    @Override
    public boolean authTokenCanBeRefreshed() {
        return false;
    }
}