/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider
import com.owncloud.android.databinding.SortOptionsLayoutBinding
import com.owncloud.android.extensions.setAccessibilityRole
import com.owncloud.android.presentation.files.SortOrder.Companion.PREF_FILE_LIST_SORT_ORDER
import com.owncloud.android.presentation.files.SortOrder.SORT_ORDER_ASCENDING
import com.owncloud.android.presentation.files.SortType.Companion.PREF_FILE_LIST_SORT_TYPE

class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var onSortOptionsListener: SortOptionsListener? = null
    var onCreateFolderListener: CreateFolderListener? = null

    private var _binding: SortOptionsLayoutBinding? = null
    private val binding get() = _binding!!

    // Enable list view by default.
    var viewTypeSelected: ViewType = ViewType.VIEW_TYPE_LIST
        set(viewType) {
            binding.viewTypeSelector.setImageDrawable(ContextCompat.getDrawable(context, viewType.getOppositeViewType().toDrawableRes()))
            field = viewType
        }

    // Enable sort by name by default.
    var sortTypeSelected: SortType = SortType.SORT_TYPE_BY_NAME
        set(sortType) {
            if (field == sortType) {
                // TODO: Should be changed directly, not here.
                sortOrderSelected = sortOrderSelected.getOppositeSortOrder()
            }
            binding.sortTypeTitle.text = context.getText(sortType.toStringRes())
            field = sortType
        }

    // Enable sort ascending by default.
    var sortOrderSelected: SortOrder = SortOrder.SORT_ORDER_ASCENDING
        set(sortOrder) {
            binding.sortTypeIcon.setImageDrawable(ContextCompat.getDrawable(context, sortOrder.toDrawableRes()))
            field = sortOrder
        }

    init {
        _binding = SortOptionsLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        val sharedPreferencesProvider: SharedPreferencesProvider = OCSharedPreferencesProvider(context)

        // Select sort type and order according to preferences.
        sortTypeSelected = SortType.values()[sharedPreferencesProvider.getInt(PREF_FILE_LIST_SORT_TYPE, SortType.SORT_TYPE_BY_NAME.ordinal)]
        sortOrderSelected = SortOrder.values()[sharedPreferencesProvider.getInt(PREF_FILE_LIST_SORT_ORDER, SortOrder.SORT_ORDER_ASCENDING.ordinal)]
        binding.sortTypeTitle.setAccessibilityRole(className = Button::class.java)
        binding.sortTypeSelector.setOnClickListener {
            onSortOptionsListener?.onSortTypeListener(
                sortTypeSelected,
                sortOrderSelected
            )
        }
        binding.viewTypeSelector.setOnClickListener {
            onSortOptionsListener?.onViewTypeListener(
                viewTypeSelected.getOppositeViewType()
            )
        }
        ViewCompat.setAccessibilityDelegate(binding.sortTypeSelector, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(v, info)
                val sortTitleText = binding.sortTypeTitle.text
                if (sortOrderSelected == SORT_ORDER_ASCENDING) {
                    binding.sortTypeTitle.contentDescription = context.getString(R.string.content_description_sort_by_name_ascending, sortTitleText)
                } else {
                    binding.sortTypeTitle.contentDescription = context.getString(R.string.content_description_sort_by_name_descending, sortTitleText)
                }
            }
        })

    }

    fun selectAdditionalView(additionalView: AdditionalView) {
        when (additionalView) {
            AdditionalView.CREATE_FOLDER -> {
                binding.viewTypeSelector.apply {
                    visibility = VISIBLE
                    contentDescription = context.getString(R.string.content_description_create_new_folder)
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_create_dir))
                    setOnClickListener {
                        onCreateFolderListener?.onCreateFolderListener()
                    }
                }
            }
            AdditionalView.VIEW_TYPE -> {
                viewTypeSelected = viewTypeSelected
                binding.viewTypeSelector.apply {
                    visibility = VISIBLE
                    contentDescription = context.getString(R.string.content_description_type_view)
                    setOnClickListener {
                        onSortOptionsListener?.onViewTypeListener(
                            viewTypeSelected.getOppositeViewType()
                        )
                    }
                }
            }
            AdditionalView.HIDDEN -> {
                binding.viewTypeSelector.visibility = INVISIBLE
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
        CREATE_FOLDER, VIEW_TYPE, HIDDEN
    }
}
