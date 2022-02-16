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

package com.owncloud.android.utils

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId

fun Int.typeText(text: String) {
    onView(withId(this)).perform(scrollTo(), ViewActions.typeText(text))
}

fun Int.replaceText(text: String) {
    onView(withId(this)).perform(scrollTo(), ViewActions.replaceText(text))
}

fun Int.scrollAndClick() {
    onView(withId(this)).perform(scrollTo(), ViewActions.click())
}

fun Int.click() {
    onView(withId(this)).perform(ViewActions.click())
}
