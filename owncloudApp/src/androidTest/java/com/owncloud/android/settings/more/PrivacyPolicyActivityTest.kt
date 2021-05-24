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
package com.owncloud.android.settings.more

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.PrivacyPolicyActivity
import com.owncloud.android.utils.matchers.isDisplayed
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PrivacyPolicyActivityTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(PrivacyPolicyActivity::class.java)

    private val introductionID = "et-main-area"
    private val introductionText = "Imprint"

    @After
    fun tearDown() {
        onWebView().reset()
    }

    @Test
    fun checkTitle() {
        onView(withText(R.string.actionbar_privacy_policy)).check(matches(isDisplayed()))
    }

    @Test
    fun checkProgressbar() {
        R.id.syncProgressBar.isDisplayed(true)
    }

    @Test
    fun privacyPolicyLoaded() {
        onWebView().withElement(findElement(Locator.ID, introductionID))
            .check(webMatches(getText(), containsString(introductionText)))
    }
}
