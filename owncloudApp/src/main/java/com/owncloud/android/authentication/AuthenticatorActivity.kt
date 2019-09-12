/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.authentication

import android.accounts.Account
import android.accounts.AccountManager
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast

import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.fragment.app.DialogFragment
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.authentication.oauth.OAuth2Constants
import com.owncloud.android.lib.common.authentication.oauth.OAuth2GetAccessTokenOperation
import com.owncloud.android.lib.common.authentication.oauth.OAuth2GrantType
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ProvidersRegistry
import com.owncloud.android.lib.common.authentication.oauth.OAuth2QueryParser
import com.owncloud.android.lib.common.authentication.oauth.OAuth2RequestBuilder
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation.UserInfo
import com.owncloud.android.operations.AuthenticationMethod
import com.owncloud.android.operations.GetServerInfoOperation
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PreferenceUtils

import java.util.ArrayList

import android.content.Intent.ACTION_VIEW

/**
 * This Activity is used to add an ownCloud account to the App
 */
class AuthenticatorActivity : AccountAuthenticatorActivity(), OnRemoteOperationListener, OnFocusChangeListener,
    OnEditorActionListener, AuthenticatorAsyncTask.OnAuthenticatorTaskListener {

    // ChromeCustomTab
    internal var mCustomTabPackageName: String? = null

    /// parameters from EXTRAs in starter Intent
    private var action: Byte = 0
    private var account: Account? = null
    private var authTokenType: String? = null

    /// activity-level references / state
    private val handler = Handler()
    private var operationsServiceConnection: ServiceConnection? = null
    private var operationsServiceBinder: OperationsServiceBinder? = null
    private var accountMgr: AccountManager? = null

    /// Server PRE-Fragment elements
    private var hostUrlInput: EditText? = null
    private var refreshButton: View? = null
    private var serverStatusView: TextView? = null

    private var hostUrlInputWatcher: TextWatcher? = null

    private var serverStatusText: String? = ""

    private var serverStatusIcon = 0

    private var serverIsChecked = false
    private var serverIsValid = false
    private var pendingAutoCheck = false

    private var serverInfo = GetServerInfoOperation.ServerInfo()

    /// Authentication PRE-Fragment elements
    private var usernameInput: EditText? = null
    private var passwordInput: EditText? = null
    private var checkServerButton: View? = null
    private var loginButton: View? = null
    private var authStatusView: TextView? = null

    private var usernamePasswordInputWatcher: TextWatcher? = null

    private var authStatusText: String? = ""

    private var authStatusIcon = 0

    private var authToken: String? = ""
    private var refreshToken: String? = ""
    private var asyncTask: AuthenticatorAsyncTask? = null

    private var isFirstAuthAttempt: Boolean = false

    /// Identifier of operation in progress which result shouldn't be lost
    private var waitingForOpId = java.lang.Long.MAX_VALUE

    private val BASIC_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypePass(
        MainApp.accountType
    )
    private val OAUTH_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypeAccessToken(
        MainApp.accountType
    )

    private var customTabsClient: CustomTabsClient? = null
    private val customTabServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            customTabsClient = client
            customTabsClient?.warmup(0)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            customTabsClient = null
        }
    }

    private// Get default VIEW intent handler.
    // Get all apps that can handle VIEW intents.
    // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
    // and service calls.
    // try getting the ChromeCustomTab of the default browser
    // try getting the ChromeCustomTab of the first browser that support its
    // return null if we don't have a browser installed that can handle ChromeCustomTabs
    val customTabPackageName: String?
        get() {

            val pm = packageManager
            val activityIntent = Intent(ACTION_VIEW, Uri.parse("https://owncloud.org"))
            val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
            val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
            val packagesSupportingCustomTabs = ArrayList<String>()
            for (info in resolvedActivityList) {
                val serviceIntent = Intent()
                serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
                serviceIntent.setPackage(info.activityInfo.packageName)
                if (pm.resolveService(serviceIntent, 0) != null) {
                    packagesSupportingCustomTabs.add(info.activityInfo.packageName)
                }
            }
            return if (defaultViewHandlerInfo != null && packagesSupportingCustomTabs.contains(defaultViewHandlerInfo.activityInfo.packageName)) {
                defaultViewHandlerInfo.activityInfo.packageName
            } else if (packagesSupportingCustomTabs.size >= 1) {
                packagesSupportingCustomTabs[0]
            } else {
                null
            }
        }

    private val isPasswordVisible: Boolean
        get() = passwordInput!!.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    /**
     * {@inheritDoc}
     *
     *
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /// protection against screen recording
        if (!MainApp.isDeveloper) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } // else, let it go, or taking screenshots & testing will not be possible

        // Workaround, for fixing a problem with Android Library Suppor v7 19
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (supportActionBar != null) {
            supportActionBar?.hide()
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }

        isFirstAuthAttempt = true

        // bind to Operations Service
        operationsServiceConnection = OperationsServiceConnection()
        if (!bindService(
                Intent(this, OperationsService::class.java),
                operationsServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        ) {
            Toast.makeText(
                this,
                R.string.error_cant_bind_to_operations_service,
                Toast.LENGTH_LONG
            )
                .show()
            //  do not use a Snackbar, finishing right now!
            finish()
        }

        /// init activity state
        accountMgr = AccountManager.get(this)

        /// get input values
        action = intent.getByteExtra(EXTRA_ACTION, ACTION_CREATE)
        account = intent.extras?.getParcelable(EXTRA_ACCOUNT)
        if (savedInstanceState == null) {
            initAuthTokenType()
        } else {
            authTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE)
            waitingForOpId = savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID)
            isFirstAuthAttempt = savedInstanceState.getBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG)
        }

        /// load user interface
        setContentView(R.layout.account_setup)

        // Allow or disallow touches with other visible windows
        val loginLayout = findViewById<FrameLayout>(R.id.login_layout)
        loginLayout.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        // Set login background color or image
        if (!resources.getBoolean(R.bool.use_login_background_image)) {
            loginLayout.setBackgroundColor(
                resources.getColor(R.color.login_background_color)
            )
        } else {
            findViewById<View>(R.id.login_background_image).visibility = View.VISIBLE
        }

        /// initialize general UI elements
        initOverallUi()

        checkServerButton = findViewById(R.id.embeddedCheckServerButton)

        checkServerButton?.setOnClickListener { view -> checkOcServer() }

        findViewById<View>(R.id.centeredRefreshButton).setOnClickListener { view -> checkOcServer() }
        findViewById<View>(R.id.embeddedRefreshButton).setOnClickListener { view -> checkOcServer() }

        loginButton = findViewById(R.id.loginButton)
        loginButton?.setOnClickListener { view -> onLoginClick() }

        /// initialize block to be moved to single Fragment to check server and get info about it
        initServerPreFragment(savedInstanceState)

        /// initialize block to be moved to single Fragment to retrieve and validate credentials
        initAuthorizationPreFragment(savedInstanceState)

        mCustomTabPackageName = customTabPackageName

        if (mCustomTabPackageName != null) {
            CustomTabsClient.bindCustomTabsService(this, mCustomTabPackageName, customTabServiceConnection)
        }
    }

    public override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && intent.action != null && intent.action == ACTION_VIEW) {
            getOAuth2AccessTokenFromCapturedRedirection(intent.data!!)
        }
    }

    private fun initAuthTokenType() {
        authTokenType = intent.extras?.getString(AccountAuthenticator.KEY_AUTH_TOKEN_TYPE)
        if (authTokenType == null) {
            if (account != null) {
                val oAuthRequired = accountMgr?.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2) != null
                authTokenType = if (oAuthRequired) OAUTH_TOKEN_TYPE else BASIC_TOKEN_TYPE
            } else {
                // OAuth will be the default authentication method
                authTokenType = ""
            }
        }
    }

    /**
     * Configures elements in the user interface under direct control of the Activity.
     */
    private fun initOverallUi() {
        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        val isWelcomeLinkVisible = resources.getBoolean(R.bool.show_welcome_link)

        var instructionsMessageText: String? = null
        if (action == ACTION_UPDATE_EXPIRED_TOKEN) {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.accountType) == authTokenType) {
                instructionsMessageText = getString(R.string.auth_expired_oauth_token_toast)
            } else {
                instructionsMessageText = getString(R.string.auth_expired_basic_auth_toast)
            }
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        val welcomeLink = findViewById<Button>(R.id.welcome_link)
        welcomeLink.visibility = if (isWelcomeLinkVisible) View.VISIBLE else View.GONE
        welcomeLink.text = String.format(getString(R.string.auth_register), getString(R.string.app_name))

        val instructionsView = findViewById<TextView>(R.id.instructions_message)
        if (instructionsMessageText != null) {
            instructionsView.visibility = View.VISIBLE
            instructionsView.text = instructionsMessageText
        } else {
            instructionsView.visibility = View.GONE
        }
    }

    /**
     * @param savedInstanceState Saved activity state, as in {[.onCreate]
     */
    private fun initServerPreFragment(savedInstanceState: Bundle?) {
        var checkHostUrl = true

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        val isUrlInputAllowed = resources.getBoolean(R.bool.show_server_url_input)
        if (savedInstanceState == null) {
            if (account != null) {
                serverInfo.mBaseUrl = accountMgr?.getUserData(account, Constants.KEY_OC_BASE_URL)
                // TODO do next in a setter for mBaseUrl
                serverInfo.mIsSslConn = serverInfo.mBaseUrl.startsWith("https://")
                serverInfo.mVersion = AccountUtils.getServerVersion(account)
            } else {
                serverInfo.mBaseUrl = getString(R.string.server_url).trim { it <= ' ' }
                serverInfo.mIsSslConn = serverInfo.mBaseUrl.startsWith("https://")
            }
        } else {
            serverStatusText = savedInstanceState.getString(KEY_SERVER_STATUS_TEXT)
            serverStatusIcon = savedInstanceState.getInt(KEY_SERVER_STATUS_ICON)

            serverIsValid = savedInstanceState.getBoolean(KEY_SERVER_VALID)
            serverIsChecked = savedInstanceState.getBoolean(KEY_SERVER_CHECKED)

            // TODO parcelable
            serverInfo.mIsSslConn = savedInstanceState.getBoolean(KEY_IS_SSL_CONN)
            serverInfo.mBaseUrl = savedInstanceState.getString(KEY_HOST_URL_TEXT)
            val ocVersion = savedInstanceState.getString(KEY_OC_VERSION)
            if (ocVersion != null) {
                serverInfo.mVersion = OwnCloudVersion(ocVersion)
            }

            val authenticationMethodNames = savedInstanceState.getStringArrayList(KEY_SERVER_AUTH_METHOD)

            for (authenticationMethodName in authenticationMethodNames!!) {
                serverInfo.mAuthMethods.add(AuthenticationMethod.valueOf(authenticationMethodName))
            }
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        hostUrlInput = findViewById(R.id.hostUrlInput)
        // Convert IDN to Unicode
        hostUrlInput?.setText(DisplayUtils.convertIdn(serverInfo.mBaseUrl, false))
        if (action != ACTION_CREATE) {
            /// lock things that should not change
            hostUrlInput?.isEnabled = false
            hostUrlInput?.isFocusable = false
        }
        if (isUrlInputAllowed) {
            if (serverInfo.mBaseUrl.isEmpty()) {
                checkHostUrl = false
            }
            refreshButton = findViewById(R.id.embeddedRefreshButton)
        } else {
            findViewById<View>(R.id.hostUrlFrame).visibility = View.GONE
            refreshButton = findViewById(R.id.centeredRefreshButton)
        }
        showRefreshButton(
            serverIsChecked && !serverIsValid &&
                    waitingForOpId > Integer.MAX_VALUE
        )
        serverStatusView = findViewById(R.id.server_status_text)
        showServerStatus()

        /// step 3 - bind some listeners and options
        hostUrlInput?.imeOptions = EditorInfo.IME_ACTION_NEXT
        hostUrlInput?.setOnEditorActionListener(this)

        /// step 4 - create listeners that will be bound at onResume
        hostUrlInputWatcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (loginButton!!.isEnabled && serverInfo.mBaseUrl != normalizeUrl(
                        s.toString(),
                        serverInfo.mIsSslConn
                    )
                ) {
                    loginButton?.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (authStatusIcon != 0) {
                    Log_OC.d(TAG, "onTextChanged: hiding authentication status")
                    authStatusIcon = 0
                    authStatusText = ""
                    showAuthStatus()
                }
            }
        }

        usernamePasswordInputWatcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (BASIC_TOKEN_TYPE == authTokenType) {
                    if (usernameInput?.text.toString().trim { it <= ' ' }.isNotEmpty() && 
                        passwordInput?.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                        loginButton?.visibility = View.VISIBLE
                    } else {
                        loginButton?.visibility = View.GONE
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        }

        findViewById<View>(R.id.scroll).setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (hostUrlInput!!.hasFocus()) {
                    checkOcServer()
                }
            }
            false
        }

        /// step 4 - mark automatic check to be started when OperationsService is ready
        pendingAutoCheck = savedInstanceState == null && (action != ACTION_CREATE || checkHostUrl)
    }

    /**
     * @param savedInstanceState Saved activity state, as in {[.onCreate]
     */
    private fun initAuthorizationPreFragment(savedInstanceState: Bundle?) {

        /// step 0 - get UI elements in layout
        usernameInput = findViewById(R.id.account_username)
        passwordInput = findViewById(R.id.account_password)
        authStatusView = findViewById(R.id.auth_status_text)

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        var presetUserName: String? = null
        var isPasswordExposed = false
        if (savedInstanceState == null) {
            if (account != null) {
                presetUserName = com.owncloud.android.lib.common.accounts.AccountUtils.getUsernameForAccount(account)
            }

        } else {
            isPasswordExposed = savedInstanceState.getBoolean(KEY_PASSWORD_EXPOSED, false)
            authStatusText = savedInstanceState.getString(KEY_AUTH_STATUS_TEXT)
            authStatusIcon = savedInstanceState.getInt(KEY_AUTH_STATUS_ICON)
            authToken = savedInstanceState.getString(KEY_AUTH_TOKEN)
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        if (presetUserName != null) {
            usernameInput?.setText(presetUserName)
        }
        if (action != ACTION_CREATE) {
            usernameInput?.isEnabled = false
            usernameInput?.isFocusable = false
        }
        passwordInput?.setText("") // clean password to avoid social hacking
        if (isPasswordExposed) {
            showPassword()
        }
        updateAuthenticationPreFragmentVisibility()
        showAuthStatus()

        if (serverIsValid && BASIC_TOKEN_TYPE != authTokenType) {
            loginButton?.visibility = View.VISIBLE
        } else {
            loginButton?.visibility = View.GONE
        }

        /// step 3 - bind listeners
        // bindings for password input field
        passwordInput?.onFocusChangeListener = this
        passwordInput?.imeOptions = EditorInfo.IME_ACTION_DONE
        passwordInput?.setOnEditorActionListener(this)
        passwordInput?.setOnTouchListener(object : RightDrawableOnTouchListener() {
            override fun onDrawableTouch(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_UP) {
                    this@AuthenticatorActivity.onViewPasswordClick()
                }
                return true
            }
        })

    }

    /**
     * Changes the visibility of input elements depending on
     * the current authorization method.
     */
    private fun updateAuthenticationPreFragmentVisibility() {
        if (AccountTypeUtils.getAuthTokenTypePass(MainApp.accountType) == authTokenType) {
            // basic HTTP authorization
            usernameInput?.visibility = View.VISIBLE
            passwordInput?.visibility = View.VISIBLE

        } else {
            usernameInput?.visibility = View.GONE
            passwordInput?.visibility = View.GONE
        }
    }

    /**
     * Saves relevant state before [.onPause]
     *
     *
     * See [super.onSaveInstanceState]
     */
    override fun onSaveInstanceState(outState: Bundle) {
        //Log_OC.e(TAG, "onSaveInstanceState init" );
        super.onSaveInstanceState(outState)

        /// global state
        outState.putString(KEY_AUTH_TOKEN_TYPE, authTokenType)
        outState.putLong(KEY_WAITING_FOR_OP_ID, waitingForOpId)

        /// Server PRE-fragment state
        outState.putString(KEY_SERVER_STATUS_TEXT, serverStatusText)
        outState.putInt(KEY_SERVER_STATUS_ICON, serverStatusIcon)
        outState.putBoolean(KEY_SERVER_CHECKED, serverIsChecked)
        outState.putBoolean(KEY_SERVER_VALID, serverIsValid)
        outState.putBoolean(KEY_IS_SSL_CONN, serverInfo.mIsSslConn)
        outState.putString(KEY_HOST_URL_TEXT, serverInfo.mBaseUrl)
        if (serverInfo.mVersion != null) {
            outState.putString(KEY_OC_VERSION, serverInfo.mVersion.version)
        }

        val authenticationMethodNames = ArrayList<String>()

        for (authenticationMethod in serverInfo.mAuthMethods) {

            authenticationMethodNames.add(authenticationMethod.name)
        }

        outState.putStringArrayList(KEY_SERVER_AUTH_METHOD, authenticationMethodNames)

        /// Authentication PRE-fragment state
        outState.putBoolean(KEY_PASSWORD_EXPOSED, isPasswordVisible)
        outState.putInt(KEY_AUTH_STATUS_ICON, authStatusIcon)
        outState.putString(KEY_AUTH_STATUS_TEXT, authStatusText)
        outState.putString(KEY_AUTH_TOKEN, authToken)

        /// authentication
        outState.putBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG, isFirstAuthAttempt)

        /// AsyncTask (User and password)
        outState.putString(KEY_USERNAME, usernameInput?.text.toString().trim { it <= ' ' })
        outState.putString(KEY_PASSWORD, passwordInput?.text.toString())

        if (asyncTask != null) {
            asyncTask?.cancel(true)
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, true)
        } else {
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, false)
        }
        asyncTask = null

        //Log_OC.e(TAG, "onSaveInstanceState end" );
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // AsyncTask
        val inProgress = savedInstanceState.getBoolean(KEY_ASYNC_TASK_IN_PROGRESS)
        if (inProgress) {
            val username = savedInstanceState.getString(KEY_USERNAME)
            val password = savedInstanceState.getString(KEY_PASSWORD)
            var credentials: OwnCloudCredentials? = null
            if (BASIC_TOKEN_TYPE == authTokenType) {
                val version = savedInstanceState.getString(KEY_OC_VERSION)
                val ocVersion = if (version != null) OwnCloudVersion(version) else null
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    username,
                    password
                )
            } else if (OAUTH_TOKEN_TYPE == authTokenType) {
                credentials = OwnCloudCredentialsFactory.newBearerCredentials(username, authToken)
            }
            accessRootFolder(credentials)
        }
    }

    /**
     * The redirection triggered by the OAuth authentication server as response to the
     * GET AUTHORIZATION, and deferred in [.onNewIntent], is processed here.
     */
    override fun onResume() {
        super.onResume()

        // bound here to avoid spurious changes triggered by Android on device rotations
        hostUrlInput?.onFocusChangeListener = this
        hostUrlInput?.addTextChangedListener(hostUrlInputWatcher)
        usernameInput?.addTextChangedListener(usernamePasswordInputWatcher)
        passwordInput?.addTextChangedListener(usernamePasswordInputWatcher)

        if (operationsServiceBinder != null) {
            doOnResumeAndBound()
        }
    }

    override fun onPause() {
        if (operationsServiceBinder != null) {
            operationsServiceBinder?.removeOperationListener(this)
        }

        usernameInput?.removeTextChangedListener(usernamePasswordInputWatcher)
        passwordInput?.removeTextChangedListener(usernamePasswordInputWatcher)
        hostUrlInput?.removeTextChangedListener(hostUrlInputWatcher)
        hostUrlInput?.onFocusChangeListener = null

        super.onPause()
    }

    override fun onDestroy() {

        hostUrlInputWatcher = null
        usernamePasswordInputWatcher = null

        if (operationsServiceConnection != null) {
            unbindService(operationsServiceConnection)
            operationsServiceBinder = null
        }

        if (customTabServiceConnection != null && mCustomTabPackageName != null) {
            unbindService(customTabServiceConnection)
        }

        super.onDestroy()
    }

    /**
     * Parses the redirection with the response to the GET AUTHORIZATION request to the
     * OAuth server and requests for the access token (GET ACCESS TOKEN)
     *
     * @param capturedUriFromOAuth2Redirection Redirection after authorization code request ends
     */
    private fun getOAuth2AccessTokenFromCapturedRedirection(capturedUriFromOAuth2Redirection: Uri) {

        // Parse data from OAuth redirection
        val queryParameters = capturedUriFromOAuth2Redirection.query
        val parsedQuery = OAuth2QueryParser().parse(queryParameters)

        if (parsedQuery.keys.contains(OAuth2Constants.KEY_CODE)) {

            /// Showing the dialog with instructions for the user
            val dialog = LoadingDialog.newInstance(R.string.auth_getting_authorization, true)
            dialog.show(supportFragmentManager, WAIT_DIALOG_TAG)

            /// CREATE ACCESS TOKEN to the oAuth server
            val getAccessTokenIntent = Intent()
            getAccessTokenIntent.action = OperationsService.ACTION_OAUTH2_GET_ACCESS_TOKEN

            getAccessTokenIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL,
                serverInfo.mBaseUrl
            )

            getAccessTokenIntent.putExtra(
                OperationsService.EXTRA_OAUTH2_AUTHORIZATION_CODE,
                parsedQuery[OAuth2Constants.KEY_CODE]
            )

            if (operationsServiceBinder != null) {
                Log_OC.i(TAG, "Getting OAuth access token...")
                waitingForOpId = operationsServiceBinder!!.queueNewOperation(getAccessTokenIntent)
            }

        } else {
            // did not obtain authorization code

            if (parsedQuery.keys.contains(OAuth2Constants.KEY_ERROR) && OAuth2Constants.VALUE_ERROR_ACCESS_DENIED == parsedQuery[OAuth2Constants.KEY_ERROR]) {
                onGetOAuthAccessTokenFinish(
                    RemoteOperationResult(ResultCode.OAUTH2_ERROR_ACCESS_DENIED)
                )

            } else {
                onGetOAuthAccessTokenFinish(
                    RemoteOperationResult(ResultCode.OAUTH2_ERROR)
                )
            }

        }
    }

    /**
     * Handles the change of focus on the text inputs for the server URL and the password
     */
    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (view.id == R.id.hostUrlInput) {
            if (!hasFocus) {
                onUrlInputFocusLost()
            } else {
                showRefreshButton(false)
            }

        } else if (view.id == R.id.account_password) {
            onPasswordFocusChanged(hasFocus)
        }
    }

    /**
     * Handles changes in focus on the text input for the server URL.
     *
     *
     * IMPORTANT ENTRY POINT 2: When (!hasFocus), user wrote the server URL and changed to
     * other field. The operation to check the existence of the server in the entered URL is
     * started.
     *
     *
     * When hasFocus:    user 'comes back' to write again the server URL.
     */
    private fun onUrlInputFocusLost() {
        if (serverInfo.mBaseUrl != normalizeUrl(hostUrlInput?.text.toString(), serverInfo.mIsSslConn)) {
            // check server again only if the user changed something in the field
            checkOcServer()
        } else {
            if (serverIsValid && BASIC_TOKEN_TYPE != authTokenType) {
                loginButton?.visibility = View.VISIBLE
            } else {
                loginButton?.visibility = View.GONE
            }
            showRefreshButton(!serverIsValid)
        }
    }

    private fun checkOcServer() {
        var uri = hostUrlInput?.text.toString().trim { it <= ' ' }
        serverIsValid = false
        serverIsChecked = false
        loginButton?.visibility = View.GONE
        serverInfo = GetServerInfoOperation.ServerInfo()
        showRefreshButton(false)

        if (uri.length != 0) {
            uri = stripIndexPhpOrAppsFiles(uri, hostUrlInput!!)
            uri = subdomainToLower(uri, hostUrlInput!!)

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true)
            } catch (ex: IllegalArgumentException) {
                // Let Owncloud library check the error of the malformed URI
            }

            serverStatusText = resources.getString(R.string.auth_testing_connection)
            serverStatusIcon = R.drawable.progress_small
            showServerStatus()

            val getServerInfoIntent = Intent()
            getServerInfoIntent.action = OperationsService.ACTION_GET_SERVER_INFO
            getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL,
                normalizeUrlSuffix(uri)
            )
            if (operationsServiceBinder != null) {
                waitingForOpId = operationsServiceBinder!!.queueNewOperation(getServerInfoIntent)
            } else {
                Log_OC.e(TAG, "Server check tried with OperationService unbound!")
            }

        } else {
            serverStatusText = ""
            serverStatusIcon = 0
            showServerStatus()
        }
    }

    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     *
     *
     * When (hasFocus), the button to toggle password visibility is shown.
     *
     *
     * When (!hasFocus), the button is made invisible and the password is hidden.
     *
     * @param hasFocus 'True' if focus is received, 'false' if is lost
     */
    private fun onPasswordFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            showViewPasswordButton()
        } else {
            hidePassword()
            hidePasswordButton()
        }
    }

    private fun showViewPasswordButton() {
        var drawable = R.drawable.ic_view
        if (isPasswordVisible) {
            drawable = R.drawable.ic_hide
        }
        passwordInput?.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
    }

    private fun hidePasswordButton() {
        passwordInput?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    private fun showPassword() {
        passwordInput?.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        showViewPasswordButton()
    }

    private fun hidePassword() {
        passwordInput?.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        showViewPasswordButton()
    }

    /**
     * Checks the credentials of the user in the root of the ownCloud server
     * before creating a new local account.
     *
     *
     * For basic authorization, a check of existence of the root folder is
     * performed.
     *
     *
     * For OAuth, starts the flow to get an access token; the credentials test
     * is postponed until it is available.
     *
     *
     * IMPORTANT ENTRY POINT 4
     */
    fun onLoginClick() {
        // this check should be unnecessary
        if (serverInfo.mVersion == null ||
            serverInfo.mBaseUrl == null ||
            serverInfo.mBaseUrl.length == 0
        ) {
            serverStatusIcon = R.drawable.common_error
            serverStatusText = resources.getString(R.string.auth_wtf_reenter_URL)
            showServerStatus()
            loginButton?.visibility = View.GONE
            return
        }

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.accountType) == authTokenType) {
            startOauthorization()
        } else {
            checkBasicAuthorization()
        }
    }

    /**
     * Tests the credentials entered by the user performing a check of existence on
     * the root folder of the ownCloud server.
     */
    private fun checkBasicAuthorization() {
        /// get basic credentials entered by user
        val username = usernameInput?.text.toString().trim { it <= ' ' }
        val password = passwordInput?.text.toString()

        /// be gentle with the user
        val dialog = LoadingDialog.newInstance(R.string.auth_trying_to_login, true)
        dialog.show(supportFragmentManager, WAIT_DIALOG_TAG)

        /// validate credentials accessing the root folder
        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            username,
            password
        )
        accessRootFolder(credentials)
    }

    private fun accessRootFolder(credentials: OwnCloudCredentials?) {
        asyncTask = AuthenticatorAsyncTask(this)
        val params = arrayOf(serverInfo.mBaseUrl, credentials)
        asyncTask?.execute(*params)
    }

    /**
     * Starts the OAuth 'grant type' flow to get an access token, with
     * a GET AUTHORIZATION request to the BUILT-IN authorization server.
     */
    private fun startOauthorization() {
        // be gentle with the user
        authStatusIcon = R.drawable.progress_small
        authStatusText = resources.getString(R.string.oauth_login_connection)
        showAuthStatus()

        // GET AUTHORIZATION CODE URI to open in WebView
        val oAuth2Provider = OAuth2ProvidersRegistry.getInstance().provider
        oAuth2Provider.authorizationServerUri = serverInfo.mBaseUrl

        val builder = oAuth2Provider.operationBuilder
        builder.setGrantType(OAuth2GrantType.AUTHORIZATION_CODE)
        builder.setRequest(OAuth2RequestBuilder.OAuthRequest.GET_AUTHORIZATION_CODE)

        if (mCustomTabPackageName != null) {
            openUrlWithCustomTab(builder.buildUri())
        } else {
            openUrlInBrowser(builder.buildUri())
        }
    }

    private fun openUrlWithCustomTab(url: String) {
        val backgroundColor = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, backgroundColor, true)

        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(backgroundColor.data)
            .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right)
            .setShowTitle(true)
            .build()

        try {
            intent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (ae: ActivityNotFoundException) {
            onNoBrowserInstalled()
        }

    }

    private fun onNoBrowserInstalled() {
        AlertDialog.Builder(this)
            .setMessage(R.string.no_borwser_installed_alert)
            .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
            .create()
            .show()
    }

    /**
     * Callback method invoked when a RemoteOperation executed by this Activity finishes.
     *
     *
     * Dispatches the operation flow to the right method.
     */
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>, result: RemoteOperationResult<*>) {

        if (operation is GetServerInfoOperation) {
            if (operation.hashCode().toLong() == waitingForOpId) {
                onGetServerInfoFinish(result as RemoteOperationResult<GetServerInfoOperation.ServerInfo>)
            }   // else nothing ; only the last check operation is considered;
            // multiple can be started if the user amends a URL quickly

        } else if (operation is OAuth2GetAccessTokenOperation) {
            onGetOAuthAccessTokenFinish(result as RemoteOperationResult<Map<String, String>>)
        }
    }

    /**
     * Processes the result of the server check performed when the user finishes the enter of the
     * server URL.
     *
     * @param result Result of the check.
     */
    private fun onGetServerInfoFinish(result: RemoteOperationResult<GetServerInfoOperation.ServerInfo>) {
        /// update activity state
        serverIsChecked = true
        waitingForOpId = java.lang.Long.MAX_VALUE

        // update server status, but don't show it yet
        updateServerStatusIconAndText(result)

        if (result.isSuccess) {
            /// SUCCESS means:
            //      1. connection succeeded, and we know if it's SSL or not
            //      2. server is installed
            //      3. we got the server version
            //      4. we got the authentication method required by the server
            serverInfo = result.data

            serverIsValid = true

            // Update authTokenType depending on the server info
            if (serverInfo.mAuthMethods.contains(AuthenticationMethod.BEARER_TOKEN)) {
                authTokenType = OAUTH_TOKEN_TYPE // OAuth2
            } else if (serverInfo.mAuthMethods.contains(AuthenticationMethod.BASIC_HTTP_AUTH)) {
                authTokenType = BASIC_TOKEN_TYPE // Basic
            }

        } else {
            serverIsValid = false
        }

        if (serverIsValid) {
            updateAuthenticationPreFragmentVisibility()
        }

        // refresh UI
        showRefreshButton(!serverIsValid)
        showServerStatus()
        if (serverIsValid && BASIC_TOKEN_TYPE != authTokenType) {
            loginButton?.visibility = View.VISIBLE
        } else {
            if (serverIsValid && usernameInput?.text?.isNotEmpty()!! && passwordInput?.text?.isNotEmpty()!!) {
                loginButton?.visibility = View.VISIBLE
            } else {
                loginButton?.visibility = View.GONE
            }
        }

        /// very special case (TODO: move to a common place for all the remote operations)
        if (result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result)
        }
    }

    // TODO remove, if possible
    private fun normalizeUrl(url: String?, sslWhenUnprefixed: Boolean): String {
        var url = url
        if (url != null && url.length > 0) {
            url = url.trim { it <= ' ' }
            if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
                if (sslWhenUnprefixed) {
                    url = "https://$url"
                } else {
                    url = "http://$url"
                }
            }

            url = normalizeUrlSuffix(url)
        }
        return url ?: ""
    }

    private fun normalizeUrlSuffix(url: String): String {
        var url = url
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        url = trimUrlWebdav(url)
        return url
    }

    private fun stripIndexPhpOrAppsFiles(url: String, mHostUrlInput: EditText): String {
        var url = url
        if (url.endsWith("/index.php")) {
            url = url.substring(0, url.lastIndexOf("/index.php"))
            mHostUrlInput.setText(url)
        } else if (url.contains("/index.php/apps/")) {
            url = url.substring(0, url.lastIndexOf("/index.php/apps/"))
            mHostUrlInput.setText(url)
        }

        return url
    }

    private fun subdomainToLower(url: String, mHostUrlInput: EditText): String {
        var url = url
        if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
            if (url.indexOf("/", 8) != -1) {
                url = url.substring(0, url.indexOf("/", 8)).toLowerCase() + url.substring(url.indexOf("/", 8))
            } else {
                url = url.toLowerCase()
            }

            mHostUrlInput.setText(url)
        } else {
            if (url.contains("/")) {
                url = url.substring(0, url.indexOf("/")).toLowerCase() + url.substring(url.indexOf("/"))
            } else {
                url = url.toLowerCase()
            }

            mHostUrlInput.setText(url)
        }

        return url
    }

    // TODO remove, if possible
    private fun trimUrlWebdav(url: String): String {
        var url = url
        if (url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_4_0_AND_LATER)) {
            url = url.substring(0, url.length - AccountUtils.WEBDAV_PATH_4_0_AND_LATER.length)
        }
        return url
    }

    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private fun updateServerStatusIconAndText(result: RemoteOperationResult<*>) {
        serverStatusIcon = R.drawable.common_error    // the most common case in the switch below

        when (result.code) {
            RemoteOperationResult.ResultCode.OK_SSL -> {
                serverStatusIcon = R.drawable.ic_lock
                serverStatusText = resources.getString(R.string.auth_secure_connection)
            }

            RemoteOperationResult.ResultCode.OK_NO_SSL, RemoteOperationResult.ResultCode.OK -> if (hostUrlInput?.text.toString().trim { it <= ' ' }.toLowerCase().startsWith(
                    "http://"
                )
            ) {
                serverStatusText = resources.getString(R.string.auth_connection_established)
                serverStatusIcon = R.drawable.ic_ok
            } else {
                serverStatusText = resources.getString(R.string.auth_nossl_plain_ok_title)
                serverStatusIcon = R.drawable.ic_lock_open
            }

            RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION -> {
                serverStatusIcon = R.drawable.ic_lock_open
                serverStatusText = ErrorMessageAdapter.getResultMessage(result, null, resources)
            }
            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> {
                serverStatusIcon = R.drawable.no_network
                serverStatusText = ErrorMessageAdapter.getResultMessage(result, null, resources)
            }
            else -> serverStatusText = ErrorMessageAdapter.getResultMessage(result, null, resources)
        }
    }

    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private fun updateAuthStatusIconAndText(result: RemoteOperationResult<*>) {
        authStatusIcon = R.drawable.common_error // the most common case in the switch below

        when (result.code) {
            RemoteOperationResult.ResultCode.OK_SSL -> {
                authStatusIcon = R.drawable.ic_lock
                authStatusText = resources.getString(R.string.auth_secure_connection)
            }
            RemoteOperationResult.ResultCode.OK_NO_SSL, RemoteOperationResult.ResultCode.OK -> if (hostUrlInput?.text.toString().trim { it <= ' ' }.toLowerCase().startsWith(
                    "http://"
                )
            ) {
                authStatusText = resources.getString(R.string.auth_connection_established)
                authStatusIcon = R.drawable.ic_ok
            } else {
                authStatusText = resources.getString(R.string.auth_nossl_plain_ok_title)
                authStatusIcon = R.drawable.ic_lock_open
            }

            RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> {
                authStatusIcon = R.drawable.no_network
                authStatusText = ErrorMessageAdapter.getResultMessage(result, null, resources)
            }
            else -> authStatusText = ErrorMessageAdapter.getResultMessage(result, null, resources)
        }
    }

    /**
     * Processes the result of the request for an access token sent to an OAuth authorization server
     *
     * @param result Result of the operation.
     */
    private fun onGetOAuthAccessTokenFinish(result: RemoteOperationResult<Map<String, String>>) {
        waitingForOpId = java.lang.Long.MAX_VALUE
        dismissDialog()

        if (result.isSuccess) {
            /// be gentle with the user
            val dialog = LoadingDialog.newInstance(R.string.auth_trying_to_login, true)
            dialog.show(supportFragmentManager, WAIT_DIALOG_TAG)

            /// time to test the retrieved access token on the ownCloud server
            val tokens = result.data
            authToken = tokens[OAuth2Constants.KEY_ACCESS_TOKEN]

            refreshToken = tokens[OAuth2Constants.KEY_REFRESH_TOKEN]

            /// validate token accessing to root folder / getting session
            val credentials = OwnCloudCredentialsFactory.newBearerCredentials(
                tokens[OAuth2Constants.KEY_USER_ID],
                authToken
            )

            accessRootFolder(credentials)

        } else {
            updateAuthStatusIconAndText(result)
            showAuthStatus()
            Log_OC.d(TAG, "Access failed: " + result.logMessage)
        }
    }

    /**
     * Processes the result of the access check performed to try the user credentials.
     *
     *
     * Creates a new account through the AccountManager.
     *
     * @param result Result of the operation.
     */
    override fun onAuthenticatorTaskCallback(result: RemoteOperationResult<*>) {
        waitingForOpId = java.lang.Long.MAX_VALUE
        dismissDialog()
        asyncTask = null

        if (result.isSuccess) {
            Log_OC.d(TAG, "Successful access - time to save the account")

            var success = false
            val username = (result as RemoteOperationResult<UserInfo>).data.mId

            if (action == ACTION_CREATE) {

                if (BASIC_TOKEN_TYPE != authTokenType) {
                    usernameInput?.setText(username)
                }

                success = createAccount(result)

            } else {

                try {

                    if (BASIC_TOKEN_TYPE == authTokenType) {

                        updateAccountAuthentication()
                        success = true

                    } else {

                        success = updateAccount(username)
                    }

                } catch (e: AccountNotFoundException) {
                    Log_OC.e(TAG, "Account $account was removed!", e)
                    Toast.makeText(
                        this, R.string.auth_account_does_not_exist,
                        Toast.LENGTH_SHORT
                    ).show()
                    // do not use a Snackbar, finishing right now!
                    finish()
                }

            }

            if (success) {
                finish()
            }

        } else if (result.isServerFail || result.isException) {
            /// server errors or exceptions in authorization take to requiring a new check of
            /// the server
            serverIsChecked = true
            serverIsValid = false
            serverInfo = GetServerInfoOperation.ServerInfo()

            // update status icon and text
            updateServerStatusIconAndText(result)
            showServerStatus()
            authStatusIcon = 0
            authStatusText = ""
            showAuthStatus()

            // update input controls state
            showRefreshButton(true)
            loginButton?.visibility = View.GONE

            // very special case (TODO: move to a common place for all the remote operations)
            if (result.code == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                showUntrustedCertDialog(result)
            }

        } else {    // authorization fail due to client side - probably wrong credentials
            updateAuthStatusIconAndText(result)
            showAuthStatus()
            Log_OC.d(TAG, "Access failed: " + result.logMessage)
        }
    }

    /**
     * Creates a new account through the Account Authenticator that started this activity.
     *
     *
     * This makes the account permanent.
     *
     *
     * TODO Decide how to name the OAuth accounts
     */
    private fun createAccount(authResult: RemoteOperationResult<UserInfo>): Boolean {
        /// create and save new ownCloud account
        val isOAuth = AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.accountType) == authTokenType

        val lastPermanentLocation = authResult.lastPermanentLocation
        if (lastPermanentLocation != null) {
            serverInfo.mBaseUrl = AccountUtils.trimWebdavSuffix(lastPermanentLocation)
        }

        val uri = Uri.parse(serverInfo.mBaseUrl)
        val username = usernameInput?.text.toString().trim { it <= ' ' }
        val accountName = com.owncloud.android.lib.common.accounts.AccountUtils.buildAccountName(uri, username)
        val newAccount = Account(accountName, MainApp.accountType)
        if (AccountUtils.exists(newAccount.name, applicationContext)) {
            // fail - not a new account, but an existing one; disallow
            val result = RemoteOperationResult<Any>(ResultCode.ACCOUNT_NOT_NEW)
            updateAuthStatusIconAndText(result)
            showAuthStatus()
            Log_OC.d(TAG, result.getLogMessage())
            return false

        } else {
            account = newAccount

            if (isOAuth) {
                // with external authorizations, the password is never input in the app
                accountMgr?.addAccountExplicitly(account, "", null)
            } else {
                accountMgr?.addAccountExplicitly(
                    account, passwordInput?.text.toString(), null
                )
            }

            // include account version with the new account
            accountMgr?.setUserData(
                account,
                Constants.KEY_OC_ACCOUNT_VERSION,
                Integer.toString(AccountUtils.ACCOUNT_VERSION)
            )

            /// add the new account as default in preferences, if there is none already
            val defaultAccount = AccountUtils.getCurrentOwnCloudAccount(this)
            if (defaultAccount == null) {
                val editor = PreferenceManager
                    .getDefaultSharedPreferences(this).edit()
                editor.putString("select_oc_account", accountName)
                editor.apply()
            }

            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same
            //  effect as addAccountExplicitly, but it's not well done
            val intent = Intent()
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, MainApp.accountType)
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account?.name)
            intent.putExtra(AccountManager.KEY_USERDATA, username)

            if (isOAuth) {
                accountMgr?.setAuthToken(account, authTokenType, authToken)
            }
            /// add user data to the new account; TODO probably can be done in the last parameter
            //      addAccountExplicitly, or in KEY_USERDATA
            accountMgr?.setUserData(
                account, Constants.KEY_OC_VERSION, serverInfo.mVersion.version
            )
            accountMgr?.setUserData(
                account, Constants.KEY_OC_BASE_URL, serverInfo.mBaseUrl
            )
            if (authResult.data != null) {
                try {
                    val userInfo = authResult.data
                    accountMgr?.setUserData(
                        account, Constants.KEY_DISPLAY_NAME, userInfo.mDisplayName
                    )
                } catch (c: ClassCastException) {
                    Log_OC.w(TAG, "Couldn't get display name for $username")
                }

            } else {
                Log_OC.w(TAG, "Couldn't get display name for $username")
            }

            if (isOAuth) {
                accountMgr?.setUserData(account, Constants.KEY_SUPPORTS_OAUTH2, "TRUE")
                accountMgr?.setUserData(account, Constants.KEY_OAUTH2_REFRESH_TOKEN, refreshToken)
            }

            setAccountAuthenticatorResult(intent.extras)
            setResult(RESULT_OK, intent)

            // Notify login to Document Provider
            val authority = resources.getString(R.string.document_provider_authority)
            val rootsUri = DocumentsContract.buildRootsUri(authority)
            contentResolver.notifyChange(rootsUri, null)

            return true
        }
    }

    /**
     * Update an existing account
     *
     *
     * Check if the username of the account to update is the same as the username in the current
     * account, calling [.updateAccountAuthentication] if so and showing an error otherwise
     *
     * @param username in the server
     * @return true if account is properly update, false otherwise
     */
    private fun updateAccount(username: String): Boolean {

        var success = false

        val result: RemoteOperationResult<*>

        if (usernameInput?.text.toString().trim { it <= ' ' } != username) {
            // fail - not a new account, but an existing one; disallow
            result = RemoteOperationResult<Any>(ResultCode.ACCOUNT_NOT_THE_SAME)
            authToken = ""
            updateAuthStatusIconAndText(result)
            showAuthStatus()
            Log_OC.d(TAG, result.logMessage)

        } else {
            try {
                updateAccountAuthentication()
                success = true

            } catch (e: AccountNotFoundException) {
                Log_OC.e(TAG, "Account $account was removed!", e)
                Toast.makeText(
                    this, R.string.auth_account_does_not_exist,
                    Toast.LENGTH_SHORT
                ).show()
                // don't use a Snackbar, finishing right now
                finish()
            }

        }
        return success
    }

    /**
     * Updates the authentication token.
     *
     *
     * Sets the proper response so that the AccountAuthenticator that started this activity
     * saves a new authorization token for account.
     *
     *
     * Kills the session kept by OwnCloudClientManager so that a new one will be created with
     * the new credentials when needed.
     */
    @Throws(AccountNotFoundException::class)
    private fun updateAccountAuthentication() {
        val response = Bundle()
        response.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
        response.putString(AccountManager.KEY_ACCOUNT_TYPE, account?.type)

        val OAuthSupported = accountMgr?.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2)

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.accountType) == authTokenType) { // OAuth
            response.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            // the next line is necessary, notifications are calling directly to the
            // AuthenticatorActivity to update, without AccountManager intervention
            accountMgr?.setAuthToken(account, authTokenType, authToken)

            if (OAuthSupported == null || OAuthSupported == "FALSE") {
                accountMgr?.setUserData(account, Constants.KEY_SUPPORTS_OAUTH2, "TRUE")
            }
        } else { // BASIC
            if (OAuthSupported != null && OAuthSupported == "TRUE") {
                accountMgr?.setUserData(account, Constants.KEY_SUPPORTS_OAUTH2, "FALSE")
            }

            response.putString(AccountManager.KEY_AUTHTOKEN, passwordInput?.text.toString())
            accountMgr?.setPassword(account, passwordInput?.text.toString())
        }

        // remove managed clients for this account to enforce creation with fresh credentials
        val ocAccount = OwnCloudAccount(account, this)
        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount)

        setAccountAuthenticatorResult(response)
        val intent = Intent()
        intent.putExtras(response)
        setResult(RESULT_OK, intent)
    }

    /**
     * Starts and activity to open the 'new account' page in the ownCloud web site
     *
     * @param view 'Account register' button
     */
    fun onRegisterClick(view: View) {
        val register = Intent(
            ACTION_VIEW, Uri.parse(getString(R.string.welcome_link_url))
        )
        setResult(RESULT_CANCELED)
        startActivity(register)
    }

    /**
     * Updates the content and visibility state of the icon and text associated
     * to the last check on the ownCloud server.
     */
    private fun showServerStatus() {
        if (serverStatusIcon == 0 && (serverStatusText == null || serverStatusText?.length == 0)) {
            serverStatusView?.visibility = View.INVISIBLE

        } else {
            serverStatusView?.text = serverStatusText
            serverStatusView?.setCompoundDrawablesWithIntrinsicBounds(serverStatusIcon, 0, 0, 0)
            serverStatusView?.visibility = View.VISIBLE
        }

    }

    /**
     * Updates the content and visibility state of the icon and text associated
     * to the interactions with the OAuth authorization server.
     */
    private fun showAuthStatus() {
        if (authStatusIcon == 0 && (authStatusText == null || authStatusText?.length == 0)) {
            authStatusView?.visibility = View.INVISIBLE

        } else {
            authStatusView?.text = authStatusText
            authStatusView?.setCompoundDrawablesWithIntrinsicBounds(authStatusIcon, 0, 0, 0)
            authStatusView?.visibility = View.VISIBLE
        }
    }

    private fun showRefreshButton(show: Boolean) {
        if (show) {
            checkServerButton?.visibility = View.GONE
            refreshButton?.visibility = View.VISIBLE
        } else {
            refreshButton?.visibility = View.GONE
            checkServerButton?.visibility = View.VISIBLE
        }
    }

    /**
     * Called when the eye icon in the password field is clicked.
     *
     *
     * Toggles the visibility of the password in the field.
     */
    fun onViewPasswordClick() {
        val selectionStart = passwordInput!!.selectionStart
        val selectionEnd = passwordInput!!.selectionEnd
        if (isPasswordVisible) {
            hidePassword()
        } else {
            showPassword()
        }
        passwordInput!!.setSelection(selectionStart, selectionEnd)
    }

    /**
     * Called when the 'action' button in an IME is pressed ('enter' in software keyboard).
     *
     *
     * Used to trigger the authentication check when the user presses 'enter' after writing the
     * password, or to throw the server test when the only field on screen is the URL input field.
     */
    override fun onEditorAction(inputField: TextView?, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE && inputField != null &&
            inputField == passwordInput
        ) {
            if (loginButton!!.isEnabled) {
                loginButton?.performClick()
            }

        } else if (actionId == EditorInfo.IME_ACTION_NEXT && inputField != null &&
            inputField == hostUrlInput
        ) {
            if (AccountTypeUtils.getAuthTokenTypePass(MainApp.accountType) != authTokenType) {
                checkOcServer()
            }
        }
        return false   // always return false to grant that the software keyboard is hidden anyway
    }

    private abstract class RightDrawableOnTouchListener : OnTouchListener {

        /**
         * {@inheritDoc}
         */
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            var rightDrawable: Drawable? = null
            if (view is TextView) {
                val drawables = view.compoundDrawables
                if (drawables.size > 2) {
                    rightDrawable = drawables[2]
                }
            }
            if (rightDrawable != null) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val bounds = rightDrawable.bounds
                val fuzz = 75
                if (x >= view.right - bounds.width() - fuzz &&
                    x <= view.right - view.paddingRight + fuzz &&
                    y >= view.paddingTop - fuzz &&
                    y <= view.height - view.paddingBottom + fuzz
                ) {

                    return onDrawableTouch(event)
                }
            }
            return false
        }

        abstract fun onDrawableTouch(event: MotionEvent): Boolean
    }

    /**
     * Show untrusted cert dialog
     */
    private fun showUntrustedCertDialog(result: RemoteOperationResult<*>) {
        // Show a dialog with the certificate info
        val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(result.exception as CertificateCombinedException)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG)

    }

    private fun doOnResumeAndBound() {
        //Log_OC.e(TAG, "registering to listen for operation callbacks" );
        operationsServiceBinder?.addOperationListener(this@AuthenticatorActivity, handler)
        if (waitingForOpId <= Integer.MAX_VALUE) {
            operationsServiceBinder?.dispatchResultIfFinished(waitingForOpId.toInt(), this)
        }

        if (pendingAutoCheck) {
            checkOcServer()
            pendingAutoCheck = false
        }
    }

    private fun dismissDialog() {
        val frag = supportFragmentManager.findFragmentByTag(AuthenticatorActivity.WAIT_DIALOG_TAG)
        if (frag is DialogFragment) {
            val dialog = frag as DialogFragment?
            dialog?.dismiss()
        }
    }

    /**
     * Implements callback methods for service binding.
     */
    private inner class OperationsServiceConnection : ServiceConnection {

        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component == ComponentName(this@AuthenticatorActivity, OperationsService::class.java)) {
                operationsServiceBinder = service as OperationsServiceBinder
                doOnResumeAndBound()
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(this@AuthenticatorActivity, OperationsService::class.java)) {
                Log_OC.e(TAG, "Operations service crashed")
                operationsServiceBinder = null
            }
        }
    }

    companion object {

        private val TAG = AuthenticatorActivity::class.java.simpleName

        val EXTRA_ACTION = "ACTION"
        val EXTRA_ACCOUNT = "ACCOUNT"

        private val KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE"

        private val KEY_HOST_URL_TEXT = "HOST_URL_TEXT"
        private val KEY_OC_VERSION = "OC_VERSION"
        private val KEY_SERVER_VALID = "SERVER_VALID"
        private val KEY_SERVER_CHECKED = "SERVER_CHECKED"
        private val KEY_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT"
        private val KEY_SERVER_STATUS_ICON = "SERVER_STATUS_ICON"
        private val KEY_IS_SSL_CONN = "IS_SSL_CONN"
        private val KEY_PASSWORD_EXPOSED = "PASSWORD_VISIBLE"
        private val KEY_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT"
        private val KEY_AUTH_STATUS_ICON = "AUTH_STATUS_ICON"
        private val KEY_SERVER_AUTH_METHOD = "SERVER_AUTH_METHOD"
        private val KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID"
        private val KEY_AUTH_TOKEN = "AUTH_TOKEN"

        val ACTION_CREATE: Byte = 0
        val ACTION_UPDATE_TOKEN: Byte = 1               // requested by the user
        val ACTION_UPDATE_EXPIRED_TOKEN: Byte = 2       // detected by the app

        private val UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG"
        private val WAIT_DIALOG_TAG = "WAIT_DIALOG"
        private val KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT"

        private val KEY_USERNAME = "USERNAME"
        private val KEY_PASSWORD = "PASSWORD"
        private val KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS"

        private val ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService"
    }
}
