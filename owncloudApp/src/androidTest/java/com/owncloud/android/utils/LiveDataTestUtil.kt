/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.owncloud.android.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object LiveDataTestUtil {

    /**
     * Get the value from a LiveData object. We're waiting for LiveData to emit, for 2 seconds.
     * Once we got a notification via onChanged, we stop observing.
     */
    inline fun <reified T> LiveData<T>.getOrAwaitValues(
        expectedValues: Int = 1
    ): List<T?> {
        var currentValue: Int = 0
        val data = arrayOfNulls<T>(expectedValues)
        val latch = CountDownLatch(expectedValues)
        val observer = object : Observer<T> {
            override fun onChanged(o: T?) {
                data[currentValue] = o
                currentValue++
                latch.countDown()
                if (currentValue == expectedValues) {
                    this@getOrAwaitValues.removeObserver(this)
                }
            }
        }
        this.observeForever(observer)
        latch.await(TIMEOUT_TEST_SHORT, TimeUnit.MILLISECONDS)

        @Suppress("UNCHECKED_CAST")
        return data.toList()
    }
}
