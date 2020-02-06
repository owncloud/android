/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.extensions

import android.app.Activity
import com.google.android.material.snackbar.Snackbar

fun Activity.showError(genericErrorMessage: String, throwable: Throwable?) =
    throwable?.let {
        showMessage(it.parseError(genericErrorMessage, resources))
    }

fun Activity.showMessage(
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(findViewById(android.R.id.content), message, duration).show()
}
