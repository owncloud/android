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

package com.owncloud.android.enums

import androidx.annotation.DrawableRes
import com.owncloud.android.R

enum class ReleaseNoteType(@DrawableRes val drawableRes: Int) {
    BUGFIX(R.drawable.ic_healing),
    CHANGE(R.drawable.ic_autorenew),
    ENHANCEMENT(R.drawable.ic_architecture),
    SECURITY(R.drawable.ic_lock)
}