package com.owncloud.android.data.storage

import android.content.Context
import android.net.Uri
import com.owncloud.android.domain.files.model.OCFile
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

    private val filesDir: File = mockk()
    private val absolutePath = "/storage/emulated/0/owncloud"
    private val remotePath = "/storage/emulated/0/owncloud/remotepath"
    private val spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id
    private val accountName = "onwcloud"
    private val newName = "onwcloudNewName.txt"
    private val uriEncoded = "/path/to/remote/?x=%D1%88%D0%B5%D0%BB%D0%BB%D1%8B"
    private val expectedValue: Long = 100
    private val file = mockk<File>().apply {
        every { exists() } returns true
        every { isDirectory } returns false
        every { length() } returns expectedValue
    }
    private val directory = mockk<File>().apply {
        every { exists() } returns true
        every { isDirectory } returns true
        every { listFiles() } returns arrayOf(file)
    }

    private val ocFile = OCFile(
        OC_FILE.id,
        OC_FILE.parentId,
        OC_FILE.owner,
        OC_FILE.length,
        OC_FILE.creationTimestamp,
        OC_FILE.modificationTimestamp,
        OC_FILE.remotePath,
        OC_FILE.mimeType,
        OC_FILE.etag,
        OC_FILE.permissions,
        OC_FILE.remoteId,
        OC_FILE.privateLink,
        OC_FILE.storagePath,
        OC_FILE.treeEtag,
        OC_FILE.availableOfflineStatus,
        OC_FILE.lastSyncDateForData,
        OC_FILE.needsToUpdateThumbnail,
        OC_FILE.modifiedAtLastSyncForData,
        OC_FILE.etagInConflict,
        OC_FILE.fileIsDownloading,
        OC_FILE.sharedWithSharee,
        OC_FILE.sharedByLink
    )

    @Before
    fun setUp() {
        context = mockk()
        rootFolderName = "root_folder"
        scopedStorageProvider = ScopedStorageProvider(rootFolderName, context)

        every { context.filesDir } returns filesDir
        every { filesDir.absolutePath } returns absolutePath
    }

    @Test
    fun `get primary storage directory - ok - should return the filesDir`() {
        val result = scopedStorageProvider.getPrimaryStorageDirectory()
        assertEquals(filesDir, result)
        verify(exactly = 1) { context.filesDir }
    }

    @Test
    fun `get root folder path - ok - String`() {
        val expectedPath = absolutePath + File.separator + rootFolderName
        val actualPath = scopedStorageProvider.getRootFolderPath()
        assertEquals(expectedPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.absolutePath
        }

    }

    @Test
    fun `get default save path if there are space - ok - should return the String with spaces`() {
        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val accountDirectoryPath = absolutePath + File.separator + rootFolderName + File.separator + uriEncoded

        val expectedPath = accountDirectoryPath + File.separator + spaceId + File.separator + remotePath
        val actualPath = scopedStorageProvider.getDefaultSavePathFor(accountName, remotePath, spaceId)

        assertEquals(expectedPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.absolutePath
        }
    }

    @Test
    fun `get default save path if there are not space - ok - should return the String without spaces`() {
        val spaceId = null

        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val accountDirectoryPath = absolutePath + File.separator + rootFolderName + File.separator + uriEncoded

        val expectedPath = accountDirectoryPath + remotePath
        val actualPath = scopedStorageProvider.getDefaultSavePathFor(accountName, remotePath, spaceId)

        assertEquals(expectedPath, actualPath)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.absolutePath
        }
    }

    @Test
    fun `get expected remote path with parent variable with separator - ok - should return String expected remote path`() {

        val isFolder = true
        val parent = "\\storage\\emulated\\0\\owncloud\\"

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `get expected remote path with parent variable without separator - ok - should return String expected remote path`() {

        val isFolder = true
        val parent = "\\storage\\emulated\\0\\owncloud"

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    @Test
    fun `get expected remote path with parent variable without separator and it is not a folder - ok - should return String expected remote path`() {
        val newName = "onwcloudNewName.txt"
        val isFolder = false
        val parent = "\\storage\\emulated\\0\\owncloud"

        val expectedPath = conditionsExpectedRemotePath(parent, newName, isFolder)
        val actualPath = scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)

        assertEquals(expectedPath, actualPath)
    }

    private fun conditionsExpectedRemotePath(parent: String, newName: String, isFolder: Boolean): String {
        every { filesDir.parent } returns parent
        val parent = if (parent.endsWith(File.separator)) parent else parent + File.separator
        var newRemotePath = parent + newName
        if (isFolder) {
            newRemotePath += File.separator
        }
        return newRemotePath
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test get expected remote path throws illegalArgumentException - ko - IllegalArgumentException`() {
        val newName = "onwcloudNewName.txt"
        val isFolder = false
        val remotePath = ""

        scopedStorageProvider.getExpectedRemotePath(remotePath, newName, isFolder)
    }

    @Test
    fun `get temporal path if there are a Space - ok - String`() {
        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val temporalPathWithoutSpace =
            scopedStorageProvider.getRootFolderPath() + File.separator + "tmp" + File.separator + uriEncoded

        val expectedValue = temporalPathWithoutSpace + File.separator + spaceId
        val actuaValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actuaValue)
    }

    @Test
    fun `get temporal path if there are not a Space - ok - String`() {
        val spaceId = null

        mockkStatic(Uri::class)
        every { Uri.encode(accountName, "@") } returns uriEncoded

        val expectedValue =
            scopedStorageProvider.getRootFolderPath() + File.separator + TEMPORAL_FOLDER_NAME + File.separator + uriEncoded
        val actuaValue = scopedStorageProvider.getTemporalPath(accountName, spaceId)
        assertEquals(expectedValue, actuaValue)
    }

    @Test
    fun `get log path - ok - String`() {
        val expectedValue =
            scopedStorageProvider.getRootFolderPath() + File.separator + LOGS_FOLDER_NAME + File.separator
        val actuaValue = scopedStorageProvider.getLogsPath()

        assertEquals(expectedValue, actuaValue)

        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.absolutePath
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
    }

    @Test
    fun `test sizeOfDirectory when isDirectory is false - ok - return Long`() {
        every { filesDir.exists() } returns true
        every { filesDir.listFiles() } returns arrayOf(file)

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)
        assertEquals(expectedValue, actualValue)
    }

    @Test
    fun `size of directory if dir not exists - ok - return a Long`() {
        val expectedValue: Long = 0

        every { filesDir.exists() } returns false

        val actualValue = scopedStorageProvider.sizeOfDirectory(filesDir)

        assertEquals(expectedValue, actualValue)
    }

    @Test
    fun `delete local file - ok `() {
        mockkStatic(Uri::class)
        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.deleteLocalFile(ocFile)
        verify(exactly = 1) {
                scopedStorageProvider.getPrimaryStorageDirectory()
                filesDir.absolutePath
        }
    }

    @Test
    fun `move local file - ok `() {
        val finalStoragePath: String = "file.txt"
        mockkStatic(Uri::class)
        every { Uri.encode(any(), any()) } returns uriEncoded
        scopedStorageProvider.moveLocalFile(ocFile, finalStoragePath)
        verify(exactly = 1) {
            scopedStorageProvider.getPrimaryStorageDirectory()
            filesDir.absolutePath
        }
    }

    companion object {
        private const val LOGS_FOLDER_NAME = "logs"
        private const val TEMPORAL_FOLDER_NAME = "tmp"
    }

}
