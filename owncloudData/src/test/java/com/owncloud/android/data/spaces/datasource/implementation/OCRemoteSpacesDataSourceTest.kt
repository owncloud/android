package com.owncloud.android.data.spaces.datasource.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.spaces.datasources.implementation.OCRemoteSpacesDataSource
import com.owncloud.android.data.spaces.datasources.implementation.OCRemoteSpacesDataSource.Companion.toModel
import com.owncloud.android.lib.resources.spaces.responses.QuotaResponse
import com.owncloud.android.lib.resources.spaces.responses.RootResponse
import com.owncloud.android.lib.resources.spaces.responses.SpaceResponse
import com.owncloud.android.lib.resources.spaces.services.OCSpacesService
import com.owncloud.android.testutil.OC_ACCOUNT_ID
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteSpacesDataSourceTest {

    private lateinit var ocRemoteSpacesDataSource: OCRemoteSpacesDataSource

    private val ocSpaceService: OCSpacesService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    private val spaceResponse =
        SpaceResponse(
            driveAlias = "driveAlias",
            driveType = "driveType",
            id = OC_ACCOUNT_ID,
            lastModifiedDateTime = "lastModifiedDateTime",
            name = "name",
            webUrl = "webUrl",
            description = "description",
            owner = null,
            root = RootResponse(
                eTag = "eTag",
                id = OC_ACCOUNT_ID,
                webDavUrl = "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f3805bca744-d89f-4e9c-a990-25a0d7f03fe9",
                deleted = null
            ),
            quota = QuotaResponse(
                remaining = 1,
                state = "state",
                total = 10,
                used = 1
            ),
            special = null,

            )

    @Before
    fun init() {
        ocRemoteSpacesDataSource = OCRemoteSpacesDataSource(clientManager)
        every { clientManager.getSpacesService(any()) } returns ocSpaceService
    }

    @Test
    fun `refreshSpacesForAccount returns a list of OCSpace`() {
        val removeRemoteSpaceOperationResult = createRemoteOperationResultMock(
            listOf(spaceResponse), isSuccess = true
        )

        every { ocSpaceService.getSpaces() } returns removeRemoteSpaceOperationResult

        val resultActual = ocRemoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)

         assertEquals(
            listOf(spaceResponse.toModel(OC_ACCOUNT_NAME)), resultActual
        )

        verify(exactly = 1) {
            ocSpaceService.getSpaces()
        }
    }

    @Test(expected = Exception::class)
    fun `refreshSpacesForAccount returns an exception when service receive an exception`() {

        every { ocSpaceService.getSpaces() } throws Exception()

        ocRemoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)
    }

}