package com.owncloud.android.data.server.datasources

import com.owncloud.android.data.server.datasources.implementation.OCRemoteAnonymousDataSource
import com.owncloud.android.data.server.network.OCAnonymousServerService
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class OCRemoteAnonymousDatasourceTest {
    private lateinit var ocRemoteAnonymousDatasource: OCRemoteAnonymousDataSource

    private val ocAnonymousService: OCAnonymousServerService = mockk()

    @Before
    fun init() {
        ocRemoteAnonymousDatasource = OCRemoteAnonymousDataSource(ocAnonymousService)
    }

    //TODO: Test getAuthenticationMethod and getRemoteStatus
}
