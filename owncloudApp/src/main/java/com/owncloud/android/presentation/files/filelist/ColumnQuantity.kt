/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.presentation.files.filelist

import android.content.Context
import android.util.DisplayMetrics
import android.view.View

/**
 * This class dynamically calculates the number of columns
 * based on the device screen for the RecyclerView Grid mode.
 */
class ColumnQuantity(context: Context, viewId: Int) {

    private var width: Int = 0
    private var height: Int = 0
    private var remaining: Int = 0
    private var displayMetrics: DisplayMetrics

    init {
        val view: View = View.inflate(context, viewId, null)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        width = view.measuredWidth
        height = view.measuredHeight
        displayMetrics = context.resources.displayMetrics
    }

    fun calculateNoOfColumns(): Int {
        var numberOfColumns = displayMetrics.widthPixels.div(width)
        remaining = displayMetrics.widthPixels.minus(numberOfColumns.times(width))
        if (remaining.div(numberOfColumns.times(2)) < 15) {
            numberOfColumns.minus(1)
            remaining = displayMetrics.widthPixels.minus(numberOfColumns.times(width))
        }
        return numberOfColumns
    }

}
