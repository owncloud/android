/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.utils.matchers

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Description

fun withChildViewCount(count: Int, childMatcher: Matcher<View>): Matcher<View> {
    return object : BoundedMatcher<View, ViewGroup>(ViewGroup::class.java) {
        override fun matchesSafely(viewGroup: ViewGroup): Boolean {
            var matchCount = 0
            for (i in 0 until viewGroup.childCount) {
                if (childMatcher.matches(viewGroup.getChildAt(i))) {
                    matchCount++
                }
            }
            return matchCount == count
        }

        override fun describeTo(description: Description?) {
            description?.appendText("ViewGroup with child-count=$count and")
            childMatcher.describeTo(description)
        }
    }
}

fun nthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            if (view.parent !is ViewGroup) {
                return parentMatcher.matches(view.parent)
            }
            val group = view.parent as ViewGroup
            return parentMatcher.matches(view.parent) && group.getChildAt(childPosition) == view
        }

        override fun describeTo(description: Description) {
            description.appendText("with $childPosition child view of type parentMatcher")
        }
    }
}
