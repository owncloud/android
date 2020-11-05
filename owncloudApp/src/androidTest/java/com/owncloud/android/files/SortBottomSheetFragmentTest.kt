/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
 * Copyright (C) 2020 ownCloud GmbH.
 *
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
package com.owncloud.android.files

import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.files.SortBottomSheetFragment
import com.owncloud.android.presentation.ui.files.SortOrder
import com.owncloud.android.presentation.ui.files.SortType
import com.owncloud.android.utils.matchers.bsfItemWithIcon
import com.owncloud.android.utils.matchers.bsfItemWithTitle
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withText
import org.junit.Before
import org.junit.Test

class SortBottomSheetFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SortBottomSheetFragment>

    @Before
    fun setUp() {
        val fragmentArgs = Bundle().apply {
            putParcelable(SortBottomSheetFragment.ARG_SORT_TYPE, SortType.SORT_TYPE_BY_NAME)
            putParcelable(SortBottomSheetFragment.ARG_SORT_ORDER, SortOrder.SORT_ORDER_ASCENDING)
        }
        fragmentScenario = launchFragment<SortBottomSheetFragment>(fragmentArgs)
    }

    @Test
    fun test_initial_view() {
        R.id.title.run {
            isDisplayed(true)
            withText(R.string.actionbar_sort_title)
        }
        R.id.sort_by_name.run {
            isDisplayed(true)
            bsfItemWithTitle(R.string.global_name, R.color.primary)
            bsfItemWithIcon(R.drawable.ic_sort_by_name, R.color.primary)
        }
        R.id.sort_by_size.run {
            isDisplayed(true)
            bsfItemWithTitle(R.string.global_size, R.color.bottom_sheet_fragment_item_color)
            bsfItemWithIcon(R.drawable.ic_sort_by_size, R.color.bottom_sheet_fragment_item_color)
        }
        R.id.sort_by_date.run {
            isDisplayed(true)
            bsfItemWithTitle(R.string.global_date, R.color.bottom_sheet_fragment_item_color)
            bsfItemWithIcon(R.drawable.ic_sort_by_date, R.color.bottom_sheet_fragment_item_color)
        }
    }
}
