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
import com.owncloud.android.domain.camerauploads.usecases.GetPictureUploadsConfigurationStreamUseCase
import com.owncloud.android.domain.camerauploads.usecases.ResetPictureUploadsUseCase
import com.owncloud.android.domain.camerauploads.usecases.SavePictureUploadsConfigurationUseCase
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
class SettingsPictureUploadsViewModelTest : ViewModelTest() {
    private lateinit var picturesViewModel: SettingsPictureUploadsViewModel
    private lateinit var accountProvider: AccountProvider
    private lateinit var workManagerProvider: WorkManagerProvider

    private lateinit var savePictureUploadsConfigurationUseCase: SavePictureUploadsConfigurationUseCase
    private lateinit var getPictureUploadsConfigurationStreamUseCase: GetPictureUploadsConfigurationStreamUseCase
    private lateinit var resetPictureUploadsUseCase: ResetPictureUploadsUseCase

    private val examplePath = "/Example/Path"
    private val exampleSourcePath = "/Example/Source/Path"
    private val exampleRemotePath = "/Example/Remote/Path"

    @Before
    fun setUp() {
        accountProvider = mockk()
        workManagerProvider = mockk()
        savePictureUploadsConfigurationUseCase = mockk()
        getPictureUploadsConfigurationStreamUseCase = mockk()
        resetPictureUploadsUseCase = mockk()

        picturesViewModel = SettingsPictureUploadsViewModel(
            accountProvider,
            savePictureUploadsConfigurationUseCase,
            getPictureUploadsConfigurationStreamUseCase,
            resetPictureUploadsUseCase,
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
    fun `enable picture upload - ok`() {
        every { accountProvider.getCurrentOwnCloudAccount() } returns OC_ACCOUNT

        picturesViewModel.enablePictureUploads()

        verify(exactly = 1) {
            savePictureUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `disable picture upload - ok`() {
        picturesViewModel.disablePictureUploads()

        verify(exactly = 1) {
            resetPictureUploadsUseCase.execute(any())
        }
    }

    @Test
    fun `enable only wifi - ok`() {
        picturesViewModel.useWifiOnly(true)

        verify(exactly = 1) {
            savePictureUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `select account - ok`() {
        picturesViewModel.handleSelectAccount(OC_ACCOUNT.name)

        verify(exactly = 1) {
            savePictureUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `select behavior - ok`() {
        picturesViewModel.handleSelectBehaviour(FolderBackUpConfiguration.Behavior.MOVE.toString())

        verify(exactly = 1) {
            savePictureUploadsConfigurationUseCase.execute(any())
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
            savePictureUploadsConfigurationUseCase.execute(any())
        }
    }

    @Test
    fun `handle select picture uploads source path - ok - source path hasn't changed`() {
    }

    @Test
    fun `handle select picture uploads source path - ok - source path has changed`() {
    }

    @Test
    fun `schedule picture uploads sync job - ok`() {
        picturesViewModel.schedulePictureUploads()

        verify(exactly = 1) {
            workManagerProvider.enqueueCameraUploadsWorker()
        }
    }
}
