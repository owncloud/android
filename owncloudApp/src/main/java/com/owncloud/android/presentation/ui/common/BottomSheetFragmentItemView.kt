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
package com.owncloud.android.presentation.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import kotlinx.android.synthetic.main.bottom_sheet_fragment_item.view.*

class BottomSheetFragmentItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var itemIcon: Drawable?
        get() = item_icon.drawable
        set(value) {
            item_icon.setImageDrawable(value)
        }

    var title: CharSequence?
        get() = item_title.text
        set(value) {
            item_title.text = value
        }

    var itemAdditionalIcon: Drawable?
        get() = item_additional_icon.drawable
        set(value) {
            item_additional_icon.setImageDrawable(value)
        }

    init {
        View.inflate(context, R.layout.bottom_sheet_fragment_item, this)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.BottomSheetFragmentItemView, 0, 0)
        try {
            itemIcon = a.getDrawable(R.styleable.BottomSheetFragmentItemView_itemIcon)
            title = a.getString(R.styleable.BottomSheetFragmentItemView_title)
        } finally {
            a.recycle()
        }
    }

    fun setSelected(iconAdditional: Int) {
        itemAdditionalIcon = ContextCompat.getDrawable(context, iconAdditional)
        val selectedColor = ContextCompat.getColor(context, R.color.primary)
        item_icon?.setColorFilter(selectedColor)
        item_title.setTextColor(selectedColor)
        item_additional_icon.setColorFilter(selectedColor)
    }
}
