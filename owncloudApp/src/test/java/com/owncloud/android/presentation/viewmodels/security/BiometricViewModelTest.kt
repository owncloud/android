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

package com.owncloud.android.presentation.viewmodels.security

import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.security.PREFERENCE_LAST_UNLOCK_TIMESTAMP
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BiometricViewModelTest : ViewModelTest() {
    private lateinit var biometricViewModel: BiometricViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        biometricViewModel = BiometricViewModel(preferencesProvider)
    }

    @Test
    fun `set last unlock timestamp - ok`() {
        biometricViewModel.setLastUnlockTimestamp()

        verify(exactly = 1) {
            preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, any())
        }
    }
}
