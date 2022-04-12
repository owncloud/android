package com.owncloud.android.presentation.viewmodels.settings

import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.settings.fragments.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsAdvancedViewModelTest : ViewModelTest() {
    private lateinit var advancedViewModel: SettingsAdvancedViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider

    @Before
    fun setUp() {
        preferencesProvider = mockk()

        advancedViewModel = SettingsAdvancedViewModel(preferencesProvider)
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `is hidden files shown - ok - true`() {
        every { preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any()) } returns true

        val shown = advancedViewModel.isHiddenFilesShown()

        Assert.assertTrue(shown)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any())
        }
    }

    @Test
    fun `is hidden files shown - ok - false`() {
        every { preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any()) } returns false

        val shown = advancedViewModel.isHiddenFilesShown()

        Assert.assertFalse(shown)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, any())
        }
    }
}