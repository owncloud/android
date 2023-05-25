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

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SortType : Parcelable {
    SORT_TYPE_BY_NAME, SORT_TYPE_BY_DATE, SORT_TYPE_BY_SIZE;

    @StringRes
    fun toStringRes(): Int =
        when (this) {
            SORT_TYPE_BY_NAME -> R.string.global_name
            SORT_TYPE_BY_DATE -> R.string.global_date
            SORT_TYPE_BY_SIZE -> R.string.global_size
        }

    companion object {
        const val PREF_FILE_LIST_SORT_TYPE = "PREF_FILE_LIST_SORT_TYPE"

        fun fromPreference(value: Int): SortType =
            when (value) {
                FileStorageUtils.SORT_NAME -> SORT_TYPE_BY_NAME
                FileStorageUtils.SORT_SIZE -> SORT_TYPE_BY_SIZE
                FileStorageUtils.SORT_DATE -> SORT_TYPE_BY_DATE
                else -> throw IllegalArgumentException("Sort type not supported")
            }
    }
}

@Parcelize
enum class SortOrder : Parcelable {
    SORT_ORDER_ASCENDING, SORT_ORDER_DESCENDING;

    fun getOppositeSortOrder(): SortOrder =
        when (this) {
            SORT_ORDER_ASCENDING -> SORT_ORDER_DESCENDING
            SORT_ORDER_DESCENDING -> SORT_ORDER_ASCENDING
        }

    @DrawableRes
    fun toDrawableRes(): Int =
        when (this) {
            SORT_ORDER_ASCENDING -> R.drawable.ic_baseline_arrow_upward
            SORT_ORDER_DESCENDING -> R.drawable.ic_baseline_arrow_downward
        }

    companion object {
        const val PREF_FILE_LIST_SORT_ORDER = "PREF_FILE_LIST_SORT_ORDER"

        fun fromPreference(value: Int) =
            when (value) {
                SORT_ORDER_ASCENDING.ordinal -> SORT_ORDER_ASCENDING
                SORT_ORDER_DESCENDING.ordinal -> SORT_ORDER_DESCENDING
                else -> SORT_ORDER_ASCENDING
            }
    }
}
