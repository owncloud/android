package com.owncloud.android.data.providers

import android.content.Context
import android.net.Uri
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import java.io.File
import org.junit.Before
import org.junit.Test

class ScopedStorageProviderTest {
    private lateinit var scopedStorageProvider: ScopedStorageProvider

    private lateinit var context: Context
    private lateinit var file: File
    private lateinit var directory: File
    private lateinit var filesDir: File

    private val absolutePath = "/storage/emulated/0/owncloud"
    private val remotePath = "/storage/emulated/0/owncloud/remotepath"
    private val spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id
    private val accountName = "owncloud"
    private val newName = "owncloudNewName.txt"
    private val uriEncoded = "/path/to/remote/?x=%D1%88%D0%B5%D0%BB%D0%BB%D1%8B"
    private val rootFolderName = "root_folder"
    private val rootFolderPath = absolutePath + File.separator + rootFolderName
    private val expectedSizeOfDirectoryValue: Long = 100
    private val separator = File.separator
    private val accountDirectoryPath = absolutePath + File.separator + rootFolderName + File.separator + uriEncoded

    @Before
    fun setUp() {
        context = mockk()
        filesDir = mockk()
        scopedStorageProvider = ScopedStorageProvider(rootFolderName, context)

        file = mockk<File>().apply {
            every { exists() } returns true
            every { isDirectory } returns false
            every { length() } returns expectedSizeOfDirectoryValue
        }

        directory = mockk<File>().apply {
            every { exists() } returns true
            every { isDirectory } returns true
            every { listFiles() } returns arrayOf(file)
        }

        every { context.filesDir } returns filesDir
        every { filesDir.absolutePath } returns absolutePath
    }

    @Test
    fun `getPrimaryStorageDirectory returns filesDir`() {
        val result = scopedStorageProvider.getPrimaryStorageDirectory()
        assertEquals(filesDir, result)

        verify(exactly = 1) {
            context.filesDir
        }
    }

    @Test
    fun `getRootFolderPath returns the root folder path String`() {
        val actualPath = scopedStorageProvider.getRootFolderPath()
        assertEquals(rootFolderPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }

    }

    @Test
    fun `getDefaultSavePathFor returns the path with spaces when there is a space`() {
        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val expectedPath = accountDirectoryPath + File.separator + spaceId + File.separator + remotePath
        val actualPath = scopedStorageProvider.getDefaultSavePathFor(accountName, remotePath, spaceId)

        assertEquals(expectedPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `getDefaultSavePathFor returns the path without spaces when there is not space`() {
        val spaceId = null

        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val expectedPath = accountDirectoryPath + remotePath
        val actualPath = scopedStorageProvider.getDefaultSavePathFor(accountName, remotePath, spaceId)

        assertEquals(expectedPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `getExpectedRemotePath returns expected remote path with separator in the end when there is separator and is folder true`() {

        val isFolder = true
        val parent = separator + "storage" + separator + "emulated" + separator + "0" + separator + "owncloud" + separator
        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `getExpectedRemotePath returns expected remote path with separator in the end when is separator and is folder false`() {

        val isFolder = false
        val parent = separator + "storage" + separator + "emulated" + separator + "0" + separator + "owncloud" + separator

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `getExpectedRemotePath returns expected remote path with separator in the end when is not separator and is folder true`() {

        val isFolder = true
        val parent = separator + "storage" + separator + "emulated" + separator + "0" + separator + "owncloud"

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `getExpectedRemotePath returns expected remote path with separator in the end when is not separator and is folder false`() {
        val isFolder = false
        val parent = separator + "storage" + separator + "emulated" + separator + "0" + separator + "owncloud"

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getExpectedRemotePath returns a IllegalArgumentException when there is not file`() {
        val isFolder = false
        val remotePath = ""

        scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)
    }

    @Test
    fun `getTemporalPath returns expected temporal path with separator and space when there is a space`() {
        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val temporalPathWithoutSpace = rootFolderPath + File.separator + "tmp" + File.separator + uriEncoded

        val expectedValue = temporalPathWithoutSpace + File.separator + spaceId
        val actualValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `getTemporalPath returns expected temporal path neither with separator not space when there is not a space`() {
        val spaceId = null

        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val expectedValue = rootFolderPath + File.separator + TEMPORAL_FOLDER_NAME + File.separator + uriEncoded
        val actualValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `getLogsPath returns logs path`() {
        val expectedValue = rootFolderPath + File.separator + LOGS_FOLDER_NAME + File.separator
        val actualValue = scopedStorageProvider.getLogsPath()

        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `getUsableSpace returns usable space from the storage directory`() {
        val expectedUsableSpace: Long = 1000000

        every { filesDir.usableSpace } returns expectedUsableSpace

        val actualUsableSpace = scopedStorageProvider.getUsableSpace()

        assertEquals(expectedUsableSpace, actualUsableSpace)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.usableSpace
        }
    }

    @Test
    fun `sizeOfDirectory returns the sum the file size in bytes (Long) when isDirectory is true doing a recursive call if it's a directory`() {
        every { filesDir.exists() } returns true
        every { filesDir.listFiles() } returns arrayOf(directory)

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)

        assertEquals(expectedSizeOfDirectoryValue, actualValue)

        verify(exactly = 1) {
            filesDir.exists()
            filesDir.listFiles()
        }
    }

    @Test
    fun `sizeOfDirectory returns the sum the file size in bytes (Long) when isDirectory is false without doing a recursive call`() {
        val fileSizeDirectory: File = mockk()
        every { fileSizeDirectory.exists() } returns true
        every { fileSizeDirectory.listFiles() } returns arrayOf(file)

        val actualValue = scopedStorageProvider.sizeOfDirectory(fileSizeDirectory)
        assertEquals(expectedSizeOfDirectoryValue, actualValue)

        verify(exactly = 1) {
            fileSizeDirectory.exists()
            fileSizeDirectory.listFiles()
        }
    }

    @Test
    fun `sizeOfDirectory returns zero value when directory not exists`() {
        val expectedSizeOfDirectoryValue: Long = 0

        every { filesDir.exists() } returns false

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)

        assertEquals(expectedSizeOfDirectoryValue, actualValue)

        verify(exactly = 1) {
            filesDir.exists()
        }
    }

    @Test
    fun `deleteLocalFile calls getPrimaryStorageDirectory()`() {
        mockkStatic(Uri::class)
        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.deleteLocalFile(OC_FILE)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `moveLocalFile calls getPrimaryStorageDirectory()`() {
        val finalStoragePath = "file.txt"
        mockkStatic(Uri::class)

        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.moveLocalFile(OC_FILE, finalStoragePath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `deleteCacheIfNeeded delete cache file when  transfer local path start with cacheDir`() {
        val transfer: OCTransfer = mockk()
        val accountName = "testAccount"
        val localPath = "/file.txt"

        mockkStatic(Uri::class)
        every { Uri.encode(any(), any()) } returns uriEncoded
        every { transfer.accountName } returns accountName
        every { transfer.localPath } returns localPath

        scopedStorageProvider.deleteCacheIfNeeded(transfer)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    private fun conditionsExpectedRemotePath(parent: String, newName: String, isFolder: Boolean): String {
        every { filesDir.parent } returns parent
        val parentDir = if (parent.endsWith(File.separator)) parent else parent + File.separator
        var newRemotePath = parentDir + newName
        if (isFolder) {
            newRemotePath += File.separator
        }
        return newRemotePath
    }

    companion object {
        private const val LOGS_FOLDER_NAME = "logs"
        private const val TEMPORAL_FOLDER_NAME = "tmp"
    }

}
