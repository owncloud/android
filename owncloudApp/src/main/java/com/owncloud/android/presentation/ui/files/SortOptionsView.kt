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
package com.owncloud.android.presentation.ui.files

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import kotlinx.android.synthetic.main.sort_options_layout.view.*

class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var onSortOptionsListener: SortOptionsListener? = null

    // Enable list view by default.
    var viewTypeSelected: ViewType = ViewType.VIEW_TYPE_LIST
        set(viewType) {
            view_type_selector.setImageDrawable(ContextCompat.getDrawable(context, viewType.toDrawableRes()))
            field = viewType
        }

    init {
        View.inflate(context, R.layout.sort_options_layout, this)

        sort_type_selector.setOnClickListener { onSortOptionsListener?.onSortTypeListener() }
        sort_type_mode.setOnClickListener { onSortOptionsListener?.onSortTypeOrderListener() }
        view_type_selector.setOnClickListener { onSortOptionsListener?.onViewTypeListener(viewTypeSelected.getAlternativeViewType()) }
    }

    interface SortOptionsListener {

        fun onSortTypeListener()
        fun onSortTypeOrderListener()
        fun onViewTypeListener(viewType: ViewType)
    }

}
