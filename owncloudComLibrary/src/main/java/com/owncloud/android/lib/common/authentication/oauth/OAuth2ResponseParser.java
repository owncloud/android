/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.lib.common.authentication.oauth;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class OAuth2ResponseParser {

    Map<String, String> parseAccessTokenResult(JSONObject tokenJson) throws JSONException {
        Map<String, String> resultTokenMap = new HashMap<>();

        if (tokenJson.has(OAuth2Constants.KEY_ACCESS_TOKEN)) {
            resultTokenMap.put(OAuth2Constants.KEY_ACCESS_TOKEN, tokenJson.
                getString(OAuth2Constants.KEY_ACCESS_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_TOKEN_TYPE)) {
            resultTokenMap.put(OAuth2Constants.KEY_TOKEN_TYPE, tokenJson.
                getString(OAuth2Constants.KEY_TOKEN_TYPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_EXPIRES_IN)) {
            resultTokenMap.put(OAuth2Constants.KEY_EXPIRES_IN, tokenJson.
                getString(OAuth2Constants.KEY_EXPIRES_IN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_REFRESH_TOKEN)) {
            resultTokenMap.put(OAuth2Constants.KEY_REFRESH_TOKEN, tokenJson.
                getString(OAuth2Constants.KEY_REFRESH_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_SCOPE)) {
            resultTokenMap.put(OAuth2Constants.KEY_SCOPE, tokenJson.
                getString(OAuth2Constants.KEY_SCOPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR)) {
            resultTokenMap.put(OAuth2Constants.KEY_ERROR, tokenJson.
                getString(OAuth2Constants.KEY_ERROR));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_DESCRIPTION)) {
            resultTokenMap.put(OAuth2Constants.KEY_ERROR_DESCRIPTION, tokenJson.
                getString(OAuth2Constants.KEY_ERROR_DESCRIPTION));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_URI)) {
            resultTokenMap.put(OAuth2Constants.KEY_ERROR_URI, tokenJson.
                getString(OAuth2Constants.KEY_ERROR_URI));
        }

        if (tokenJson.has(OAuth2Constants.KEY_USER_ID)) {   // not standard
            resultTokenMap.put(OAuth2Constants.KEY_USER_ID, tokenJson.
                getString(OAuth2Constants.KEY_USER_ID));
        }

        return resultTokenMap;
    }

}
