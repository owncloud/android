package com.owncloud.android.data.folderbackup.datasources.implementation

import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.folderbackup.db.FolderBackUpEntity
import com.owncloud.android.data.folderbackup.db.FolderBackupDao
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class OCFolderBackupLocalDataSourceTest {

    private lateinit var ocFolderBackupLocalDataSource: OCFolderBackupLocalDataSource
    private val folderBackupDao = mockkClass(FolderBackupDao::class)
    private val db = mockkClass(OwncloudDatabase::class)

    private val ocFolderBackUpEntity = FolderBackUpEntity(
        accountName = OC_ACCOUNT_NAME,
        behavior = UploadBehavior.COPY.name,
        sourcePath = "/Photos",
        uploadPath = "/Photos",
        wifiOnly = true,
        chargingOnly = true,
        lastSyncTimestamp = 1542628397,
        name = ""
    )
    private val ocFolderBackUpConfiguration = FolderBackUpConfiguration(
        accountName = OC_ACCOUNT_NAME,
        behavior = UploadBehavior.COPY,
        sourcePath = "/Photos",
        uploadPath = "/Photos",
        wifiOnly = true,
        chargingOnly = true,
        lastSyncTimestamp = 1542628397,
        name = ""
    )

    @Before
    fun init() {

        every { db.folderBackUpDao() } returns folderBackupDao

        ocFolderBackupLocalDataSource = OCFolderBackupLocalDataSource(folderBackupDao)
    }

    @Test
    fun `OCFolderBackupLocalDataSource with valid configurations returns a CameraUploadsConfiguration object`() {
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName) } returns ocFolderBackUpEntity
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName) } returns ocFolderBackUpEntity

        val resultCurrent = ocFolderBackupLocalDataSource.getCameraUploadsConfiguration()

        assertEquals(ocFolderBackUpEntity.toModel(), resultCurrent?.pictureUploadsConfiguration)
        assertEquals(ocFolderBackUpEntity.toModel(), resultCurrent?.videoUploadsConfiguration)

        verify(exactly = 1) {
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName)
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName)
        }
    }

    @Test
    fun `OCFolderBackupLocalDataSource with no configurations returns null`() {
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName) } returns null
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName) } returns null

        val resultCurrent = ocFolderBackupLocalDataSource.getCameraUploadsConfiguration()

        assertNull(resultCurrent)

        verify(exactly = 1) {
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName)
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName)
        }
    }

    @Test(expected = Exception::class)
    fun `OCFolderBackupLocalDataSource when dao receive an exception returns an exception `() {
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName) } throws Exception()
        every { folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName) } throws Exception()

        ocFolderBackupLocalDataSource.getCameraUploadsConfiguration()

        verify(exactly = 1) {
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.pictureUploadsName)
            folderBackupDao.getFolderBackUpConfigurationByName(FolderBackUpConfiguration.videoUploadsName)
        }
    }

    @Test
    fun `getFolderBackupConfigurationByNameAsFlow with valid configurations returns a flow of CameraUploadsConfiguration`() = runBlocking {
        every { folderBackupDao.getFolderBackUpConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName) } returns flowOf(
            ocFolderBackUpEntity
        )

        val resultCurrent = ocFolderBackupLocalDataSource.getFolderBackupConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName)

        resultCurrent.collect { result ->
            assertEquals(ocFolderBackUpEntity.toModel(), result)
        }
        verify(exactly = 1) {
            folderBackupDao.getFolderBackUpConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName)
        }
    }

    @Test(expected = Exception::class)
    fun `getFolderBackupConfigurationByNameAsFlow when dao receive an exception returns an exception`() = runBlocking {
        every { folderBackupDao.getFolderBackUpConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName) } throws Exception()

        val resultCurrent = ocFolderBackupLocalDataSource.getFolderBackupConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName)

        resultCurrent.collect { result ->
            assertEquals(ocFolderBackUpEntity.toModel(), result)
        }
        verify(exactly = 1) {
            folderBackupDao.getFolderBackUpConfigurationByNameAsFlow(FolderBackUpConfiguration.pictureUploadsName)
        }
    }

    @Test
    fun `saveFolderBackupConfiguration with valid configurations returns unit and save the information`() {
        every { folderBackupDao.update(ocFolderBackUpEntity) } returns 1

        ocFolderBackupLocalDataSource.saveFolderBackupConfiguration(ocFolderBackUpConfiguration)

        verify(exactly = 1) {
            folderBackupDao.update(ocFolderBackUpEntity)
        }
    }

    @Test(expected = Exception::class)
    fun `saveFolderBackupConfiguration when dao receive an exception returns an exception`() {
        every { folderBackupDao.update(ocFolderBackUpEntity) } throws Exception()

        ocFolderBackupLocalDataSource.saveFolderBackupConfiguration(ocFolderBackUpConfiguration)

        verify(exactly = 1) {
            folderBackupDao.update(ocFolderBackUpEntity)
        }
    }

    @Test
    fun `resetFolderBackupConfigurationByName when folder backup configuration is reset by name returns unit `() {
        every { folderBackupDao.delete(FolderBackUpConfiguration.pictureUploadsName) } returns 1

        ocFolderBackupLocalDataSource.resetFolderBackupConfigurationByName(FolderBackUpConfiguration.pictureUploadsName)

        verify(exactly = 1) {
            folderBackupDao.delete(FolderBackUpConfiguration.pictureUploadsName)
        }
    }

    @Test(expected = Exception::class)
    fun `resetFolderBackupConfigurationByName when dao receive an exception returns an exception `() {
        every { folderBackupDao.delete(any()) } throws Exception()

        ocFolderBackupLocalDataSource.resetFolderBackupConfigurationByName(FolderBackUpConfiguration.pictureUploadsName)

        verify(exactly = 1) {
            folderBackupDao.delete(FolderBackUpConfiguration.pictureUploadsName)
        }
    }

    private fun FolderBackUpEntity.toModel() =
        FolderBackUpConfiguration(
            accountName = accountName,
            behavior = UploadBehavior.fromString(behavior),
            sourcePath = sourcePath,
            uploadPath = uploadPath,
            wifiOnly = wifiOnly,
            chargingOnly = chargingOnly,
            lastSyncTimestamp = lastSyncTimestamp,
            name = name
        )

}