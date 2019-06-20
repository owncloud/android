/*
 * ownCloud Android client application
 *
 * @author Jesus Recio (@jesmrec)
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.settings.security

import android.content.Intent
import android.os.Parcelable
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class OCSettingsPasscode {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(PassCodeActivity::class.java, true, false)

    val intent = Intent()

    @Before
    fun setUp() {

    }

    @After
    fun tearDown() {

    }


    @Test
    fun passcodeView(){
        intent.setAction(PassCodeActivity.ACTION_REQUEST_WITH_RESULT)
        activityRule.launchActivity(intent)

        onView(withId(R.id.header)).check(matches(isDisplayed()))
        onView(withId(R.id.explanation)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_configure_your_pass_code)).check(matches(isDisplayed()))
        onView(withText(R.string.pass_code_configure_your_pass_code_explanation)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel)).check(matches(isDisplayed()))
    }

}