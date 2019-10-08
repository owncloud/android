/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.extensions

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R

fun Activity.showError(message: String, throwable: Throwable?) {
    val reason = throwable?.parseError(resources) ?: return
    if (reason.isEmpty()) {
        showMessage(message)
    } else {
        showMessage("$message ${getString(R.string.error_reason)} $reason")
    }
}

fun Activity.showMessage(
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(findViewById(android.R.id.content), message, duration).show()
}
