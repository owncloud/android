/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

import com.owncloud.android.R
import com.owncloud.android.domain.spaces.model.SpaceMenuOption

fun SpaceMenuOption.toStringResId() =
    when (this) {
        SpaceMenuOption.EDIT -> R.string.edit_space
        SpaceMenuOption.EDIT_IMAGE -> R.string.edit_space_image
        SpaceMenuOption.DISABLE -> R.string.disable_space
        SpaceMenuOption.ENABLE -> R.string.enable_space
        SpaceMenuOption.DELETE -> R.string.delete_space
    }

fun SpaceMenuOption.toDrawableResId() =
    when (this) {
        SpaceMenuOption.EDIT -> R.drawable.ic_pencil
        SpaceMenuOption.EDIT_IMAGE -> R.drawable.file_image
        SpaceMenuOption.DISABLE -> R.drawable.ic_disable_space
        SpaceMenuOption.ENABLE -> R.drawable.ic_enable_space
        SpaceMenuOption.DELETE -> R.drawable.ic_action_delete_white
    }
