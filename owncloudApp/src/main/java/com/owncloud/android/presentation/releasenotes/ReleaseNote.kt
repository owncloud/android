/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
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

package com.owncloud.android.presentation.releasenotes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R

data class ReleaseNote(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val type: ReleaseNoteType
)

enum class ReleaseNoteType(@DrawableRes val drawableRes: Int) {
    BUGFIX(R.drawable.ic_release_notes_healing),
    CHANGE(R.drawable.ic_release_notes_autorenew),
    ENHANCEMENT(R.drawable.ic_release_notes_architecture),
    SECURITY(R.drawable.ic_lock)
}
