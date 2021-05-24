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
import android.os.Environment
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.lib.common.http.LogInterceptor
import com.owncloud.android.lib.common.utils.LoggingHelper
import timber.log.Timber
import java.io.File

class LogsProvider(
    context: Context
) {
    private val sharedPreferencesProvider = SharedPreferencesProviderImpl(context)

    fun startLogging() {
        val dataFolder = MainApp.dataFolder

        // Set folder for store logs
        LoggingHelper.startLogging(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + dataFolder), dataFolder
        )
        Timber.d("${BuildConfig.BUILD_TYPE} start logging ${BuildConfig.VERSION_NAME} ${BuildConfig.COMMIT_SHA1}")

        initHttpLogs()
    }

    fun stopLogging() {
        LoggingHelper.stopLogging()
    }

    fun initHttpLogs() {
        val httpLogsEnabled: Boolean = sharedPreferencesProvider.getBoolean(PREFERENCE_LOG_HTTP, false)
        LogInterceptor.httpLogsEnabled = httpLogsEnabled
    }

    fun shouldLogHttpRequests(logsEnabled: Boolean) {
        sharedPreferencesProvider.putBoolean(PREFERENCE_LOG_HTTP, logsEnabled)
        LogInterceptor.httpLogsEnabled = logsEnabled
    }

    companion object {
        const val PREFERENCE_LOG_HTTP = "set_httpLogs"
    }
}
