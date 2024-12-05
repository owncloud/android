/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.folderbackup.repository

import com.owncloud.android.data.folderbackup.OCFolderBackupRepository
import com.owncloud.android.data.folderbackup.datasources.LocalFolderBackupDataSource
import com.owncloud.android.testutil.OC_AUTOMATIC_UPLOADS_CONFIGURATION
import com.owncloud.android.testutil.OC_BACKUP
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OCFolderBackupRepositoryTest {

    private val localFolderBackupDataSource = mockk<LocalFolderBackupDataSource>(relaxUnitFun = true)
    private val ocFolderBackupRepository = OCFolderBackupRepository(localFolderBackupDataSource)

    private val pictureUploadsName = "Pictures uploads"

    @Test
    fun `getAutomaticUploadsConfiguration returns AutomaticUploadsConfiguration`() {
        every {
            localFolderBackupDataSource.getAutomaticUploadsConfiguration()
        } returns OC_AUTOMATIC_UPLOADS_CONFIGURATION

        val automaticUploadsConfiguration = ocFolderBackupRepository.getAutomaticUploadsConfiguration()
        assertEquals(OC_AUTOMATIC_UPLOADS_CONFIGURATION, automaticUploadsConfiguration)

        verify(exactly = 1) {
            localFolderBackupDataSource.getAutomaticUploadsConfiguration()
        }
    }

    @Test
    fun `getAutomaticUploadsConfiguration returns null when local data source returns a null configuration`() {
        every {
            localFolderBackupDataSource.getAutomaticUploadsConfiguration()
        } returns null

        val automaticUploadsConfiguration = ocFolderBackupRepository.getAutomaticUploadsConfiguration()
        assertNull(automaticUploadsConfiguration)

        verify(exactly = 1) {
            localFolderBackupDataSource.getAutomaticUploadsConfiguration()
        }
    }

    @Test
    fun `getFolderBackupConfigurationByNameAsFlow returns a Flow with a FolderBackUpConfiguration`() = runTest {
        every {
            localFolderBackupDataSource.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName)
        } returns flowOf(OC_BACKUP)

        val folderBackUpConfiguration = ocFolderBackupRepository.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName).first()
        assertEquals(OC_BACKUP, folderBackUpConfiguration)

        verify(exactly = 1) {
            localFolderBackupDataSource.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName)
        }
    }

    @Test
    fun `getFolderBackupConfigurationByNameAsFlow returns a Flow with null when local data source returns a Flow with null `() = runTest {
        every {
            localFolderBackupDataSource.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName)
        } returns flowOf(null)

        val folderBackUpConfiguration = ocFolderBackupRepository.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName).first()
        assertNull(folderBackUpConfiguration)

        verify(exactly = 1) {
            localFolderBackupDataSource.getFolderBackupConfigurationByNameAsFlow(pictureUploadsName)
        }
    }

    @Test
    fun `saveFolderBackupConfiguration saves a folder backup configuration correctly`() {
        ocFolderBackupRepository.saveFolderBackupConfiguration(OC_BACKUP)

        verify(exactly = 1) {
            localFolderBackupDataSource.saveFolderBackupConfiguration(OC_BACKUP)
        }
    }

    @Test
    fun `resetFolderBackupConfigurationByName resets a folder backup configuration correctly by the name`() {
        ocFolderBackupRepository.resetFolderBackupConfigurationByName(pictureUploadsName)

        verify(exactly = 1) {
            localFolderBackupDataSource.resetFolderBackupConfigurationByName(pictureUploadsName)
        }
    }

}
