/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.testutil.livedata.getEmittedValues
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule

@ExperimentalCoroutinesApi
open class ViewModelTest {

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    val testCoroutineDispatcher = TestCoroutineDispatcher()
    val coroutineDispatcherProvider: CoroutinesDispatcherProvider = CoroutinesDispatcherProvider(
        io = testCoroutineDispatcher,
        main = testCoroutineDispatcher,
        computation = testCoroutineDispatcher
    )

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()

        unmockkAll()
    }

    fun <DomainModel> assertEmittedValues(
        expectedValues: List<Event<UIResult<DomainModel>>>,
        liveData: LiveData<Event<UIResult<DomainModel>>>
    ) {
        val emittedValues = liveData.getEmittedValues(expectedValues.size) {
            testCoroutineDispatcher.resumeDispatcher()
        }
        assertEquals(expectedValues, emittedValues)
    }

}
