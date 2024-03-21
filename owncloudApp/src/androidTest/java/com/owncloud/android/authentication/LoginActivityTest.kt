/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.accounts.AccountManager.KEY_ACCOUNT_TYPE
import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.parseError
import com.owncloud.android.presentation.authentication.ACTION_UPDATE_EXPIRED_TOKEN
import com.owncloud.android.presentation.authentication.ACTION_UPDATE_TOKEN
import com.owncloud.android.presentation.authentication.AuthenticationViewModel
import com.owncloud.android.presentation.authentication.BASIC_TOKEN_TYPE
import com.owncloud.android.presentation.authentication.EXTRA_ACCOUNT
import com.owncloud.android.presentation.authentication.EXTRA_ACTION
import com.owncloud.android.presentation.authentication.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.presentation.authentication.LoginActivity
import com.owncloud.android.presentation.authentication.OAUTH_TOKEN_TYPE
import com.owncloud.android.presentation.authentication.oauth.OAuthViewModel
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.presentation.settings.SettingsViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.MdmProvider
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_BASIC_PASSWORD
import com.owncloud.android.testutil.OC_BASIC_USERNAME
import com.owncloud.android.testutil.OC_INSECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BEARER_AUTH
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY
import com.owncloud.android.utils.NO_MDM_RESTRICTION_YET
import com.owncloud.android.utils.matchers.assertVisibility
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.isEnabled
import com.owncloud.android.utils.matchers.isFocusable
import com.owncloud.android.utils.matchers.withText
import com.owncloud.android.utils.mockIntentToComponent
import com.owncloud.android.utils.replaceText
import com.owncloud.android.utils.scrollAndClick
import com.owncloud.android.utils.typeText
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class LoginActivityTest {

    private lateinit var activityScenario: ActivityScenario<LoginActivity>

    private lateinit var authenticationViewModel: AuthenticationViewModel
    private lateinit var oauthViewModel: OAuthViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var ocContextProvider: ContextProvider
    private lateinit var mdmProvider: MdmProvider
    private lateinit var context: Context

    private lateinit var loginResultLiveData: MutableLiveData<Event<UIResult<String>>>
    private lateinit var serverInfoLiveData: MutableLiveData<Event<UIResult<ServerInfo>>>
    private lateinit var supportsOauth2LiveData: MutableLiveData<Event<UIResult<Boolean>>>
    private lateinit var baseUrlLiveData: MutableLiveData<Event<UIResult<String>>>
    private lateinit var accountDiscoveryLiveData: MutableLiveData<Event<UIResult<Unit>>>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        authenticationViewModel = mockk(relaxed = true)
        oauthViewModel = mockk(relaxed = true)
        settingsViewModel = mockk(relaxUnitFun = true)
        ocContextProvider = mockk(relaxed = true)
        mdmProvider = mockk(relaxed = true)

        loginResultLiveData = MutableLiveData()
        serverInfoLiveData = MutableLiveData()
        supportsOauth2LiveData = MutableLiveData()
        baseUrlLiveData = MutableLiveData()
        accountDiscoveryLiveData = MutableLiveData()

        every { authenticationViewModel.loginResult } returns loginResultLiveData
        every { authenticationViewModel.serverInfo } returns serverInfoLiveData
        every { authenticationViewModel.supportsOAuth2 } returns supportsOauth2LiveData
        every { authenticationViewModel.baseUrl } returns baseUrlLiveData
        every { authenticationViewModel.accountDiscovery } returns accountDiscoveryLiveData
        every { settingsViewModel.isThereAttachedAccount() } returns false

        stopKoin()

        startKoin {
            context
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        authenticationViewModel
                    }
                    viewModel {
                        oauthViewModel
                    }
                    viewModel {
                        settingsViewModel
                    }
                    factory {
                        ocContextProvider
                    }
                    factory {
                        mdmProvider
                    }
                }
            )
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun launchTest(
        showServerUrlInput: Boolean = true,
        serverUrl: String = "",
        showLoginBackGroundImage: Boolean = true,
        showWelcomeLink: Boolean = true,
        accountType: String = "owncloud",
        loginWelcomeText: String = "",
        webfingerLookupServer: String = "",
        intent: Intent? = null
    ) {
        every { mdmProvider.getBrandingBoolean(CONFIGURATION_SERVER_URL_INPUT_VISIBILITY, R.bool.show_server_url_input) } returns showServerUrlInput
        every { mdmProvider.getBrandingString(CONFIGURATION_SERVER_URL, R.string.server_url) } returns serverUrl
        every { mdmProvider.getBrandingString(NO_MDM_RESTRICTION_YET, R.string.webfinger_lookup_server) } returns webfingerLookupServer
        every { ocContextProvider.getBoolean(R.bool.use_login_background_image) } returns showLoginBackGroundImage
        every { ocContextProvider.getBoolean(R.bool.show_welcome_link) } returns showWelcomeLink
        every { ocContextProvider.getString(R.string.account_type) } returns accountType
        every { ocContextProvider.getString(R.string.login_welcome_text) } returns loginWelcomeText
        every { ocContextProvider.getString(R.string.app_name) } returns BRANDED_APP_NAME

        activityScenario = if (intent == null) {
            ActivityScenario.launch(LoginActivity::class.java)
        } else {
            ActivityScenario.launch(intent)
        }
    }

    @Test
    fun initialViewStatus_notBrandedOptions() {
        launchTest()

        assertViewsDisplayed()
        assertWebfingerFlowDisplayed(webfingerEnabled = false)
    }

    @Test
    fun initialViewStatus_brandedOptions_webfinger() {
        launchTest(webfingerLookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertWebfingerFlowDisplayed(webfingerEnabled = true)
    }

    @Test
    fun initialViewStatus_brandedOptions_serverInfoInSetup() {
        launchTest(showServerUrlInput = false, serverUrl = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertViewsDisplayed(
            showHostUrlFrame = false,
            showHostUrlInput = false,
            showCenteredRefreshButton = true,
            showEmbeddedCheckServerButton = false
        )
    }

    @Test
    fun initialViewStatus_brandedOptions_serverInfoInSetup_connectionFails() {

        launchTest(showServerUrlInput = false, serverUrl = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        serverInfoLiveData.postValue(Event(UIResult.Error(NoNetworkConnectionException())))

        R.id.centeredRefreshButton.isDisplayed(true)
        R.id.centeredRefreshButton.scrollAndClick()

        verify(exactly = 1) { authenticationViewModel.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true) }
        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        R.id.centeredRefreshButton.isDisplayed(false)
    }

    @Test
    fun initialViewStatus_brandedOptions_dontUseLoginBackgroundImage() {
        launchTest(showLoginBackGroundImage = false)

        assertViewsDisplayed(showLoginBackGroundImage = false)
    }

    @Test
    fun initialViewStatus_brandedOptions_dontShowWelcomeLink() {
        launchTest(showWelcomeLink = false)

        assertViewsDisplayed(showWelcomeLink = false)
    }

    @Test
    fun initialViewStatus_brandedOptions_customWelcomeText() {
        launchTest(showWelcomeLink = true, loginWelcomeText = CUSTOM_WELCOME_TEXT)

        assertViewsDisplayed(showWelcomeLink = true)

        R.id.welcome_link.withText(CUSTOM_WELCOME_TEXT)
    }

    @Test
    fun initialViewStatus_brandedOptions_defaultWelcomeText() {
        launchTest(showWelcomeLink = true, loginWelcomeText = "")

        assertViewsDisplayed(showWelcomeLink = true)

        R.id.welcome_link.withText(String.format(ocContextProvider.getString(R.string.auth_register), BRANDED_APP_NAME))
    }

    @Test
    fun checkServerInfo_clickButton_callGetServerInfo() {
        launchTest()

        R.id.hostUrlInput.typeText(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        R.id.embeddedCheckServerButton.scrollAndClick()

        verify(exactly = 1) { authenticationViewModel.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true) }
    }

    @Test
    fun checkServerInfo_clickLogo_callGetServerInfo() {
        launchTest()
        R.id.hostUrlInput.typeText(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        R.id.thumbnail.scrollAndClick()

        verify(exactly = 1) { authenticationViewModel.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true) }
    }

    @Test
    fun checkServerInfo_isLoading_show() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Loading()))

        with(R.id.server_status_text) {
            isDisplayed(true)
            withText(R.string.auth_testing_connection)
        }
    }

    @Test
    fun checkServerInfo_isSuccess_updateUrlInput() {
        launchTest()
        R.id.hostUrlInput.typeText("demo.owncloud.com")

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        R.id.hostUrlInput.withText(SECURE_SERVER_INFO_BASIC.baseUrl)
    }

    @Test
    fun checkServerInfo_isSuccess_Secure() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.auth_secure_connection)
        }
    }

    @Test
    fun checkServerInfo_isSuccess_NotSecure() {
        launchTest()
        serverInfoLiveData.postValue(Event(UIResult.Success(INSECURE_SERVER_INFO_BASIC)))

        onView(withText(R.string.insecure_http_url_title_dialog)).check(matches(isDisplayed()))
        onView(withText(R.string.insecure_http_url_message_dialog)).check(matches(isDisplayed()))
        onView(withText(R.string.insecure_http_url_continue_button)).inRoot(isDialog()).check(matches(isDisplayed())).perform(click())

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.auth_connection_established)
        }
    }

    @Test
    fun checkServerInfo_isSuccess_Basic() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility(loginButtonShouldBeVisible = false)
    }

    @Test
    fun checkServerInfo_isSuccess_Bearer() {
        Intents.init()
        launchTest()
        avoidOpeningChromeCustomTab()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()
        Intents.release()
    }

    @Test
    fun checkServerInfo_isSuccess_basicModifyUrlInput() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        checkBasicFieldsVisibility()

        R.id.account_username.typeText(OC_BASIC_USERNAME)

        R.id.account_password.typeText(OC_BASIC_PASSWORD)

        R.id.hostUrlInput.typeText("anything")

        with(R.id.account_username) {
            withText("")
            assertVisibility(Visibility.GONE)
        }

        with(R.id.account_password) {
            withText("")
            assertVisibility(Visibility.GONE)
        }

        R.id.loginButton.assertVisibility(Visibility.GONE)
    }

    @Test
    fun checkServerInfo_isSuccess_bearerModifyUrlInput() {
        Intents.init()
        launchTest()
        avoidOpeningChromeCustomTab()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BEARER)))

        checkBearerFieldsVisibility()

        R.id.hostUrlInput.typeText("anything")

        R.id.auth_status_text.assertVisibility(Visibility.GONE)
        Intents.release()
    }

    @Test
    fun checkServerInfo_isError_emptyUrl() {
        launchTest()

        R.id.hostUrlInput.typeText("")

        R.id.embeddedCheckServerButton.scrollAndClick()

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.auth_can_not_auth_against_server)
        }

        verify(exactly = 0) { authenticationViewModel.getServerInfo(any()) }
    }

    @Test
    fun checkServerInfo_isError_ownCloudVersionNotSupported() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(OwncloudVersionNotSupportedException())))

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.server_not_supported)
        }
    }

    @Test
    fun checkServerInfo_isError_noNetworkConnection() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(NoNetworkConnectionException())))

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.error_no_network_connection)
        }
    }

    @Test
    fun checkServerInfo_isError_otherExceptions() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Error(ServerNotReachableException())))

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(R.string.network_host_not_available)
        }
    }

    @Test
    fun loginBasic_callLoginBasic() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        R.id.account_username.typeText(OC_BASIC_USERNAME)

        R.id.account_password.typeText(OC_BASIC_PASSWORD)

        with(R.id.loginButton) {
            isDisplayed(true)
            scrollAndClick()
        }

        verify(exactly = 1) { authenticationViewModel.loginBasic(OC_BASIC_USERNAME, OC_BASIC_PASSWORD, null) }
    }

    @Test
    fun loginBasic_callLoginBasic_trimUsername() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        R.id.account_username.typeText("  $OC_BASIC_USERNAME  ")

        R.id.account_password.typeText(OC_BASIC_PASSWORD)

        with(R.id.loginButton) {
            isDisplayed(true)
            scrollAndClick()
        }

        verify(exactly = 1) { authenticationViewModel.loginBasic(OC_BASIC_USERNAME, OC_BASIC_PASSWORD, null) }
    }

    @Test
    fun loginBasic_showOrHideFields() {
        launchTest()

        serverInfoLiveData.postValue(Event(UIResult.Success(SECURE_SERVER_INFO_BASIC)))

        R.id.account_username.typeText(OC_BASIC_USERNAME)

        R.id.loginButton.isDisplayed(false)

        R.id.account_password.typeText(OC_BASIC_PASSWORD)

        R.id.loginButton.isDisplayed(true)

        R.id.account_username.replaceText("")

        R.id.loginButton.isDisplayed(false)

    }

    @Test
    fun login_isLoading() {
        launchTest()

        loginResultLiveData.postValue(Event(UIResult.Loading()))

        with(R.id.auth_status_text) {
            withText(R.string.auth_trying_to_login)
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
        }
    }

    @Test
    fun login_isSuccess_finishResultCode() {
        launchTest()

        loginResultLiveData.postValue(Event(UIResult.Success(data = "Account_name")))
        accountDiscoveryLiveData.postValue(Event(UIResult.Success()))

        assertEquals(activityScenario.result.resultCode, RESULT_OK)
        val accountName: String? = activityScenario.result?.resultData?.extras?.getString(KEY_ACCOUNT_NAME)
        val accountType: String? = activityScenario.result?.resultData?.extras?.getString(KEY_ACCOUNT_TYPE)

        assertNotNull(accountName)
        assertNotNull(accountType)
        assertEquals("Account_name", accountName)
        assertEquals("owncloud", accountType)
    }

    @Test
    fun login_isSuccess_finishResultCodeBrandedAccountType() {
        launchTest(accountType = "notOwnCloud")

        loginResultLiveData.postValue(Event(UIResult.Success(data = "Account_name")))
        accountDiscoveryLiveData.postValue(Event(UIResult.Success()))

        assertEquals(activityScenario.result.resultCode, RESULT_OK)
        val accountName: String? = activityScenario.result?.resultData?.extras?.getString(KEY_ACCOUNT_NAME)
        val accountType: String? = activityScenario.result?.resultData?.extras?.getString(KEY_ACCOUNT_TYPE)

        assertNotNull(accountName)
        assertNotNull(accountType)
        assertEquals("Account_name", accountName)
        assertEquals("notOwnCloud", accountType)
    }

    @Test
    fun login_isError_NoNetworkConnectionException() {
        launchTest()

        loginResultLiveData.postValue(Event(UIResult.Error(NoNetworkConnectionException())))

        R.id.server_status_text.withText(R.string.error_no_network_connection)

        checkBasicFieldsVisibility(fieldsShouldBeVisible = false)
    }

    @Test
    fun login_isError_ServerNotReachableException() {
        launchTest()

        loginResultLiveData.postValue(Event(UIResult.Error(ServerNotReachableException())))

        R.id.server_status_text.withText(R.string.error_no_network_connection)

        checkBasicFieldsVisibility(fieldsShouldBeVisible = false)
    }

    @Test
    fun login_isError_OtherException() {
        launchTest()

        val exception = UnauthorizedException()

        loginResultLiveData.postValue(Event(UIResult.Error(exception)))

        R.id.auth_status_text.withText(exception.parseError("", context.resources, true) as String)
    }

    @Test
    fun intent_withSavedAccount_viewModelCalls() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
        }

        launchTest(intent = intentWithAccount)

        verify(exactly = 1) { authenticationViewModel.supportsOAuth2(OC_ACCOUNT.name) }
        verify(exactly = 1) { authenticationViewModel.getBaseUrl(OC_ACCOUNT.name) }
    }

    @Test
    fun supportsOAuth_isSuccess_actionUpdateExpiredTokenOAuth() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
            putExtra(EXTRA_ACTION, ACTION_UPDATE_EXPIRED_TOKEN)
            putExtra(KEY_AUTH_TOKEN_TYPE, OAUTH_TOKEN_TYPE)
        }

        launchTest(intent = intentWithAccount)

        supportsOauth2LiveData.postValue(Event(UIResult.Success(true)))

        with(R.id.instructions_message) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(context.getString(R.string.auth_expired_oauth_token_toast))
        }
    }

    @Test
    fun supportsOAuth_isSuccess_actionUpdateToken() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
            putExtra(EXTRA_ACTION, ACTION_UPDATE_TOKEN)
            putExtra(KEY_AUTH_TOKEN_TYPE, OC_AUTH_TOKEN_TYPE)
        }

        launchTest(intent = intentWithAccount)

        supportsOauth2LiveData.postValue(Event(UIResult.Success(false)))

        R.id.instructions_message.assertVisibility(Visibility.GONE)
    }

    @Test
    fun supportsOAuth_isSuccess_actionUpdateExpiredTokenBasic() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
            putExtra(EXTRA_ACTION, ACTION_UPDATE_EXPIRED_TOKEN)
            putExtra(KEY_AUTH_TOKEN_TYPE, BASIC_TOKEN_TYPE)
        }

        launchTest(intent = intentWithAccount)

        supportsOauth2LiveData.postValue(Event(UIResult.Success(false)))

        with(R.id.instructions_message) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
            withText(context.getString(R.string.auth_expired_basic_auth_toast))
        }
    }

    @Test
    fun getBaseUrl_isSuccess_updatesBaseUrl() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
        }

        launchTest(intent = intentWithAccount)

        baseUrlLiveData.postValue(Event(UIResult.Success(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)))

        with(R.id.hostUrlInput) {
            withText(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)
            assertVisibility(Visibility.VISIBLE)
            isDisplayed(true)
            isEnabled(false)
            isFocusable(false)
        }

        verify(exactly = 0) { authenticationViewModel.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl) }
    }

    @Test
    fun getBaseUrlAndActionNotCreate_isSuccess_updatesBaseUrl() {
        val intentWithAccount = Intent(context, LoginActivity::class.java).apply {
            putExtra(EXTRA_ACCOUNT, OC_ACCOUNT)
            putExtra(EXTRA_ACTION, ACTION_UPDATE_EXPIRED_TOKEN)
        }

        launchTest(intent = intentWithAccount)

        baseUrlLiveData.postValue(Event(UIResult.Success(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)))

        with(R.id.hostUrlInput) {
            withText(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)
            assertVisibility(Visibility.VISIBLE)
            isDisplayed(true)
            isEnabled(false)
            isFocusable(false)
        }

        verify(exactly = 1) { authenticationViewModel.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl) }
    }

    @Test
    fun settingsLink() {
        Intents.init()
        launchTest()

        closeSoftKeyboard()

        mockIntentToComponent(RESULT_OK, SettingsActivity::class.java.name)
        onView(withId(R.id.settings_link)).perform(click())
        intended(hasComponent(SettingsActivity::class.java.name))

        Intents.release()
    }

    private fun avoidOpeningChromeCustomTab() {
        Intents.intending(allOf(IntentMatchers.hasAction(Intent.ACTION_VIEW)))
            .respondWith(Instrumentation.ActivityResult(RESULT_OK, null))
    }

    private fun checkBasicFieldsVisibility(
        fieldsShouldBeVisible: Boolean = true,
        loginButtonShouldBeVisible: Boolean = false
    ) {
        val visibilityMatcherFields = if (fieldsShouldBeVisible) Visibility.VISIBLE else Visibility.GONE
        val visibilityMatcherLoginButton = if (loginButtonShouldBeVisible) Visibility.VISIBLE else Visibility.GONE

        with(R.id.account_username) {
            isDisplayed(fieldsShouldBeVisible)
            assertVisibility(visibilityMatcherFields)
            withText("")
        }

        with(R.id.account_password) {
            isDisplayed(fieldsShouldBeVisible)
            assertVisibility(visibilityMatcherFields)
            withText("")
        }

        R.id.loginButton.assertVisibility(visibilityMatcherLoginButton)
        R.id.auth_status_text.assertVisibility(visibilityMatcherLoginButton)
    }

    private fun checkBearerFieldsVisibility() {
        R.id.account_username.assertVisibility(Visibility.GONE)
        R.id.account_password.assertVisibility(Visibility.GONE)
        R.id.auth_status_text.assertVisibility(Visibility.GONE)

        with(R.id.server_status_text) {
            isDisplayed(true)
            assertVisibility(Visibility.VISIBLE)
        }
    }

    private fun assertWebfingerFlowDisplayed(
        webfingerEnabled: Boolean,
    ) {
        R.id.webfinger_layout.isDisplayed(webfingerEnabled)
        R.id.webfinger_username.isDisplayed(webfingerEnabled)
        R.id.webfinger_button.isDisplayed(webfingerEnabled)
    }

    private fun assertViewsDisplayed(
        showLoginBackGroundImage: Boolean = true,
        showThumbnail: Boolean = true,
        showCenteredRefreshButton: Boolean = false,
        showInstructionsMessage: Boolean = false,
        showHostUrlFrame: Boolean = true,
        showHostUrlInput: Boolean = true,
        showEmbeddedCheckServerButton: Boolean = true,
        showEmbeddedRefreshButton: Boolean = false,
        showServerStatusText: Boolean = false,
        showAccountUsername: Boolean = false,
        showAccountPassword: Boolean = false,
        showAuthStatus: Boolean = false,
        showLoginButton: Boolean = false,
        showWelcomeLink: Boolean = true
    ) {
        R.id.login_background_image.isDisplayed(displayed = showLoginBackGroundImage)
        R.id.thumbnail.isDisplayed(displayed = showThumbnail)
        R.id.centeredRefreshButton.isDisplayed(displayed = showCenteredRefreshButton)
        R.id.instructions_message.isDisplayed(displayed = showInstructionsMessage)
        R.id.hostUrlFrame.isDisplayed(displayed = showHostUrlFrame)
        R.id.hostUrlInput.isDisplayed(displayed = showHostUrlInput)
        R.id.embeddedCheckServerButton.isDisplayed(displayed = showEmbeddedCheckServerButton)
        R.id.embeddedRefreshButton.isDisplayed(displayed = showEmbeddedRefreshButton)
        R.id.server_status_text.isDisplayed(displayed = showServerStatusText)
        R.id.account_username_container.isDisplayed(displayed = showAccountUsername)
        R.id.account_username.isDisplayed(displayed = showAccountUsername)
        R.id.account_password_container.isDisplayed(displayed = showAccountPassword)
        R.id.account_password.isDisplayed(displayed = showAccountPassword)
        R.id.auth_status_text.isDisplayed(displayed = showAuthStatus)
        R.id.loginButton.isDisplayed(displayed = showLoginButton)
        R.id.welcome_link.isDisplayed(displayed = showWelcomeLink)
    }

    companion object {
        val SECURE_SERVER_INFO_BASIC = OC_SECURE_SERVER_INFO_BASIC_AUTH
        val INSECURE_SERVER_INFO_BASIC = OC_INSECURE_SERVER_INFO_BASIC_AUTH
        val SECURE_SERVER_INFO_BEARER = OC_SECURE_SERVER_INFO_BEARER_AUTH
        private const val CUSTOM_WELCOME_TEXT = "Welcome to this test"
        private const val BRANDED_APP_NAME = "BrandedAppName"
    }
}
