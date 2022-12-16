/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.files

import androidx.annotation.DrawableRes
import com.owncloud.android.R

enum class ViewType {
    VIEW_TYPE_GRID, VIEW_TYPE_LIST;

    fun getOppositeViewType(): ViewType =
        when (this) {
            VIEW_TYPE_LIST -> VIEW_TYPE_GRID
            VIEW_TYPE_GRID -> VIEW_TYPE_LIST
        }

    @DrawableRes
    fun toDrawableRes(): Int =
        when (this) {
            VIEW_TYPE_LIST -> R.drawable.ic_baseline_view_list
            VIEW_TYPE_GRID -> R.drawable.ic_baseline_view_grid
        }
}
