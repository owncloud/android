/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.providers

import android.content.Context
import android.content.RestrictionsManager
import androidx.enterprise.feedback.KeyedAppState
import androidx.enterprise.feedback.KeyedAppStatesReporter
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY

class MdmProvider(
    private val context: Context
) {
    private val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    private val restrictions = restrictionsManager.applicationRestrictions
    private val preferencesProvider = SharedPreferencesProviderImpl(context)
    private val restrictionsReporter = KeyedAppStatesReporter.create(context)

    fun cacheStringRestriction(key: String, idMessageFeedback: Int) {
        if (restrictions.containsKey(key)) {
            val conf = restrictions.getString(key)
            val isConfCached = preferencesProvider.contains(key)
            val oldConf = preferencesProvider.getString(key, null)
            conf?.let { preferencesProvider.putString(key, it) }
            if (!isConfCached || !conf.equals(oldConf)) {
                sendFeedback(
                    key = key,
                    message = context.getString(idMessageFeedback)
                )
            }
        }
    }

    fun cacheBooleanRestriction(key: String, idMessageFeedback: Int) {
        if (restrictions.containsKey(key)) {
            val conf = restrictions.getBoolean(key)
            val isConfCached = preferencesProvider.contains(key)
            val oldConf = preferencesProvider.getBoolean(key, false)
            preferencesProvider.putBoolean(key, conf)
            if (!isConfCached || (conf != oldConf)) {
                sendFeedback(
                    key = key,
                    message = context.getString(idMessageFeedback)
                )
            }
        }
    }

    private fun sendFeedback(
        reporter: KeyedAppStatesReporter = restrictionsReporter,
        key: String,
        isError: Boolean = false,
        message: String,
        data: String? = null
    ) {
        val severity = if (isError) KeyedAppState.SEVERITY_ERROR else KeyedAppState.SEVERITY_INFO
        val states = hashSetOf(
            KeyedAppState.builder()
                .setKey(key)
                .setSeverity(severity)
                .setMessage(message)
                .setData(data)
                .build()
        )
        reporter.setStates(states, null)
    }
}