/**
 * ownCloud Android client application
 *
 * @author Jesus Recio Rincon
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.test

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit

/**
 *
 * A ContentProvider used only in the QA flavor to allow
 * external test tools (E2E tests for automation) to modify
 * SharedPreferences at runtime.
 *
 * This provider is NOT intended for production usage.
 */
class TestPreferencesProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?
    ): Bundle {

        val prefs = context!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (method == SET_BOOLEAN) {
                prefs.edit {
                    putBoolean(arg!!, extras!!.getBoolean("value"))
                }
            }
        return Bundle()
    }

    override fun query(
        uri: Uri, projection: Array<out String>?,
        selection: String?, selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    companion object {
        private const val SET_BOOLEAN = "set_boolean"
        private const val PREFS_NAME = "com.owncloud.android_preferences"
    }
}
