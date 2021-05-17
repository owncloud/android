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

package com.owncloud.android.data.preferences.datasources.implementation

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider

class SharedPreferencesProviderImpl(
    context: Context
) : SharedPreferencesProvider {

    // TODO: Move to Androidx Preferences or DataStore
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = sharedPreferences.edit()

    override fun putString(key: String, value: String) = editor.putString(key, value).apply()
    override fun getString(key: String, defaultValue: String?) = sharedPreferences.getString(key, defaultValue)

    override fun putInt(key: String, value: Int) = editor.putInt(key, value).apply()
    override fun getInt(key: String, defaultValue: Int) = sharedPreferences.getInt(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = editor.putBoolean(key, value).apply()
    override fun getBoolean(key: String, defaultValue: Boolean) = sharedPreferences.getBoolean(key, defaultValue)

    override fun removePreference(key: String) = editor.remove(key).apply()
}
