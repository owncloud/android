/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop

fun View.setAccessibilityRole(className: Class<*>? = null, roleDescription: String? = null) {
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(v, info)
            className?.let { info.className = it.name }
            roleDescription?.let { info.roleDescription = it }
        }
    })
}

/**
 * Applies system status bar insets (padding or margin) to this [View].
 *
 * This ensures the view is laid out correctly under the status bar by
 * either updating its padding or margins with the system bar insets,
 * while preserving the original values.
 *
 * @param usePaddings If `true` (default), status bar insets are added to
 * the view’s padding. If `false`, they are added to the view’s margins.
 */
fun View.applyStatusBarInsets(usePaddings: Boolean = true) {
    // Cache original padding once
    val initialPaddingTop = paddingTop
    val initialPaddingLeft = paddingLeft
    val initialPaddingRight = paddingRight

    val initialMarginTop = marginTop
    val initialMarginLeft = marginLeft
    val initialMarginRight = marginRight

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        if (usePaddings) {
            view.setPadding(
                initialPaddingLeft + statusBarInsets.left,
                initialPaddingTop + statusBarInsets.top,
                initialPaddingRight + statusBarInsets.right,
                paddingBottom,
            )
        } else {
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.apply { 
                topMargin = initialMarginTop + statusBarInsets.top
                leftMargin = initialMarginLeft + statusBarInsets.left
                rightMargin = initialMarginRight + statusBarInsets.right
            }
        }
        insets
    }
}
