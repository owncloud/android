/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.testutil.oauth

import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_REFRESH_TOKEN

val OC_TOKEN_RESPONSE = TokenResponse(
    accessToken = OC_ACCESS_TOKEN,
    expiresIn = 3600,
    refreshToken = OC_REFRESH_TOKEN,
    tokenType = "Bearer",
    userId = "demo",
    scope = null,
    additionalParameters = mapOf()
)
