/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

import com.owncloud.android.authentication.oauth.OAuthUtils
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.authentication.oauth.OIDCDiscoveryUseCase
import com.owncloud.android.domain.authentication.oauth.RegisterClientUseCase
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase
import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration
import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.presentation.viewmodels.oauth.OAuthViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_REFRESH
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
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
class OAuthViewModelTest : ViewModelTest() {
    private lateinit var oAuthViewModel: OAuthViewModel

    private lateinit var getOIDCDiscoveryUseCase: OIDCDiscoveryUseCase
    private lateinit var registerClientUseCase: RegisterClientUseCase
    private lateinit var requestTokenUseCase: RequestTokenUseCase
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

        mockkConstructor(OAuthUtils::class)
        every { anyConstructed<OAuthUtils>().generateRandomCodeVerifier() } returns "CODE VERIFIER"
        every { anyConstructed<OAuthUtils>().generateCodeChallenge(any()) } returns "CODE CHALLENGE"

        getOIDCDiscoveryUseCase = mockk()
        requestTokenUseCase = mockk()
        registerClientUseCase = mockk()

        testCoroutineDispatcher.pauseDispatcher()

        oAuthViewModel = OAuthViewModel(
            getOIDCDiscoveryUseCase = getOIDCDiscoveryUseCase,
            requestTokenUseCase = requestTokenUseCase,
            coroutinesDispatcherProvider = coroutineDispatcherProvider,
            registerClientUseCase = registerClientUseCase
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        stopKoin()
    }

    @Test
    fun `get oidc server configuration - ok`() {
        every { getOIDCDiscoveryUseCase.execute(any()) } returns UseCaseResult.Success(OC_OIDC_SERVER_CONFIGURATION)
        oAuthViewModel.getOIDCServerConfiguration(OC_SERVER_INFO.baseUrl)

        assertEmittedValues(
            expectedValues = listOf(
                Event<UIResult<OIDCServerConfiguration>>(
                    UIResult.Success(
                        OC_OIDC_SERVER_CONFIGURATION
                    )
                )
            ),
            liveData = oAuthViewModel.oidcDiscovery
        )
    }

    @Test
    fun `get oidc server configuration - ko - exception`() {
        every { getOIDCDiscoveryUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        oAuthViewModel.getOIDCServerConfiguration(OC_SERVER_INFO.baseUrl)

        assertEmittedValues(
            expectedValues = listOf(Event<UIResult<OIDCServerConfiguration>>(UIResult.Error(commonException))),
            liveData = oAuthViewModel.oidcDiscovery
        )
    }

    @Test
    fun `request token - ok - refresh token`() {
        every { requestTokenUseCase.execute(any()) } returns UseCaseResult.Success(OC_TOKEN_RESPONSE)
        oAuthViewModel.requestToken(OC_TOKEN_REQUEST_REFRESH)

        assertEmittedValues(
            expectedValues = listOf(Event<UIResult<TokenResponse>>(UIResult.Success(OC_TOKEN_RESPONSE))),
            liveData = oAuthViewModel.requestToken
        )
    }

    @Test
    fun `request token - ko - refresh token`() {
        every { requestTokenUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        oAuthViewModel.requestToken(OC_TOKEN_REQUEST_REFRESH)

        assertEmittedValues(
            expectedValues = listOf(Event<UIResult<TokenResponse>>(UIResult.Error(commonException))),
            liveData = oAuthViewModel.requestToken
        )
    }

    @Test
    fun `request token - ok - access token`() {
        every { requestTokenUseCase.execute(any()) } returns UseCaseResult.Success(OC_TOKEN_RESPONSE)
        oAuthViewModel.requestToken(OC_TOKEN_REQUEST_ACCESS)

        assertEmittedValues(
            expectedValues = listOf(Event<UIResult<TokenResponse>>(UIResult.Success(OC_TOKEN_RESPONSE))),
            liveData = oAuthViewModel.requestToken
        )
    }

    @Test
    fun `request token - ko - access token`() {
        every { requestTokenUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        oAuthViewModel.requestToken(OC_TOKEN_REQUEST_ACCESS)

        assertEmittedValues(
            expectedValues = listOf(Event<UIResult<TokenResponse>>(UIResult.Error(commonException))),
            liveData = oAuthViewModel.requestToken
        )
    }
}
