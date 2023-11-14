package com.owncloud.android.files.details

import android.content.Context
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.presentation.files.details.FileDetailsFragment
import com.owncloud.android.presentation.files.details.FileDetailsViewModel
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.sharing.shares.ui.TestShareFileActivity
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AVAILABLE_OFFLINE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_WITHOUT_PERSONAL_SPACE
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.matchers.assertVisibility
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withDrawable
import com.owncloud.android.utils.matchers.withText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FileDetailsFragmentTest {

    private lateinit var fileDetailsViewModel: FileDetailsViewModel
    private lateinit var fileOperationsViewModel: FileOperationsViewModel
    private lateinit var context: Context

    private var currentFile: MutableStateFlow<OCFileWithSyncInfo?> = MutableStateFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE)
    private var currentFileWithoutPersonalSpace: MutableStateFlow<OCFileWithSyncInfo?> =
        MutableStateFlow(OC_FILE_WITH_SYNC_INFO_AND_WITHOUT_PERSONAL_SPACE)
    private var currentFileSyncInfo: MutableStateFlow<OCFileWithSyncInfo?> = MutableStateFlow(OC_FILE_WITH_SYNC_INFO)
    private var currentFileAvailableOffline: MutableStateFlow<OCFileWithSyncInfo?> = MutableStateFlow(OC_FILE_WITH_SYNC_INFO_AVAILABLE_OFFLINE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fileDetailsViewModel = mockk(relaxed = true)
        fileOperationsViewModel = mockk(relaxed = true)
        every { fileDetailsViewModel.currentFile } returns currentFile
        stopKoin()

        startKoin {
            context
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        fileDetailsViewModel
                    }
                    viewModel {
                        fileOperationsViewModel
                    }
                }
            )

        }

        val fileDetailsFragment = FileDetailsFragment.newInstance(
            OC_FILE,
            OC_ACCOUNT,
            syncFileAtOpen = false
        )

        launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(fileDetailsFragment)
        }
    }

    @Test
    fun display_visibility_of_detail_view_when_it_is_displayed() {
        assertViewsDisplayed()
    }

    @Test
    fun show_space_personal_when_it_has_value() {
        R.id.fdSpace.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        R.id.fdSpaceLabel.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        R.id.fdIconSpace.assertVisibility(ViewMatchers.Visibility.VISIBLE)

        R.id.fdSpace.withText(R.string.bottom_nav_personal)
        R.id.fdSpaceLabel.withText(R.string.space_label)
        onView(withId(R.id.fdIconSpace))
            .check(matches(withDrawable(R.drawable.ic_spaces)))
    }

    @Test
    fun hide_space_when_it_has_no_value() {
        every { fileDetailsViewModel.currentFile } returns currentFileSyncInfo

        R.id.fdSpace.assertVisibility(ViewMatchers.Visibility.GONE)
        R.id.fdSpaceLabel.assertVisibility(ViewMatchers.Visibility.GONE)
        R.id.fdIconSpace.assertVisibility(ViewMatchers.Visibility.GONE)
    }

    @Test
    fun show_space_not_personal_when_it_has_value() {
        every { fileDetailsViewModel.currentFile } returns currentFileWithoutPersonalSpace

        R.id.fdSpace.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        R.id.fdSpaceLabel.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        R.id.fdIconSpace.assertVisibility(ViewMatchers.Visibility.VISIBLE)

        R.id.fdSpace.withText(currentFileWithoutPersonalSpace.value?.space?.name.toString())
        R.id.fdSpaceLabel.withText(R.string.space_label)
        onView(withId(R.id.fdIconSpace))
            .check(matches(withDrawable(R.drawable.ic_spaces)))
    }

    @Test
    fun show_last_sync_when_it_has_value() {
        currentFile.value?.file?.lastSyncDateForData = 1212121212212
        R.id.fdLastSync.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        R.id.fdLastSyncLabel.assertVisibility(ViewMatchers.Visibility.VISIBLE)

        R.id.fdLastSyncLabel.withText(R.string.filedetails_last_sync)
        R.id.fdLastSync.withText(DisplayUtils.unixTimeToHumanReadable(currentFile.value?.file?.lastSyncDateForData!!))
    }

    @Test
    fun hide_last_sync_when_it_has_no_value() {
        every { fileDetailsViewModel.currentFile } returns currentFile

        R.id.fdLastSync.assertVisibility(ViewMatchers.Visibility.GONE)
        R.id.fdLastSyncLabel.assertVisibility(ViewMatchers.Visibility.GONE)
    }

    @Test
    fun verifyTests() {
        R.id.fdCreatedLabel.withText(R.string.filedetails_created)
        R.id.fdCreated.withText(DisplayUtils.unixTimeToHumanReadable(currentFile.value?.file?.creationTimestamp!!))

        R.id.fdModifiedLabel.withText(R.string.filedetails_modified)
        R.id.fdModified.withText(DisplayUtils.unixTimeToHumanReadable(currentFile.value?.file?.modificationTimestamp!!))

        R.id.fdPathLabel.withText(R.string.ssl_validator_label_L)
        R.id.fdPath.withText(currentFile.value?.file?.getParentRemotePath()!!)

        R.id.fdname.withText(currentFile.value?.file?.fileName!!)
    }

    @Test
    fun badge_available_offline_in_image_is_not_viewed_when_file_does_not_change_state() {
        every { fileDetailsViewModel.currentFile } returns currentFileAvailableOffline

        R.id.badgeDetailFile.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        onView(withId(R.id.badgeDetailFile))
            .check(matches(withDrawable(R.drawable.offline_available_pin)))

    }

    @Test
    fun show_badge_isAvailableLocally_in_image_when_file_change_state() {
        currentFile.value?.file?.etagInConflict = "error"

        R.id.badgeDetailFile.assertVisibility(ViewMatchers.Visibility.VISIBLE)
        onView(withId(R.id.badgeDetailFile))
            .check(matches(withDrawable(R.drawable.error_pin)))
    }

    private fun assertViewsDisplayed(
        showImage: Boolean = true,
        showFdName: Boolean = true,
        showFdProgressText: Boolean = false,
        showFdProgressBar: Boolean = false,
        showFdCancelBtn: Boolean = false,
        showDivider: Boolean = true,
        showDivider2: Boolean = true,
        showFdTypeLabel: Boolean = true,
        showFdType: Boolean = true,
        showFdSizeLabel: Boolean = true,
        showFdSize: Boolean = true,
        showFdModifiedLabel: Boolean = true,
        showFdModified: Boolean = true,
        showFdCreatedLabel: Boolean = true,
        showFdCreated: Boolean = true,
        showDivider3: Boolean = true,
        showFdPathLabel: Boolean = true,
        showFdPath: Boolean = true
    ) {
        R.id.fdImageDetailFile.isDisplayed(displayed = showImage)
        R.id.fdname.isDisplayed(displayed = showFdName)
        R.id.fdProgressText.isDisplayed(displayed = showFdProgressText)
        R.id.fdProgressBar.isDisplayed(displayed = showFdProgressBar)
        R.id.fdCancelBtn.isDisplayed(displayed = showFdCancelBtn)
        R.id.divider.isDisplayed(displayed = showDivider)
        R.id.fdTypeLabel.isDisplayed(displayed = showFdTypeLabel)
        R.id.fdType.isDisplayed(displayed = showFdType)
        R.id.fdSizeLabel.isDisplayed(displayed = showFdSizeLabel)
        R.id.fdSize.isDisplayed(displayed = showFdSize)
        R.id.divider2.isDisplayed(displayed = showDivider2)
        R.id.fdModifiedLabel.isDisplayed(displayed = showFdModifiedLabel)
        R.id.fdModified.isDisplayed(displayed = showFdModified)
        R.id.fdCreatedLabel.isDisplayed(displayed = showFdCreatedLabel)
        R.id.fdCreated.isDisplayed(displayed = showFdCreated)
        R.id.divider3.isDisplayed(displayed = showDivider3)
        R.id.fdPathLabel.isDisplayed(displayed = showFdPathLabel)
        R.id.fdPath.isDisplayed(displayed = showFdPath)
    }
}
