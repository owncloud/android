package com.owncloud.android.utils;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.owncloud.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import android.content.Context;

/**
 *   ownCloud Android client application
 *
 *   @author Jesús Recio @jesmrec
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


public class FileManager {

    //To select an option in files view
    public static void selectOptionActionsMenu (Context context, int option) {
        String optionSelected = context.getResources().getString(option);
        if (!new UiObject(new UiSelector().description(optionSelected)).exists()) {
            onView(allOf(withContentDescription("More options"),
                    isDescendantOfA(withId(R.id.toolbar)))).perform(click());
            switch (option) {
                case R.string.action_share:
                    onView(withId(R.id.action_share_file)).perform(click());
                    break;
                default:
                    break;
            }

        } else {
            switch (option) {
                case R.string.action_share:
                    onView(withId(R.id.action_share_file)).perform(click());
                    break;
                default:
                    break;
            }
        }
    }


}
