/**
 * ownCloud Android client application
 *
 * @author Jesús Recio @jesmrec
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

package com.owncloud.android.settings.logs

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.LogHistoryActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

class LogHistoryActivityTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(LogHistoryActivity::class.java)

    @Test
    fun itemsToolbar() {
        onView(
            allOf(withId(R.id.search_button), isDescendantOfA(withId(R.id.standard_toolbar)))
        ).check(
            matches(isDisplayed())
        )
    }
}
