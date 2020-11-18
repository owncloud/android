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
package com.owncloud.android.utils.matchers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

fun withDrawable(
    @DrawableRes id: Int,
    @ColorRes tint: Int? = null,
    tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("ImageView with drawable same as drawable with id $id")
        tint?.let { description.appendText(", tint color id: $tint, mode: $tintMode") }
    }

    override fun matchesSafely(view: View): Boolean {
        val context = view.context
        val tintColor = tint?.toColor(context)
        val expectedBitmap = context.getDrawable(id)?.tinted(tintColor, tintMode)?.toBitmap()

        return view is ImageView && view.drawable.toBitmap().sameAs(expectedBitmap)
    }
}

private fun Int.toColor(context: Context) = ContextCompat.getColor(context, this)

private fun Drawable.tinted(@ColorInt tintColor: Int? = null, tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) =
    apply {
        setTintList(tintColor?.toColorStateList())
        setTintMode(tintMode)
    }

private fun Int.toColorStateList() = ColorStateList.valueOf(this)
