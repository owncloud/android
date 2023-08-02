package com.owncloud.android.data.appRegistry.datasources.implementation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.appregistry.datasources.implementation.OCLocalAppRegistryDataSource
import com.owncloud.android.data.appregistry.db.AppRegistryDao
import com.owncloud.android.data.appregistry.db.AppRegistryEntity
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_APP_REGISTRY_MIMETYPE
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCLocalAppRegistryDataSourceTest {
    private lateinit var ocLocalAppRegistryDataSource: OCLocalAppRegistryDataSource
    private val appRegistryDao = mockkClass(AppRegistryDao::class)
    private val mimetype = "DIR"
    private val ocAppRegistryEntity = AppRegistryEntity(
        accountName = OC_ACCOUNT_NAME,
        mimeType = mimetype,
        ext = "appRegistryMimeTypes.ext",
        appProviders = "null",
        name = "appRegistryMimeTypes.name",
        icon = "appRegistryMimeTypes.icon",
        description = "appRegistryMimeTypes.description",
        allowCreation = true,
        defaultApplication = "appRegistryMimeTypes.defaultApplication",
    )

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {

        val db = mockkClass(OwncloudDatabase::class)

        every {
            db.appRegistryDao()
        } returns appRegistryDao

        ocLocalAppRegistryDataSource =
            OCLocalAppRegistryDataSource(
                appRegistryDao,
            )
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns a flow with AppRegistryMimeType object`() = runBlocking {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } returns flowOf(ocAppRegistryEntity)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        appRegistry.collect { appRegistryEmitted ->
            Assert.assertEquals(OC_APP_REGISTRY_MIMETYPE, appRegistryEmitted)
        }

        verify (exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test
    fun `getAppRegistryForMimeTypeAsStream returns null when DAO no receive values from db`() = runBlocking {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } returns flowOf(null)

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        appRegistry.collect { appRegistryEmitted ->
            Assert.assertNull(appRegistryEmitted)
        }
        verify (exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test(expected = Exception::class)
    fun `getAppRegistryForMimeTypeAsStream returns an Exception when DAO return an Exception`() = runBlocking {

        every { appRegistryDao.getAppRegistryForMimeType(any(), any()) } throws Exception()

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryForMimeTypeAsStream(OC_ACCOUNT_NAME, mimetype)

        appRegistry.collect { appRegistryEmitted ->
            Assert.assertNull(appRegistryEmitted)
        }
        verify (exactly = 1) { appRegistryDao.getAppRegistryForMimeType(OC_ACCOUNT_NAME, mimetype) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns a flow with a list of AppRegistryMimeType object`() = runBlocking {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(any()) } returns flowOf(listOf(ocAppRegistryEntity))

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        appRegistry.collect { appRegistryEmitted ->
            Assert.assertEquals(listOf(OC_APP_REGISTRY_MIMETYPE), appRegistryEmitted)
        }

        verify (exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getAppRegistryWhichAllowCreation returns empty list when DAO return empty list`() = runBlocking {

        every { appRegistryDao.getAppRegistryWhichAllowCreation(any()) } returns flowOf(emptyList<AppRegistryEntity>())

        val appRegistry = ocLocalAppRegistryDataSource.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME)

        appRegistry.collect {listEmitted ->
            Assert.assertEquals(emptyList<AppRegistryEntity>(), listEmitted)
        }

        verify (exactly = 1) { appRegistryDao.getAppRegistryWhichAllowCreation(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `saveAppRegistryForAccount should save the AppRegistry entities`() = runBlocking {
        val appRegistry = AppRegistry(
            OC_ACCOUNT_NAME, mutableListOf(
                AppRegistryMimeType("mime_type_1", "ext_1", emptyList(), "name_1", "icon_1", "description_1", true, "default_app_1"),
                AppRegistryMimeType("mime_type_2", "ext_2", emptyList(), "name_2", "icon_2", "description_2", true, "default_app_2")
            )
        )

        every { appRegistryDao.deleteAppRegistryForAccount(OC_ACCOUNT_NAME) } returns Unit
        every { appRegistryDao.upsertAppRegistries(any()) } returns Unit

        ocLocalAppRegistryDataSource.saveAppRegistryForAccount(appRegistry)

        verify (exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName) }
        verify (exactly = 1) { appRegistryDao.upsertAppRegistries(any()) }
    }

    @Test(expected = Exception::class)
    fun `saveAppRegistryForAccount should returns an Exception`() = runBlocking {
        val appRegistry = AppRegistry(
            OC_ACCOUNT_NAME, mutableListOf(
                AppRegistryMimeType("mime_type_1", "ext_1", emptyList(), "name_1", "icon_1", "description_1", true, "default_app_1"),
                AppRegistryMimeType("mime_type_2", "ext_2", emptyList(), "name_2", "icon_2", "description_2", true, "default_app_2")
            )
        )

        every { appRegistryDao.deleteAppRegistryForAccount(OC_ACCOUNT_NAME) } throws Exception()
        every { appRegistryDao.upsertAppRegistries(any()) } throws Exception()

        ocLocalAppRegistryDataSource.saveAppRegistryForAccount(appRegistry)

        verify (exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(appRegistry.accountName) }
        verify (exactly = 1) { appRegistryDao.upsertAppRegistries(any()) }
    }

    @Test
    fun `deleteAppRegistryForAccount should delete appRegistry`() = runBlocking {

        every { appRegistryDao.deleteAppRegistryForAccount(OC_ACCOUNT_NAME) } returns Unit

        ocLocalAppRegistryDataSource.deleteAppRegistryForAccount(OC_ACCOUNT_NAME)

        verify (exactly = 1) { appRegistryDao.deleteAppRegistryForAccount(any()) }
    }

}