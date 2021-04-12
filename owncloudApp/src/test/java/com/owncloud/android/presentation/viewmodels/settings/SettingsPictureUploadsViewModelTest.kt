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

import android.os.Environment
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.CameraUploadsHandlerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class SettingsPictureUploadsViewModelTest : ViewModelTest() {
    private lateinit var picturesViewModel: SettingsPictureUploadsViewModel
    private lateinit var preferencesProvider: SharedPreferencesProvider
    private lateinit var cameraUploadsHandlerProvider: CameraUploadsHandlerProvider

    private val examplePath = "/Example/Path"
    private val exampleSourcePath = "/Example/Source/Path"

    @Before
    fun setUp() {
        preferencesProvider = mockk(relaxUnitFun = true)
        cameraUploadsHandlerProvider = mockk(relaxUnitFun = true)

        picturesViewModel = SettingsPictureUploadsViewModel(
            preferencesProvider,
            cameraUploadsHandlerProvider
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
            preferencesProvider.getBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
        }
    }

    @Test
    fun `is picture upload enabled - ok - false`() {
        every { preferencesProvider.getBoolean(any(), any())} returns false

        val pictureUploadEnabled = picturesViewModel.isPictureUploadEnabled()

        assertFalse(pictureUploadEnabled)

        verify(exactly = 1) {
            preferencesProvider.getBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
        }
    }

    @Test
    fun `set enable picture upload - ok - true`() {
        picturesViewModel.setEnablePictureUpload(true)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, true)
        }
    }

    @Test
    fun `set enable picture upload - ok - false`() {
        picturesViewModel.setEnablePictureUpload(false)

        verify(exactly = 1) {
            preferencesProvider.putBoolean(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
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
    fun `load picture uploads path - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns examplePath

        picturesViewModel.loadPictureUploadsPath()

        verify(exactly = 1) {
            preferencesProvider.getString(
                PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH,
                PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH
            )
        }
    }

    @Test
    fun `get picture uploads path - ok - no load before`() {
        val uploadPath = picturesViewModel.getPictureUploadsPath()

        assertNull(uploadPath)
    }

    @Test
    fun `get picture uploads path - ok - load before`() {
        every { preferencesProvider.getString(any(), any()) } returns examplePath

        picturesViewModel.loadPictureUploadsPath()
        val uploadPath = picturesViewModel.getPictureUploadsPath()

        assertEquals(examplePath, uploadPath)
    }

    @Test
    fun `load picture uploads source path - ok`() {
        every { preferencesProvider.getString(any(), any()) } returns exampleSourcePath

        picturesViewModel.loadPictureUploadsSourcePath()
    }

    @Test
    fun `get picture uploads source path - ok - no load before`() {
        val uploadSourcePath = picturesViewModel.getPictureUploadsSourcePath()

        assertNull(uploadSourcePath)
    }

    @Test
    fun `get picture uploads source path - ok - load before`() {
        mockkStatic(PreferenceManager.CameraUploadsConfiguration::class)

        every { preferencesProvider.getString(any(), any()) } returns exampleSourcePath
        every { PreferenceManager.CameraUploadsConfiguration.DEFAULT_SOURCE_PATH } returns ""

        picturesViewModel.loadPictureUploadsSourcePath()
        val uploadSourcePath = picturesViewModel.getPictureUploadsSourcePath()

        assertEquals(exampleSourcePath, uploadSourcePath)
    }
}
