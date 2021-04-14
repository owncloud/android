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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers

fun Int.isDisplayed(displayed: Boolean) {
    val displayMatcher = if (displayed) ViewMatchers.isDisplayed() else CoreMatchers.not(ViewMatchers.isDisplayed())

    onView(withId(this))
        .check(matches(displayMatcher))
}

fun Int.isEnabled(enabled: Boolean) {
    val enableMatcher = if (enabled) ViewMatchers.isEnabled() else CoreMatchers.not(ViewMatchers.isEnabled())

    onView(withId(this))
        .check(matches(enableMatcher))
}

fun Int.isFocusable(focusable: Boolean) {
    val focusableMatcher = if (focusable) ViewMatchers.isFocusable() else CoreMatchers.not(ViewMatchers.isFocusable())

    onView(withId(this))
        .check(matches(focusableMatcher))
}

fun Int.withText(text: String) {
    onView(withId(this))
        .check(matches(ViewMatchers.withText(text)))
}

fun Int.withText(resourceId: Int) {
    onView(withId(this))
        .check(matches(ViewMatchers.withText(resourceId)))
}

fun Int.withTextColor(resourceId: Int) {
    onView(withId(this))
        .check(matches(ViewMatchers.hasTextColor(resourceId)))
}

fun Int.assertVisibility(visibility: ViewMatchers.Visibility) {
    onView(withId(this))
        .check(matches(ViewMatchers.withEffectiveVisibility(visibility)))
}
