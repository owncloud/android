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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.owncloud.android.R
import kotlinx.android.synthetic.main.sort_bottom_sheet_fragment.*

class SortBottomSheetFragment(
    private val sortType: SortType,
    private val sortOrder: SortOrder
) : BottomSheetDialogFragment() {
    var sortDialogListener: SortDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.sort_bottom_sheet_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (sortType) {
            SortType.SORT_TYPE_BY_NAME -> sort_by_name.apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                setCompoundDrawablesWithIntrinsicBounds(sortOrder.toDrawableRes(), 0, 0, 0)
            }
            SortType.SORT_TYPE_BY_SIZE -> sort_by_size.apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                setCompoundDrawablesWithIntrinsicBounds(sortOrder.toDrawableRes(), 0, 0, 0)
            }
            SortType.SORT_TYPE_BY_DATE -> sort_by_date.apply {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                setCompoundDrawablesWithIntrinsicBounds(sortOrder.toDrawableRes(), 0, 0, 0)
            }
        }

        sort_by_name.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_NAME) }
        sort_by_size.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_SIZE) }
        sort_by_date.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_DATE) }
    }

    private fun onSortClick(sortType: SortType) {
        sortDialogListener?.onSortSelected(sortType)
        dismiss()
    }

    interface SortDialogListener {
        fun onSortSelected(sortType: SortType)
    }

    companion object {
        const val TAG = "SortBottomSheetFragment"
    }
}
