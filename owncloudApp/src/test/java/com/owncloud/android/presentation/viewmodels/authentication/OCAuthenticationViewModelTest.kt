/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.authentication

import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_BASE_URL
import com.owncloud.android.testutil.OC_BASIC_PASSWORD
import com.owncloud.android.testutil.OC_BASIC_USERNAME
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_SCOPE
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class OCAuthenticationViewModelTest : ViewModelTest() {
    private lateinit var ocAuthenticationViewModel: OCAuthenticationViewModel

    private lateinit var loginBasicAsyncUseCase: LoginBasicAsyncUseCase
    private lateinit var loginOAuthAsyncUseCase: LoginOAuthAsyncUseCase
    private lateinit var getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase
    private lateinit var supportsOAuth2UseCase: SupportsOAuth2UseCase
    private lateinit var getBaseUrlUseCase: GetBaseUrlUseCase
    private lateinit var contextProvider: ContextProvider

    private val commonException = ServerNotReachableException()

    @Before
    fun setUp() {
        contextProvider = mockk()

        every { contextProvider.isConnected() } returns true

        Dispatchers.setMain(testCoroutineDispatcher)
        startKoin {
            modules(
                module(override = true) {
                    factory {
                        contextProvider
                    }
                })
        }

        loginBasicAsyncUseCase = mockk()
        loginOAuthAsyncUseCase = mockk()
        getServerInfoAsyncUseCase = mockk()
        supportsOAuth2UseCase = mockk()
        getBaseUrlUseCase = mockk()

        testCoroutineDispatcher.pauseDispatcher()

        ocAuthenticationViewModel = OCAuthenticationViewModel(
            loginBasicAsyncUseCase = loginBasicAsyncUseCase,
            loginOAuthAsyncUseCase = loginOAuthAsyncUseCase,
            getServerInfoAsyncUseCase = getServerInfoAsyncUseCase,
            supportsOAuth2UseCase = supportsOAuth2UseCase,
            getBaseUrlUseCase = getBaseUrlUseCase,
            coroutinesDispatcherProvider = coroutineDispatcherProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        stopKoin()
    }

    @Test
    fun getServerInfoOk() {
        every { getServerInfoAsyncUseCase.execute(any()) } returns UseCaseResult.Success(OC_SERVER_INFO)
        ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<ServerInfo>>>(
                Event(UIResult.Loading()), Event(UIResult.Success(OC_SERVER_INFO))
            ),
            liveData = ocAuthenticationViewModel.serverInfo
        )
    }

    @Test
    fun getServerInfoException() {
        every { getServerInfoAsyncUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<ServerInfo>>>
                (Event(UIResult.Loading()), Event(UIResult.Error(commonException))),
            liveData = ocAuthenticationViewModel.serverInfo
        )
    }

    @Test
    fun loginBasicOk() {
        every { loginBasicAsyncUseCase.execute(any()) } returns UseCaseResult.Success(OC_BASIC_USERNAME)
        ocAuthenticationViewModel.loginBasic(OC_BASIC_USERNAME, OC_BASIC_PASSWORD, OC_ACCOUNT_NAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(
                Event(UIResult.Loading()), Event(UIResult.Success(OC_BASIC_USERNAME))
            ),
            liveData = ocAuthenticationViewModel.loginResult
        )
    }

    @Test
    fun loginBasicException() {
        every { loginBasicAsyncUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        ocAuthenticationViewModel.loginBasic(OC_BASIC_USERNAME, OC_BASIC_PASSWORD, null)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(
                Event(UIResult.Loading()), Event(UIResult.Error(commonException))
            ),
            liveData = ocAuthenticationViewModel.loginResult
        )
    }

    @Test
    fun loginOAuthOk() {
        every { loginOAuthAsyncUseCase.execute(any()) } returns UseCaseResult.Success(OC_BASIC_USERNAME)
        ocAuthenticationViewModel.loginOAuth(
            username = OC_BASIC_USERNAME,
            authTokenType = OC_AUTH_TOKEN_TYPE,
            accessToken = OC_ACCESS_TOKEN,
            refreshToken = OC_REFRESH_TOKEN,
            scope = OC_SCOPE,
            updateAccountWithUsername = null,
            clientRegistrationInfo = OC_CLIENT_REGISTRATION
        )

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(
                Event(UIResult.Loading()), Event(UIResult.Success(OC_BASIC_USERNAME))
            ),
            liveData = ocAuthenticationViewModel.loginResult
        )
    }

    @Test
    fun loginOAuthException() {
        every { loginOAuthAsyncUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        ocAuthenticationViewModel.loginOAuth(
            username = OC_BASIC_USERNAME,
            authTokenType = OC_AUTH_TOKEN_TYPE,
            accessToken = OC_ACCESS_TOKEN,
            refreshToken = OC_REFRESH_TOKEN,
            scope = OC_SCOPE,
            updateAccountWithUsername = null,
            clientRegistrationInfo = OC_CLIENT_REGISTRATION
        )

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(
                Event(UIResult.Loading()), Event(UIResult.Error(commonException))
            ),
            liveData = ocAuthenticationViewModel.loginResult
        )
    }

    @Test
    fun supportsOAuthOk() {
        every { supportsOAuth2UseCase.execute(any()) } returns UseCaseResult.Success(true)
        ocAuthenticationViewModel.supportsOAuth2(OC_BASIC_USERNAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<Boolean>>>(Event(UIResult.Success(true))),
            liveData = ocAuthenticationViewModel.supportsOAuth2
        )
    }

    @Test
    fun supportsOAuthException() {
        every { supportsOAuth2UseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        ocAuthenticationViewModel.supportsOAuth2(OC_BASIC_USERNAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<Boolean>>>(Event(UIResult.Error(commonException))),
            liveData = ocAuthenticationViewModel.supportsOAuth2
        )
    }

    @Test
    fun getBaseUrlOk() {
        every { getBaseUrlUseCase.execute(any()) } returns UseCaseResult.Success(OC_BASE_URL)
        ocAuthenticationViewModel.getBaseUrl(OC_BASIC_USERNAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(Event(UIResult.Success(OC_BASE_URL))),
            liveData = ocAuthenticationViewModel.baseUrl
        )
    }

    @Test
    fun getBaseUrlException() {
        every { getBaseUrlUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        ocAuthenticationViewModel.getBaseUrl(OC_BASIC_USERNAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<String>>>(Event(UIResult.Error(commonException))),
            liveData = ocAuthenticationViewModel.baseUrl
        )
    }
}
