/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
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

package com.owncloud.android.operations;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

public class OAuth2RefreshAccessToken extends RemoteOperation {

    private static final String TAG = OAuth2GetAccessToken.class.getSimpleName();

    private String mClientId;
    private String mSecretId;
    private String mGrantType;
    private String mRefreshToken;

    public OAuth2RefreshAccessToken(
            String clientId,
            String secretId,
            String grantType,
            String refreshToken
    ) {

        mClientId = clientId;
        mSecretId = secretId;
        mGrantType = grantType;
        mRefreshToken = refreshToken;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {


        return null;
    }
}
