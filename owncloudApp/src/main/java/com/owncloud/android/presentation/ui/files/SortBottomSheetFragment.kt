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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.owncloud.android.R
import kotlinx.android.synthetic.main.sort_bottom_sheet_fragment.*

class SortBottomSheetFragment : BottomSheetDialogFragment() {
    var sortDialogListener: SortDialogListener? = null

    lateinit var sortType: SortType
    lateinit var sortOrder: SortOrder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortType = arguments?.getParcelable(ARG_SORT_TYPE) ?: SortType.SORT_TYPE_BY_NAME
        sortOrder = arguments?.getParcelable(ARG_SORT_ORDER) ?: SortOrder.SORT_ORDER_ASCENDING
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.sort_bottom_sheet_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show bottom sheet expanded even in landscape, since there are just 3 options at the moment.
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        when (sortType) {
            SortType.SORT_TYPE_BY_NAME -> sort_by_name.setSelected(sortOrder.toDrawableRes())
            SortType.SORT_TYPE_BY_SIZE -> sort_by_size.setSelected(sortOrder.toDrawableRes())
            SortType.SORT_TYPE_BY_DATE -> sort_by_date.setSelected(sortOrder.toDrawableRes())
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
        const val ARG_SORT_TYPE = "ARG_SORT_TYPE"
        const val ARG_SORT_ORDER = "ARG_SORT_ORDER"

        fun newInstance(
            sortType: SortType,
            sortOrder: SortOrder
        ): SortBottomSheetFragment {
            val fragment = SortBottomSheetFragment()
            val args = Bundle()
            args.putParcelable(ARG_SORT_TYPE, sortType)
            args.putParcelable(ARG_SORT_ORDER, sortOrder)
            fragment.arguments = args
            return fragment
        }
    }
}
