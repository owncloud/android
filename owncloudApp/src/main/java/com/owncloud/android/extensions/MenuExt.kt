/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

import android.view.Menu
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.FileMenuOption

fun Menu.filterMenuOptions(
    optionsToShow: List<FileMenuOption>,
    hasWritePermission: Boolean,
) {
    FileMenuOption.values().forEach { fileMenuOption ->
        val item = this.findItem(fileMenuOption.toResId())
        item?.let {
            if (optionsToShow.contains(fileMenuOption)) {
                it.isVisible = true
                it.isEnabled = true
                if (fileMenuOption.toResId() == R.id.action_open_file_with) {
                    if (!hasWritePermission) {
                        item.setTitle(R.string.actionbar_open_with_read_only)
                    } else {
                        item.setTitle(R.string.actionbar_open_with)
                    }
                }
            } else {
                it.isVisible = false
                it.isEnabled = false
            }
        }

    }
}
