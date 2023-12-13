/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.providers

import android.content.Context
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.data.providers.ScopedStorageProvider
import com.owncloud.android.lib.common.http.logging.LogInterceptor
import com.owncloud.android.lib.common.utils.LoggingHelper
import com.owncloud.android.utils.CONFIGURATION_REDACT_AUTH_HEADER_LOGS
import timber.log.Timber
import java.io.File

class LogsProvider(
    private val context: Context,
    private val mdmProvider: MdmProvider,
) {
    private val sharedPreferencesProvider = OCSharedPreferencesProvider(context)

    fun startLogging() {
        val dataFolder = MainApp.dataFolder
        val localStorageProvider = ScopedStorageProvider(dataFolder, context)

        // Set folder for store logs
        LoggingHelper.startLogging(
            directory = File(localStorageProvider.getLogsPath()),
            storagePath = dataFolder
        )
        Timber.d("${BuildConfig.BUILD_TYPE} start logging ${BuildConfig.VERSION_NAME} ${BuildConfig.COMMIT_SHA1}")

        initHttpLogs()
    }

    fun stopLogging() {
        LoggingHelper.stopLogging()
    }

    private fun initHttpLogs() {
        val httpLogsEnabled: Boolean = sharedPreferencesProvider.getBoolean(PREFERENCE_LOG_HTTP, false)
        LogInterceptor.httpLogsEnabled = httpLogsEnabled
        val redactAuthHeader = mdmProvider.getBrandingBoolean(mdmKey = CONFIGURATION_REDACT_AUTH_HEADER_LOGS, booleanKey = R.bool.redact_auth_header_logs)
        LogInterceptor.redactAuthHeader = redactAuthHeader
    }

    fun shouldLogHttpRequests(logsEnabled: Boolean) {
        sharedPreferencesProvider.putBoolean(PREFERENCE_LOG_HTTP, logsEnabled)
        LogInterceptor.httpLogsEnabled = logsEnabled
    }

    companion object {
        private const val PREFERENCE_LOG_HTTP = "set_httpLogs"
    }
}
