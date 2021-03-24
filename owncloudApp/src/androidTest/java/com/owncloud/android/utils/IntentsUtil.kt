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

package com.owncloud.android.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction

fun mockIntent(
    extras: Pair<String, String>,
    resultCode: Int = Activity.RESULT_OK,
    action: String
) {
    val result = Intent()
    result.putExtra(extras.first, extras.second)
    val intentResult = Instrumentation.ActivityResult(resultCode, result)
    intending(hasAction(action)).respondWith(intentResult)
}

@JvmName("mockIntentBoolean")
fun mockIntent(
    extras: Pair<String, Boolean>,
    resultCode: Int = Activity.RESULT_OK,
    action: String
) {
    val result = Intent()
    result.putExtra(extras.first, extras.second)
    val intentResult = Instrumentation.ActivityResult(resultCode, result)
    intending(hasAction(action)).respondWith(intentResult)
}

@JvmName("mockIntentNoExtras")
fun mockIntent(
    resultCode: Int = Activity.RESULT_OK,
    action: String
) {
    val result = Intent()
    val intentResult = Instrumentation.ActivityResult(resultCode, result)
    intending(hasAction(action)).respondWith(intentResult)
}
