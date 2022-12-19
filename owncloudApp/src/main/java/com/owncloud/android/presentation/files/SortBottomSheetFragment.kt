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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.owncloud.android.databinding.SortBottomSheetFragmentBinding
import com.owncloud.android.utils.PreferenceUtils

class SortBottomSheetFragment : BottomSheetDialogFragment() {
    var sortDialogListener: SortDialogListener? = null

    lateinit var sortType: SortType
    lateinit var sortOrder: SortOrder

    private var _binding: SortBottomSheetFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortType = arguments?.getParcelable(ARG_SORT_TYPE) ?: SortType.SORT_TYPE_BY_NAME
        sortOrder = arguments?.getParcelable(ARG_SORT_ORDER) ?: SortOrder.SORT_ORDER_ASCENDING
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SortBottomSheetFragmentBinding.inflate(inflater, container, false)
        return binding.root.apply {
            // Allow or disallow touches with other visible windows
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (sortType) {
            SortType.SORT_TYPE_BY_NAME -> binding.sortByName.setSelected(sortOrder.toDrawableRes())
            SortType.SORT_TYPE_BY_SIZE -> binding.sortBySize.setSelected(sortOrder.toDrawableRes())
            SortType.SORT_TYPE_BY_DATE -> binding.sortByDate.setSelected(sortOrder.toDrawableRes())
        }

        binding.sortByName.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_NAME) }
        binding.sortBySize.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_SIZE) }
        binding.sortByDate.setOnClickListener { onSortClick(SortType.SORT_TYPE_BY_DATE) }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onStart() {
        super.onStart()

        // Show bottom sheet expanded even in landscape, since there are just 3 options at the moment.
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
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
            val args = Bundle().apply {
                putParcelable(ARG_SORT_TYPE, sortType)
                putParcelable(ARG_SORT_ORDER, sortOrder)
            }
            return SortBottomSheetFragment().apply { arguments = args }
        }
    }
}
