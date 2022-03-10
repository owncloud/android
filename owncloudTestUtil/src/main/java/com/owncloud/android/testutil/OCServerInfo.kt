/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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


package com.owncloud.android.testutil

import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo

const val OC_BASE_URL = "https://demo.owncloud.com"

val OC_SERVER_INFO = ServerInfo(
    authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH,
    baseUrl = "https://demo.owncloud.com",
    ownCloudVersion = "10.3.2.1",
    isSecureConnection = false
)
