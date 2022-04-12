package com.owncloud.android.settings.advanced

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.SwitchPreferenceCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.presentation.ui.settings.fragments.SettingsAdvancedFragment
import com.owncloud.android.presentation.ui.settings.fragments.SettingsAdvancedFragment.Companion.PREF_SHOW_HIDDEN_FILES
import com.owncloud.android.presentation.viewmodels.settings.SettingsAdvancedViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsAdvancedFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsAdvancedFragment>

    private var prefShowHiddenFiles: SwitchPreferenceCompat? = null

    private lateinit var advancedViewModel: SettingsAdvancedViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        advancedViewModel = mockk(relaxUnitFun = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        advancedViewModel
                    }
                }
            )
        }

        Intents.init()

        every { advancedViewModel.isHiddenFilesShown() } returns true

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
    }

    @After
    fun tearDown() {
        Intents.release()
        unmockkAll()
    }

    @Test
    fun advancedView() {
        prefShowHiddenFiles = getPreference(PREF_SHOW_HIDDEN_FILES)
        assertNotNull(prefShowHiddenFiles)
        prefShowHiddenFiles?.verifyPreference(
            keyPref = PREF_SHOW_HIDDEN_FILES,
            titlePref = context.getString(R.string.prefs_show_hidden_files),
            visible = true,
            enabled = true
        )
    }

    @Test
    fun disableShowHiddenFiles() {
        prefShowHiddenFiles = getPreference(PREF_SHOW_HIDDEN_FILES)
        prefShowHiddenFiles?.isChecked = advancedViewModel.isHiddenFilesShown()

        onView(withText(context.getString(R.string.prefs_show_hidden_files))).perform(click())

        prefShowHiddenFiles?.isChecked?.let { assertFalse(it) }
    }

    private fun getPreference(key: String): SwitchPreferenceCompat? {
        var preference: SwitchPreferenceCompat? = null
        fragmentScenario.onFragment { fragment ->
            preference = fragment.findPreference(key)
        }
        return preference
    }
}