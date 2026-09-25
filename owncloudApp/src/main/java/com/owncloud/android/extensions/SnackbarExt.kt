/**
 * ownCloud Android client application
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
package com.owncloud.android.extensions

import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.google.android.material.snackbar.Snackbar

fun Snackbar.applyResponsiveWidth(): Snackbar {
    if (view.context.resources.configuration.smallestScreenWidthDp >= 600) {
        view.updateLayoutParams<ViewGroup.LayoutParams> {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }
    return this
}
