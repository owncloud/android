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
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.presentation.ui.files.SortOrder.Companion.fromPreference
import com.owncloud.android.presentation.ui.files.SortType.Companion.fromPreference
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.android.synthetic.main.sort_options_layout.view.*

class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var onSortOptionsListener: SortOptionsListener? = null
    var onCreateFolderListener: CreateFolderListener? = null

    // Enable list view by default.
    var viewTypeSelected: ViewType = ViewType.VIEW_TYPE_LIST
        set(viewType) {
            view_type_selector.setImageDrawable(ContextCompat.getDrawable(context, viewType.getOppositeViewType().toDrawableRes()))
            field = viewType
        }

    // Enable sort by name by default.
    var sortTypeSelected: SortType = SortType.SORT_TYPE_BY_NAME
        set(sortType) {
            if (field == sortType) {
                // TODO: Should be changed directly, not here.
                sortOrderSelected = sortOrderSelected.getOppositeSortOrder()
            }
            sort_type_title.text = context.getText(sortType.toStringRes())
            field = sortType
        }

    // Enable sort ascending by default.
    var sortOrderSelected: SortOrder = SortOrder.SORT_ORDER_ASCENDING
        set(sortOrder) {
            sort_type_icon.setImageDrawable(ContextCompat.getDrawable(context, sortOrder.toDrawableRes()))
            field = sortOrder
        }

    init {
        View.inflate(context, R.layout.sort_options_layout, this)

        // Select sort type and order according to preference.
        val sortBy = PreferenceManager.getSortOrder(getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        sortTypeSelected = fromPreference(sortBy)

        val isAscending = PreferenceManager.getSortAscending(getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        sortOrderSelected = fromPreference(isAscending)

        sort_type_selector.setOnClickListener {
            onSortOptionsListener?.onSortTypeListener(
                sortTypeSelected,
                sortOrderSelected
            )
        }
        view_type_selector.setOnClickListener {
            onSortOptionsListener?.onViewTypeListener(
                viewTypeSelected.getOppositeViewType()
            )
        }
    }

    fun selectAdditionalView(additionalView: AdditionalView) {
        when (additionalView) {
            AdditionalView.CREATE_FOLDER -> {
                view_type_selector.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_create_dir))
                view_type_selector.setOnClickListener {
                    onCreateFolderListener?.onCreateFolderListener()
                }
            }
            AdditionalView.VIEW_TYPE -> {
                viewTypeSelected = viewTypeSelected
                view_type_selector.setOnClickListener {
                    onSortOptionsListener?.onViewTypeListener(
                        viewTypeSelected.getOppositeViewType()
                    )
                }
            }
        }
    }

    interface SortOptionsListener {
        fun onSortTypeListener(sortType: SortType, sortOrder: SortOrder)
        fun onViewTypeListener(viewType: ViewType)
    }

    interface CreateFolderListener {
        fun onCreateFolderListener()
    }

    enum class AdditionalView {
        CREATE_FOLDER, VIEW_TYPE
    }
}
