/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.authentication

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.authentication.LoginActivity
import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.testutil.OC_SERVER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class LoginActivityTest {

    private lateinit var activityScenario: ActivityScenario<LoginActivity>

    private lateinit var ocAuthenticationViewModel: OCAuthenticationViewModel
    private lateinit var ocContextProvider: ContextProvider

    private lateinit var loginResultLiveData: MutableLiveData<Event<UIResult<String>>>
    private lateinit var serverInfoLiveData: MutableLiveData<Event<UIResult<ServerInfo>>>

    @Before
    fun setUp() {
        ocAuthenticationViewModel = mockk(relaxed = true)
        ocContextProvider = mockk(relaxed = true)

        loginResultLiveData = MutableLiveData()
        serverInfoLiveData = MutableLiveData()

        every { ocAuthenticationViewModel.loginResult } returns loginResultLiveData
        every { ocAuthenticationViewModel.serverInfo } returns serverInfoLiveData

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            modules(
                module(override = true) {
                    viewModel {
                        ocAuthenticationViewModel
                    }
                    factory {
                        ocContextProvider
                    }

                }
            )
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Launch the test with default configuration
     * At this moment:
     * R.bool.show_server_url_input = true
     * R.string.server_url = ""
     * R.bool.use_login_background_image = true
     * R.bool.show_welcome_link = true
     */
    private fun launchTest(
        showServerUrlInput: Boolean = true,
        serverUrl: String = "",
        showLoginBackGroundImage: Boolean = true,
        showWelcomeLink: Boolean = true
    ) {
        every { ocContextProvider.getBoolean(R.bool.show_server_url_input) } returns showServerUrlInput
        every { ocContextProvider.getString(R.string.server_url) } returns serverUrl
        every { ocContextProvider.getBoolean(R.bool.use_login_background_image) } returns showLoginBackGroundImage
        every { ocContextProvider.getBoolean(R.bool.show_welcome_link) } returns showWelcomeLink

        activityScenario = ActivityScenario.launch(LoginActivity::class.java)
    }

    @Test
    fun initialViewStatus_notBrandedOptions() {
        launchTest()

        onView(withId(R.id.login_background_image)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.thumbnail)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.centeredRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.instructions_message)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.hostUrlFrame)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.hostUrlInput)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedCheckServerButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.server_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.auth_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.loginButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.welcome_link)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun initialViewStatus_brandedOptions_serverInfoInSetup() {
        launchTest(showServerUrlInput = false, serverUrl = OC_SERVER_INFO.baseUrl)

        onView(withId(R.id.login_background_image)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.thumbnail)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.centeredRefreshButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.instructions_message)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.hostUrlFrame)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.hostUrlInput)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.embeddedCheckServerButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.embeddedRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.server_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.auth_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.loginButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.welcome_link)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        verify(exactly = 1) { ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl) }

        onView(withId(R.id.thumbnail)).perform(click())

        verify(exactly = 2) { ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl) }

        onView(withId(R.id.centeredRefreshButton)).perform(click())

        verify(exactly = 3) { ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl) }

    }

    @Test
    fun initialViewStatus_brandedOptions_dontUseLoginBackgroundImage() {
        launchTest(showLoginBackGroundImage = false)

        onView(withId(R.id.login_background_image)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.thumbnail)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.centeredRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.instructions_message)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.hostUrlFrame)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.hostUrlInput)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedCheckServerButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.server_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.auth_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.loginButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.welcome_link)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun initialViewStatus_brandedOptions_dontShowWelcomeLink() {
        launchTest(showWelcomeLink = false)

        onView(withId(R.id.login_background_image)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.thumbnail)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.centeredRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.instructions_message)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.hostUrlFrame)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.hostUrlInput)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedCheckServerButton)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.embeddedRefreshButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.server_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_username)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password_container)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.account_password)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.auth_status_text)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.loginButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.welcome_link)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun checkServerInfo_clickButton_callGetServerInfo() {
        launchTest()
        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText(OC_SERVER_INFO.baseUrl))

        onView(withId(R.id.embeddedCheckServerButton))
            .perform(click())

        verify(exactly = 1) { ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl) }
    }

    @Test
    fun checkServerInfo_clickLogo_callGetServerInfo() {
        launchTest()
        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText(OC_SERVER_INFO.baseUrl))

        onView(withId(R.id.thumbnail))
            .perform(click())

        verify(exactly = 1) { ocAuthenticationViewModel.getServerInfo(OC_SERVER_INFO.baseUrl) }
    }

    @Test
    fun checkServerInfo_isLoading_show() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Loading()))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.auth_testing_connection)))
    }

    @Test
    fun checkServerInfo_isSuccess_updateUrlInput() {
        launchTest()
        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText("demo.owncloud.com"))

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(isSecureConnection = true))))

        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .check(matches(withText(SERVER_INFO_BASIC.baseUrl)))
    }

    @Test
    fun checkServerInfo_isSuccess_Secure() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(isSecureConnection = true))))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.auth_secure_connection)))
    }

    @Test
    fun checkServerInfo_isSuccess_NotSecure() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(isSecureConnection = false))))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.auth_connection_established)))
    }

    @Test
    fun checkServerInfo_isSuccess_Basic() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_BasicToBearer() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_Bearer() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_BearerToBasic() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_None() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(authenticationMethod = AuthenticationMethod.NONE))))

        checkNoneFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_NoneToBasic() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(authenticationMethod = AuthenticationMethod.NONE))))

        checkNoneFieldsVisibility()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_NoneToBearer() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC.copy(authenticationMethod = AuthenticationMethod.NONE))))

        checkNoneFieldsVisibility()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()
    }

    @Test
    fun checkServerInfo_isSuccess_modifyUrlInput() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()

        onView(withId(R.id.account_username))
            .perform(typeText("username"))

        onView(withId(R.id.account_password))
            .perform(typeText("password"))

        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText("demo.owncloud.com"))

        onView(withId(R.id.account_username))
            .check(matches(withText("")))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.account_password))
            .check(matches(withText("")))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.loginButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        serverInfoLiveData.postValue(Event(UIResult.Success(SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()

        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText("demo.owncloud.com"))

        onView(withId(R.id.loginButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun checkServerInfo_isError_emptyUrl() {
        launchTest()

        onView(withId(R.id.hostUrlInput))
            .check(matches(isDisplayed()))
            .perform(typeText(""))

        onView(withId(R.id.thumbnail))
            .perform(click())

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.auth_can_not_auth_against_server)))

        verify(exactly = 0) { ocAuthenticationViewModel.getServerInfo(any()) }
    }

    @Test
    fun checkServerInfo_isError_ownCloudVersionNotSupported() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(OwncloudVersionNotSupportedException())))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.server_not_supported)))
    }

    @Test
    fun checkServerInfo_isError_noNetworkConnection() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(NoNetworkConnectionException())))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.error_no_network_connection)))
    }

    @Test
    fun checkServerInfo_isError_otherExceptions() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(ServerNotReachableException())))

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.network_host_not_available)))
    }

    private fun checkBasicFieldsVisibility() {
        launchTest()

        onView(withId(R.id.account_username))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText("")))

        onView(withId(R.id.account_password))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText("")))

        onView(withId(R.id.loginButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.auth_status_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun checkBearerFieldsVisibility() {
        launchTest()

        onView(withId(R.id.account_username))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.account_password))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.loginButton))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        onView(withId(R.id.auth_status_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun checkNoneFieldsVisibility() {
        launchTest()

        onView(withId(R.id.server_status_text))
            .check(matches(isDisplayed()))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.auth_unsupported_auth_method)))

        onView(withId(R.id.account_username))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.account_password))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.loginButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.auth_status_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    companion object {
        val SERVER_INFO_BASIC = OC_SERVER_INFO
        val SERVER_INFO_BEARER = OC_SERVER_INFO.copy(authenticationMethod = AuthenticationMethod.BEARER_TOKEN)
    }
}
