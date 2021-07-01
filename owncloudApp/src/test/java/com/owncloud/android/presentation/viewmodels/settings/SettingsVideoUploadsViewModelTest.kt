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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetVideoUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetVideoUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SaveVideoUploadsConfigurationUseCase
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.AccountProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.ui.activity.UploadPathActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.core.context.stopKoin

@Ignore("TODO")
@ExperimentalCoroutinesApi
class SettingsVideoUploadsViewModelTest : ViewModelTest() {
    private lateinit var videosViewModel: SettingsVideoUploadsViewModel
    private lateinit var accountProvider: AccountProvider
    private lateinit var workManagerProvider: WorkManagerProvider

    private lateinit var saveVideoUploadsConfigurationUseCase: SaveVideoUploadsConfigurationUseCase
    private lateinit var getVideoUploadsConfigurationStreamUseCase: GetVideoUploadsConfigurationStreamUseCase
    private lateinit var resetVideoUploadsUseCase: ResetVideoUploadsUseCase

    private val examplePath = "/Example/Path"
    private val exampleSourcePath = "/Example/Source/Path"
    private val exampleRemotePath = "/Example/Remote/Path"

    @Before
    fun setUp() {
        accountProvider = mockk()
        workManagerProvider = mockk()
        saveVideoUploadsConfigurationUseCase = mockk()
        getVideoUploadsConfigurationStreamUseCase = mockk()
        resetVideoUploadsUseCase = mockk()

        videosViewModel = SettingsVideoUploadsViewModel(
            accountProvider,
            saveVideoUploadsConfigurationUseCase,
            getVideoUploadsConfigurationStreamUseCase,
            resetVideoUploadsUseCase,
            workManagerProvider,
            coroutineDispatcherProvider,
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()

        stopKoin()
        unmockkAll()
    }

    @Test
    fun `enable video upload - ok`() {
        every { accountProvider.getCurrentOwnCloudAccount() } returns OC_ACCOUNT

        videosViewModel.enableVideoUploads()

        verify(exactly = 1) {
            saveVideoUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `disable video upload - ok`() {
        videosViewModel.disableVideoUploads()

        verify(exactly = 1) {
            resetVideoUploadsUseCase.execute(any())
        }
    }

    @Test
    fun `enable only wifi - ok`() {
        videosViewModel.useWifiOnly(true)

        verify(exactly = 1) {
            saveVideoUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `select account - ok`() {
        videosViewModel.handleSelectAccount(OC_ACCOUNT.name)

        verify(exactly = 1) {
            saveVideoUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `select behavior - ok`() {
        videosViewModel.handleSelectBehaviour(FolderBackUpConfiguration.Behavior.MOVE.toString())

        verify(exactly = 1) {
            saveVideoUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `handle select video uploads path - ok`() {
        val data: Intent = mockk()
        val ocFile: OCFile = mockk()

        every { ocFile.remotePath } returns exampleRemotePath
        every { data.getParcelableExtra<OCFile>(any()) } returns ocFile

        videosViewModel.handleSelectVideoUploadsPath(data)

        verify(exactly = 1) {
            data.getParcelableExtra<OCFile>(UploadPathActivity.EXTRA_FOLDER)
            ocFile.remotePath
            saveVideoUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `handle select video uploads source path - ok - source path hasn't changed`() {

    }

    @Test
    fun `handle select video uploads source path - ok - source path has changed`() {

    }

    @Test
    fun `schedule video uploads - ok`() {
        videosViewModel.scheduleVideoUploads()

        verify(exactly = 1) {
            workManagerProvider.enqueueCameraUploadsWorker()
        }
    }
}
