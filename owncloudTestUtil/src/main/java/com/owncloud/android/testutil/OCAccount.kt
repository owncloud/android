package com.owncloud.android.testutil

import android.accounts.Account

const val OC_ACCOUNT_ID = "username"
const val OC_ACCOUNT_NAME = "$OC_ACCOUNT_ID@demo.owncloud.com"

/**
 * Accounts
 */
val OC_ACCOUNT = Account(OC_ACCOUNT_NAME, "owncloud")

/**
 * BasicCredentials
 */
const val OC_BASIC_USERNAME = "user"
const val OC_BASIC_PASSWORD = "password"

/**
 * OAuth
 */
const val OC_OAUTH_SUPPORTED_TRUE = "TRUE"
const val OC_AUTH_TOKEN_TYPE = "owncloud.oauth2.access_token"
const val OC_ACCESS_TOKEN = "Asqweh12p93yehd10eu"
const val OC_REFRESH_TOKEN = "P3sd19DSsjdp1jwdd1"
const val OC_SCOPE = "email"
