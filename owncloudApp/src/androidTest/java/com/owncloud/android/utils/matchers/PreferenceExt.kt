/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.utils.matchers

import androidx.preference.Preference
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Assert.assertEquals

fun Preference.verifyPreference(
    keyPref: String,
    titlePref: String,
    summaryPref: String? = null,
    visible: Boolean,
    enabled: Boolean? = null
) {
    if (visible) onView(withText(titlePref)).check(matches(isDisplayed()))
    summaryPref?.let {
        if (visible) onView(withText(it)).check(matches(isDisplayed()))
        assertEquals(it, summary)
    }
    assertEquals(keyPref, key)
    assertEquals(titlePref, title)
    assertEquals(visible, isVisible)
    enabled?.let {
        assertEquals(enabled, isEnabled)
    }
}
