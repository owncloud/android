/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.refactor.authentication.credentials;

import com.owncloud.android.lib.refactor.authentication.OwnCloudCredentials;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Credentials;

public class OwnCloudBasicCredentials implements OwnCloudCredentials {

    private String mUsername;
    private String mPassword;

    public OwnCloudBasicCredentials(String username, String password) {
        mUsername = username != null ? username : "";
        mPassword = password != null ? password : "";
    }

    @Override
    public Map<String, String> getCredentialHeaders() {
        HashMap<String, String> header = new HashMap<>(1);
        header.put("Authorization", Credentials.basic(mUsername, mPassword));
        return header;
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

    @Override
    public String getAuthToken() {
        return mPassword;
    }

    @Override
    public boolean authTokenCanBeRefreshed() {
        return false;
    }

}
