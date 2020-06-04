/**
 * ownCloud Android client application
 *
 * @author José Carlos Montes Martos
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.testutil.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

inline fun <reified T> LiveData<T>.getEmittedValues(
    expectedSize: Int,
    timeout: Long = TIMEOUT_TEST_SHORT,
    crossinline onObserved: () -> Unit = {}
): List<T?> {
    val currentValue = AtomicInteger(0)
    val data = arrayOfNulls<T>(expectedSize)
    val latch = CountDownLatch(expectedSize)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data[currentValue.getAndAdd(1)] = o
            if (currentValue.get() == expectedSize) {
                removeObserver(this)
            }
            latch.countDown()

        }
    }
    observeForever(observer)
    onObserved()
    latch.await(timeout, TimeUnit.MILLISECONDS)

    return data.toList()
}

inline fun <reified T> LiveData<T>.getLastEmittedValue(
    crossinline onObserved: () -> Unit = {}
): T? = getEmittedValues(expectedSize = 1, onObserved = onObserved).firstOrNull()
