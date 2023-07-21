/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.settings.logging

import androidx.lifecycle.ViewModel
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.providers.LogsProvider
import com.owncloud.android.providers.WorkManagerProvider

class SettingsLogsViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val logsProvider: LogsProvider,
    private val workManagerProvider: WorkManagerProvider,
) : ViewModel() {

    fun shouldLogHttpRequests(value: Boolean) = logsProvider.shouldLogHttpRequests(value)

    fun setEnableLogging(value: Boolean) {
        preferencesProvider.putBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, value)
        if (value) {
            logsProvider.startLogging()
        } else {
            logsProvider.stopLogging()
        }
    }

    fun isLoggingEnabled() = preferencesProvider.getBoolean(SettingsLogsFragment.PREFERENCE_ENABLE_LOGGING, false)

    fun enqueueOldLogsCollectorWorker() {
        workManagerProvider.enqueueOldLogsCollectorWorker()
    }
}
