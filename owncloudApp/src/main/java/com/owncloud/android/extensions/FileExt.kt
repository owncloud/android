/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.extensions

import android.content.Context
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun File.toLegibleStringSize(context: Context): String {
    val bytes = if (!exists()) 0L else length()
    return DisplayUtils.bytesToHumanReadable(bytes, context, true)
}

fun File.humanReadableModificationDateTime(context: Context): String {
    val dateTime = Date(this.lastModified())
    val dateString = SimpleDateFormat("yyyy-MMM-dd", Locale.getDefault()).format(dateTime)
    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateTime)
    return context.getString(R.string.homecloud_settings_logs_datetime_pattern, dateString, timeString)
}
