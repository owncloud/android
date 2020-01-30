/* ownCloud Android Library is available under MIT license
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.lib.common.authentication.oauth;

import timber.log.Timber;

import java.util.HashMap;
import java.util.Map;

public class OAuth2QueryParser {

    private Map<String, String> mOAuth2ParsedAuthorizationResponse;

    public OAuth2QueryParser() {
        mOAuth2ParsedAuthorizationResponse = new HashMap<>();
    }

    public Map<String, String> parse(String query) {
        mOAuth2ParsedAuthorizationResponse.clear();

        if (query != null) {
            String[] pairs = query.split("&");
            int i = 0;
            String key = "";
            String value;
            while (pairs.length > i) {
                int j = 0;
                String[] part = pairs[i].split("=");
                while (part.length > j) {
                    String p = part[j];
                    if (j == 0) {
                        key = p;
                    } else if (j == 1) {
                        value = p;
                        mOAuth2ParsedAuthorizationResponse.put(key, value);
                    }

                    Timber.v("[" + i + "," + j + "] = " + p);
                    j++;
                }
                i++;
            }
        }

        return mOAuth2ParsedAuthorizationResponse;
    }

}
