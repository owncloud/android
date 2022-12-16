/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
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
@file:JvmName("AuthenticatorConstants")

package com.owncloud.android.presentation.authentication

import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.lib.common.accounts.AccountTypeUtils

const val EXTRA_ACTION = "ACTION"
const val EXTRA_ACCOUNT = "ACCOUNT"

const val ACTION_CREATE: Byte = 0
const val ACTION_UPDATE_TOKEN: Byte = 1 // requested by the user
const val ACTION_UPDATE_EXPIRED_TOKEN: Byte = 2 // detected by the app

const val KEY_AUTH_TOKEN_TYPE = "authTokenType"

val BASIC_TOKEN_TYPE: String = AccountTypeUtils.getAuthTokenTypePass(
    accountType
)

val OAUTH_TOKEN_TYPE: String = AccountTypeUtils.getAuthTokenTypeAccessToken(
    accountType
)

const val UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG"
const val WAIT_DIALOG_TAG = "WAIT_DIALOG"
