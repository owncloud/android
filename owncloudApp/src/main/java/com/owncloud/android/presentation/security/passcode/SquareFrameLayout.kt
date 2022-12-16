/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
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
 */

package com.owncloud.android.presentation.security.passcode

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

/**
 * [android.widget.FrameLayout] which forces itself to be laid out as square, without deformations, adapting itself at every screen sizes.
 * https://developer.android.com/samples/ActivitySceneTransitionBasic/src/com.example.android.activityscenetransitionbasic/SquareFrameLayout.html#l26
 */
class SquareFrameLayout(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthSize == 0 && heightSize == 0) {
            // If there are no constraints on size, let FrameLayout measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            // Now use the smallest of the measured dimensions for both dimensions
            val minSize = min(measuredWidth, measuredHeight)
            setMeasuredDimension(minSize, minSize)
            return
        }

        val size = if (widthSize == 0 || heightSize == 0) {
            // If one of the dimensions has no restriction on size, set both dimensions to be the
            // on that does
            max(widthSize, heightSize)
        } else {
            // Both dimensions have restrictions on size, set both dimensions to be the
            // smallest of the two
            min(widthSize, heightSize)
        }

        val newMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(newMeasureSpec, newMeasureSpec)
    }
}
