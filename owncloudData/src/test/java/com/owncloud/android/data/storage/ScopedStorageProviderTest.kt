package com.owncloud.android.data.storage

import android.content.Context
import android.net.Uri
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class ScopedStorageProviderTest {
    private lateinit var scopedStorageProvider: ScopedStorageProvider

    private lateinit var context: Context
    private lateinit var rootFolderName: String
    private lateinit var file: File
    private lateinit var directory: File
    private lateinit var accountDirectoryPath: String
    private lateinit var rootFolderPath: String
    private lateinit var filesDir: File

    private val absolutePath = "/storage/emulated/0/owncloud"
    private val remotePath = "/storage/emulated/0/owncloud/remotepath"
    private val spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id
    private val accountName = "owncloud"
    private val newName = "owncloudNewName.txt"
    private val uriEncoded = "/path/to/remote/?x=%D1%88%D0%B5%D0%BB%D0%BB%D1%8B"
    private val expectedValue: Long = 100
    private val separator = File.separator

    @Before
    fun setUp() {
        context = mockk()
        filesDir = mockk()
        rootFolderName = "root_folder"
        scopedStorageProvider = ScopedStorageProvider(rootFolderName, context)
        accountDirectoryPath = absolutePath + File.separator + rootFolderName + File.separator + uriEncoded

        rootFolderPath = absolutePath + File.separator + rootFolderName
        file = mockk<File>().apply {
            every { exists() } returns true
            every { isDirectory } returns false
            every { length() } returns expectedValue
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
    fun `get primary storage directory - ok - should return the filesDir`() {
        val result = scopedStorageProvider.getPrimaryStorageDirectory()
        assertEquals(filesDir, result)

        verify(exactly = 1) {
            context.filesDir
        }
    }

    @Test
    fun `get root folder path - ok - String`() {
        val actualPath = scopedStorageProvider.getRootFolderPath()
        assertEquals(rootFolderPath, actualPath)

        verify (exactly = 1)  {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }

    }

    @Test
    fun `get default save path if there are space - ok - should return the String with spaces`() {
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
    fun `get default save path if there are not space - ok - should return the String without spaces`() {
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
    fun `get expected remote path with parent variable with separator in the end - ok - should return String expected remote path`() {

        val isFolder = true
        val parent = "$separator storage$separator emulated$separator 0$separator owncloud$separator".replace(" ", "")

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `get expected remote path with parent variable without separator - ok - should return String expected remote path`() {

        val isFolder = true
        val parent = "$separator storage$separator emulated$separator 0$separator owncloud".replace(" ", "")

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `get expected remote path with parent variable without separator and it is not a folder - ok - should return String expected remote path`() {
        val isFolder = false
        val parent = "$separator storage$separator emulated$separator 0$separator owncloud".replace(" ", "")

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test get expected remote path throws illegalArgumentException - ko - IllegalArgumentException`() {
        val isFolder = false
        val remotePath = ""

        scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)
    }

    @Test
    fun `get temporal path if there are a Space - ok - String`() {
        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val temporalPathWithoutSpace = rootFolderPath + File.separator + "tmp" + File.separator + uriEncoded

        val expectedValue = temporalPathWithoutSpace + File.separator + spaceId
        val actualValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actualValue)

        verify (exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `get temporal path if there are not a Space - ok - String`() {
        val spaceId = null

        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val expectedValue = rootFolderPath + File.separator + TEMPORAL_FOLDER_NAME + File.separator + uriEncoded
        val actualValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actualValue)
    }

    @Test
    fun `get log path - ok - String`() {
        val expectedValue = rootFolderPath + File.separator + LOGS_FOLDER_NAME + File.separator
        val actualValue = scopedStorageProvider.getLogsPath()

        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `get usable space  - ok -  Long`() {
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
    fun `test sizeOfDirectory when isDirectory is true - ok - Long`() {
        every { filesDir.exists() } returns true
        every { filesDir.listFiles() } returns arrayOf(directory)

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)

        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            filesDir.exists()
            filesDir.listFiles()
        }
    }

    @Test
    fun `test sizeOfDirectory when isDirectory is false - ok - return Long`() {
        val fileSizeDirectory: File = mockk()
        every { fileSizeDirectory.exists() } returns true
        every { fileSizeDirectory.listFiles() } returns arrayOf(file)

        val actualValue = scopedStorageProvider.sizeOfDirectory(fileSizeDirectory)
        assertEquals(expectedValue, actualValue)

        verify(exactly = 1) {
            fileSizeDirectory.exists()
            fileSizeDirectory.listFiles()
        }
    }

    @Test
    fun `size of directory if dir not exists - ok - return a Long`() {
        val expectedSizeOfDirectoryValue: Long = 0

        every { filesDir.exists() } returns false

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)

        assertEquals(expectedSizeOfDirectoryValue, actualValue)

        verify(exactly = 1) {
            filesDir.exists()
        }
    }

    @Test
    fun `delete local file - ok `() {
        mockkStatic(Uri::class)
        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.deleteLocalFile(OC_FILE)

        verify(exactly = 1) {
                scopedStorageProvider.getPrimaryStorageDirectory()
        }
    }

    @Test
    fun `move local file - ok `() {
        val finalStoragePath: String = "file.txt"
        mockkStatic(Uri::class)

        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.moveLocalFile(OC_FILE, finalStoragePath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()

        }
    }

    companion object {
        private const val LOGS_FOLDER_NAME = "logs"
        private const val TEMPORAL_FOLDER_NAME = "tmp"
    }

}
