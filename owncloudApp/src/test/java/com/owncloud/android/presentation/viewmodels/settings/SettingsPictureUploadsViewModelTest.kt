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

package com.owncloud.android.presentation.viewmodels.settings

import android.content.Intent
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE
import com.owncloud.android.db.PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.CameraUploadsHandlerProvider
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.ui.activity.UploadPathActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsPictureUploadsViewModelTest : ViewModelTest() {
    private lateinit var picturesViewModel: SettingsPictureUploadsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var cameraUploadsHandlerProvider: CameraUploadsHandlerProvider
    private lateinit var accountProvider: AccountProvider

    private val examplePath = "/Example/Path"
    private val exampleSourcePath = "/Example/Source/Path"
    private val exampleRemotePath = "/Example/Remote/Path"

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        cameraUploadsHandlerProvider = mockk(relaxUnitFun = true)
        accountProvider = mockk()

        picturesViewModel = SettingsPictureUploadsViewModel(
            preferencesProvider,
            cameraUploadsHandlerProvider,
            accountProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `is picture upload enabled - ok - true`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns true

        val pictureUploadEnabled = picturesViewModel.isPictureUploadEnabled()

        assertTrue(pictureUploadEnabled)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
        }
    }

    @Test
    fun `is picture upload enabled - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any()) } returns false

        val pictureUploadEnabled = picturesViewModel.isPictureUploadEnabled()

        assertFalse(pictureUploadEnabled)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
        }
    }

    @Test
    fun `set enable picture upload - ok - true`() {
        every { accountProvider.getCurrentOwnCloudAccount() } returns OC_ACCOUNT

        picturesViewModel.setEnablePictureUpload(true)

        verify(exactly = 1) {
            preferencesProvider.putString(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME, OC_ACCOUNT.name)
            preferencesProvider.putBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, true)
        }
    }

    @Test
    fun `set enable picture upload - ok - false`() {
        picturesViewModel.setEnablePictureUpload(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
            preferencesProvider.removePreference(key = PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME)
            preferencesProvider.removePreference(key = PREF__CAMERA_PICTURE_UPLOADS_PATH)
        }
    }

    @Test
    fun `update pictures last sync - ok`() {
        picturesViewModel.updatePicturesLastSync()

        verify(exactly = 1) {
            cameraUploadsHandlerProvider.updatePicturesLastSync(0)
        }
    }

    @Test
    fun `get picture uploads path - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns examplePath

        val uploadPath = picturesViewModel.getPictureUploadsPath()

        assertEquals(examplePath, uploadPath)

        verify(exactly = 1) {
            preferencesProvider.getString(
                PREF__CAMERA_PICTURE_UPLOADS_PATH,
                PREF__CAMERA_UPLOADS_DEFAULT_PATH
            )
        }
    }

    @Test
    fun `get picture uploads source path - ok`() {
        mockkStatic(PreferenceManager::class)

        every { preferencesProvider.getString(any(), any()) } returns exampleSourcePath
        every { PreferenceManager.getDefaultCameraSourcePath() } returns ""

        val uploadSourcePath = picturesViewModel.getPictureUploadsSourcePath()

        assertEquals(exampleSourcePath, uploadSourcePath)

        verify(exactly = 1) {
            preferencesProvider.getString(
                PREF__CAMERA_PICTURE_UPLOADS_SOURCE,
                PreferenceManager.getDefaultCameraSourcePath()
            )
        }
    }

    @Test
    fun `handle select picture uploads path - ok`() {
        val data: Intent = mockk()
        val ocFile: OCFile = mockk()

        every { ocFile.remotePath } returns exampleRemotePath
        every { data.getParcelableExtra<OCFile>(any()) } returns ocFile

        picturesViewModel.handleSelectPictureUploadsPath(data)

        verify(exactly = 1) {
            data.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
            ocFile.remotePath
            preferencesProvider.putString(PREF__CAMERA_PICTURE_UPLOADS_PATH, exampleRemotePath)
        }
    }

    @Test
    fun `handle select picture uploads path - ko - folder to upload is null`() {
        val data: Intent = mockk()

        every { data.getParcelableExtra<OCFile>(any()) } returns null

        picturesViewModel.handleSelectPictureUploadsPath(data)

        verify(exactly = 1) {
            data.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
        }
    }

    @Ignore("Needs to be fixed after local picker removal")
    @Test
    fun `handle select picture uploads source path - ok - source path hasn't changed`() {
        val data: Intent = mockk()
        mockkStatic(PreferenceManager::class)

        every { preferencesProvider.getString(any(), any()) } returns exampleSourcePath
        // It has to be "" for the test to pass
        every { PreferenceManager.getDefaultCameraSourcePath() } returns ""
        every { data.getStringExtra(any()) } returns exampleSourcePath

//        picturesViewModel.handleSelectPictureUploadsSourcePath(data)
//
//        verify(exactly = 2) {
//            data.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH)
//        }
        verify(exactly = 1) {
            preferencesProvider.putString(PREF__CAMERA_PICTURE_UPLOADS_SOURCE, exampleSourcePath)
        }
    }

    @Ignore("Needs to be fixed after local picker removal")
    @Test
    fun `handle select picture uploads source path - ok - source path has changed`() {
        val data: Intent = mockk()
        val sourcePath = "/New/Source/Path"
        mockkStatic(PreferenceManager::class)

        every { preferencesProvider.getString(any(), any()) } returns exampleSourcePath
        // It has to be "" for the test to pass
        every { PreferenceManager.getDefaultCameraSourcePath() } returns ""
        every { data.getStringExtra(any()) } returns sourcePath

//        picturesViewModel.handleSelectPictureUploadsSourcePath(data)

        every { preferencesProvider.getString(any(), any()) } returns sourcePath

        val newSourcePath = picturesViewModel.getPictureUploadsSourcePath()
        assertEquals(sourcePath, newSourcePath)

//        verify(exactly = 2) {
//            data.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH)
//        }
        verify(exactly = 1) {
            cameraUploadsHandlerProvider.updatePicturesLastSync(any())
            preferencesProvider.putString(PREF__CAMERA_PICTURE_UPLOADS_SOURCE, sourcePath)
        }
    }

    @Test
    fun `schedule picture uploads sync job - ok`() {
        picturesViewModel.schedulePictureUploadsSyncJob()

        verify(exactly = 1) {
            cameraUploadsHandlerProvider.schedulePictureUploadsSyncJob()
        }
    }
}
