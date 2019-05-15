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
 * <p>
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

package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.SAMLWebViewClient.SsoWebViewClientListener;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2Constants;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2GetAccessTokenOperation;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2GrantType;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2Provider;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ProvidersRegistry;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2QueryParser;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2RequestBuilder;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation.UserInfo;
import com.owncloud.android.operations.AuthenticationMethod;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.dialog.CredentialsDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.LoginWebViewDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.Intent.ACTION_VIEW;

/**
 * This Activity is used to add an ownCloud account to the App
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements OnRemoteOperationListener, OnFocusChangeListener, OnEditorActionListener,
        SsoWebViewClientListener, OnSslUntrustedCertListener,
        AuthenticatorAsyncTask.OnAuthenticatorTaskListener {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private static final String KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";

    private static final String KEY_HOST_URL_TEXT = "HOST_URL_TEXT";
    private static final String KEY_OC_VERSION = "OC_VERSION";
    private static final String KEY_SERVER_VALID = "SERVER_VALID";
    private static final String KEY_SERVER_CHECKED = "SERVER_CHECKED";
    private static final String KEY_SERVER_STATUS_TEXT = "SERVER_STATUS_TEXT";
    private static final String KEY_SERVER_STATUS_ICON = "SERVER_STATUS_ICON";
    private static final String KEY_IS_SSL_CONN = "IS_SSL_CONN";
    private static final String KEY_PASSWORD_EXPOSED = "PASSWORD_VISIBLE";
    private static final String KEY_AUTH_STATUS_TEXT = "AUTH_STATUS_TEXT";
    private static final String KEY_AUTH_STATUS_ICON = "AUTH_STATUS_ICON";
    private static final String KEY_SERVER_AUTH_METHOD = "SERVER_AUTH_METHOD";
    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";

    private static final String AUTH_ON = "on";

    public static final byte ACTION_CREATE = 0;
    public static final byte ACTION_UPDATE_TOKEN = 1;               // requested by the user
    public static final byte ACTION_UPDATE_EXPIRED_TOKEN = 2;       // detected by the app

    private static final String UNTRUSTED_CERT_DIALOG_TAG = "UNTRUSTED_CERT_DIALOG";
    private static final String SAML_DIALOG_TAG = "SAML_DIALOG";
    private static final String OAUTH_DIALOG_TAG = "OAUTH_DIALOG";
    private static final String WAIT_DIALOG_TAG = "WAIT_DIALOG";
    private static final String CREDENTIALS_DIALOG_TAG = "CREDENTIALS_DIALOG";
    private static final String KEY_AUTH_IS_FIRST_ATTEMPT_TAG = "KEY_AUTH_IS_FIRST_ATTEMPT";

    private static final String KEY_USERNAME = "USERNAME";
    private static final String KEY_PASSWORD = "PASSWORD";
    private static final String KEY_ASYNC_TASK_IN_PROGRESS = "AUTH_IN_PROGRESS";

    private static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";

    // ChromeCustomTab
    String mCustomTabPackageName;

    /// parameters from EXTRAs in starter Intent
    private byte mAction;
    private Account mAccount;
    private String mAuthTokenType;

    /// activity-level references / state
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection = null;
    private OperationsServiceBinder mOperationsServiceBinder = null;
    private AccountManager mAccountMgr;

    /// Server PRE-Fragment elements
    private EditText mHostUrlInput;
    private View mRefreshButton;
    private TextView mServerStatusView;

    private TextWatcher mHostUrlInputWatcher;

    private String mServerStatusText = "";

    private int mServerStatusIcon = 0;

    private boolean mServerIsChecked = false;
    private boolean mServerIsValid = false;
    private boolean mPendingAutoCheck = false;

    private GetServerInfoOperation.ServerInfo mServerInfo =
            new GetServerInfoOperation.ServerInfo();

    /// Authentication PRE-Fragment elements
    private EditText mUsernameInput;
    private EditText mPasswordInput;
    private View mCheckServerButton;
    private View mLoginButton;
    private TextView mAuthStatusView;

    private TextWatcher mUsernamePasswordInputWatcher;

    private String mAuthStatusText = "";

    private int mAuthStatusIcon = 0;

    private String mAuthToken = "";
    private String mRefreshToken = "";
    private AuthenticatorAsyncTask mAsyncTask;

    private boolean mIsFirstAuthAttempt;

    /// Identifier of operation in progress which result shouldn't be lost 
    private long mWaitingForOpId = Long.MAX_VALUE;

    private final String BASIC_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypePass(
            MainApp.getAccountType());
    private final String OAUTH_TOKEN_TYPE = AccountTypeUtils.getAuthTokenTypeAccessToken(
            MainApp.getAccountType());
    private final String SAML_TOKEN_TYPE =
            AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType());

    private CustomTabsClient mCustomTabsClient;
    private CustomTabsServiceConnection mCustomTabServiceConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            mCustomTabsClient = client;
            mCustomTabsClient.warmup(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCustomTabsClient = null;
        }
    };

    /**
     * {@inheritDoc}
     * <p>
     * IMPORTANT ENTRY POINT 1: activity is shown to the user
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /// protection against screen recording
        if (!MainApp.isDeveloper()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } // else, let it go, or taking screenshots & testing will not be possible

        // Workaround, for fixing a problem with Android Library Suppor v7 19
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();

            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mIsFirstAuthAttempt = true;

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        if (!bindService(new Intent(this, OperationsService.class),
                mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            Toast.makeText(this,
                    R.string.error_cant_bind_to_operations_service,
                    Toast.LENGTH_LONG)
                    .show();
            //  do not use a Snackbar, finishing right now!
            finish();
        }

        /// init activity state
        mAccountMgr = AccountManager.get(this);

        /// get input values
        mAction = getIntent().getByteExtra(EXTRA_ACTION, ACTION_CREATE);
        mAccount = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
        if (savedInstanceState == null) {
            initAuthTokenType();
        } else {
            mAuthTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE);
            mWaitingForOpId = savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID);
            mIsFirstAuthAttempt = savedInstanceState.getBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG);
        }

        /// load user interface
        setContentView(R.layout.account_setup);

        // Allow or disallow touches with other visible windows
        FrameLayout loginLayout = findViewById(R.id.login_layout);
        loginLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        // Set login background color or image
        if (!getResources().getBoolean(R.bool.use_login_background_image)) {
            loginLayout.setBackgroundColor(
                    getResources().getColor(R.color.login_background_color)
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findViewById(R.id.login_background_image).setVisibility(View.VISIBLE);
        }

        /// initialize general UI elements
        initOverallUi();

        mCheckServerButton = findViewById(R.id.embeddedCheckServerButton);

        mCheckServerButton.setOnClickListener(view -> {
            checkOcServer();
        });

        findViewById(R.id.centeredRefreshButton).setOnClickListener(view -> checkOcServer());
        findViewById(R.id.embeddedRefreshButton).setOnClickListener(view -> checkOcServer());

        mLoginButton = findViewById(R.id.loginButton);
        mLoginButton.setOnClickListener(view -> onLoginClick());

        /// initialize block to be moved to single Fragment to check server and get info about it 
        initServerPreFragment(savedInstanceState);

        /// initialize block to be moved to single Fragment to retrieve and validate credentials 
        initAuthorizationPreFragment(savedInstanceState);

        mCustomTabPackageName = getCustomTabPackageName();

        if (mCustomTabPackageName != null) {
            CustomTabsClient.bindCustomTabsService(this, mCustomTabPackageName, mCustomTabServiceConnection);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_VIEW)) {
            getOAuth2AccessTokenFromCapturedRedirection(intent.getData());
        }
    }

    private String getCustomTabPackageName() {

        PackageManager pm = getPackageManager();
        // Get default VIEW intent handler.
        Intent activityIntent = new Intent(ACTION_VIEW, Uri.parse("https://owncloud.org"));
        ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);

        // Get all apps that can handle VIEW intents.
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }

        // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
        // and service calls.
        if (defaultViewHandlerInfo != null
                && packagesSupportingCustomTabs.contains(defaultViewHandlerInfo.activityInfo.packageName)) {
            // try getting the ChromeCustomTab of the default browser
            return defaultViewHandlerInfo.activityInfo.packageName;
        } else if (packagesSupportingCustomTabs.size() >= 1) {
            // try getting the ChromeCustomTab of the first browser that support its
            return packagesSupportingCustomTabs.get(0);
        } else {
            // return null if we don't have a browser installed that can handle ChromeCustomTabs
            return null;
        }
    }

    private void initAuthTokenType() {
        mAuthTokenType =
                getIntent().getExtras().getString(AccountAuthenticator.KEY_AUTH_TOKEN_TYPE);
        if (mAuthTokenType == null) {
            if (mAccount != null) {
                boolean oAuthRequired =
                        (mAccountMgr.getUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2) != null);
                boolean samlWebSsoRequired = (
                        mAccountMgr.getUserData(
                                mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO
                        ) != null
                );
                mAuthTokenType = chooseAuthTokenType(oAuthRequired, samlWebSsoRequired);

            } else {
                boolean samlWebSsoSupported =
                        AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));

                if (samlWebSsoSupported) {
                    mAuthTokenType = SAML_TOKEN_TYPE;
                } else {
                    // If SAML is not supported, OAuth will be the default authentication method
                    mAuthTokenType = "";
                }
            }
        }
    }

    private String chooseAuthTokenType(boolean oauth, boolean saml) {
        if (saml) {
            return SAML_TOKEN_TYPE;
        } else if (oauth) {
            return OAUTH_TOKEN_TYPE;
        } else {
            return BASIC_TOKEN_TYPE;
        }
    }

    /**
     * Configures elements in the user interface under direct control of the Activity.
     */
    private void initOverallUi() {

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        boolean isWelcomeLinkVisible = getResources().getBoolean(R.bool.show_welcome_link);

        String instructionsMessageText = null;
        if (mAction == ACTION_UPDATE_EXPIRED_TOKEN) {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType())
                    .equals(mAuthTokenType)) {
                instructionsMessageText = getString(R.string.auth_expired_oauth_token_toast);

            } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType())
                    .equals(mAuthTokenType)) {
                instructionsMessageText = getString(R.string.auth_expired_saml_sso_token_toast);

            } else {
                instructionsMessageText = getString(R.string.auth_expired_basic_auth_toast);
            }
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        Button welcomeLink = findViewById(R.id.welcome_link);
        welcomeLink.setVisibility(isWelcomeLinkVisible ? View.VISIBLE : View.GONE);
        welcomeLink.setText(
                String.format(getString(R.string.auth_register), getString(R.string.app_name)));

        TextView instructionsView = findViewById(R.id.instructions_message);
        if (instructionsMessageText != null) {
            instructionsView.setVisibility(View.VISIBLE);
            instructionsView.setText(instructionsMessageText);
        } else {
            instructionsView.setVisibility(View.GONE);
        }
    }

    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initServerPreFragment(Bundle savedInstanceState) {
        boolean checkHostUrl = true;

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        boolean isUrlInputAllowed = getResources().getBoolean(R.bool.show_server_url_input);
        if (savedInstanceState == null) {
            if (mAccount != null) {
                mServerInfo.mBaseUrl = mAccountMgr.getUserData(mAccount, Constants.KEY_OC_BASE_URL);
                // TODO do next in a setter for mBaseUrl
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith("https://");
                mServerInfo.mVersion = AccountUtils.getServerVersion(mAccount);
            } else {
                mServerInfo.mBaseUrl = getString(R.string.server_url).trim();
                mServerInfo.mIsSslConn = mServerInfo.mBaseUrl.startsWith("https://");
            }
        } else {
            mServerStatusText = savedInstanceState.getString(KEY_SERVER_STATUS_TEXT);
            mServerStatusIcon = savedInstanceState.getInt(KEY_SERVER_STATUS_ICON);

            mServerIsValid = savedInstanceState.getBoolean(KEY_SERVER_VALID);
            mServerIsChecked = savedInstanceState.getBoolean(KEY_SERVER_CHECKED);

            // TODO parcelable
            mServerInfo.mIsSslConn = savedInstanceState.getBoolean(KEY_IS_SSL_CONN);
            mServerInfo.mBaseUrl = savedInstanceState.getString(KEY_HOST_URL_TEXT);
            String ocVersion = savedInstanceState.getString(KEY_OC_VERSION);
            if (ocVersion != null) {
                mServerInfo.mVersion = new OwnCloudVersion(ocVersion);
            }

            ArrayList<String> authenticationMethodNames = savedInstanceState.
                    getStringArrayList(KEY_SERVER_AUTH_METHOD);

            for (String authenticationMethodName : authenticationMethodNames) {
                mServerInfo.mAuthMethods.add(AuthenticationMethod.valueOf(authenticationMethodName));
            }
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        mHostUrlInput = findViewById(R.id.hostUrlInput);
        // Convert IDN to Unicode
        mHostUrlInput.setText(DisplayUtils.convertIdn(mServerInfo.mBaseUrl, false));
        if (mAction != ACTION_CREATE) {
            /// lock things that should not change
            mHostUrlInput.setEnabled(false);
            mHostUrlInput.setFocusable(false);
        }
        if (isUrlInputAllowed) {
            if (mServerInfo.mBaseUrl.isEmpty()) {
                checkHostUrl = false;
            }
            mRefreshButton = findViewById(R.id.embeddedRefreshButton);
        } else {
            findViewById(R.id.hostUrlFrame).setVisibility(View.GONE);
            mRefreshButton = findViewById(R.id.centeredRefreshButton);
        }
        showRefreshButton(mServerIsChecked && !mServerIsValid &&
                mWaitingForOpId > Integer.MAX_VALUE);
        mServerStatusView = findViewById(R.id.server_status_text);
        showServerStatus();

        /// step 3 - bind some listeners and options
        mHostUrlInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mHostUrlInput.setOnEditorActionListener(this);

        /// step 4 - create listeners that will be bound at onResume
        mHostUrlInputWatcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (mLoginButton.isEnabled() &&
                        !mServerInfo.mBaseUrl.equals(
                                normalizeUrl(s.toString(), mServerInfo.mIsSslConn))) {
                    mLoginButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mAuthStatusIcon != 0) {
                    Log_OC.d(TAG, "onTextChanged: hiding authentication status");
                    mAuthStatusIcon = 0;
                    mAuthStatusText = "";
                    showAuthStatus();
                }
            }
        };

        mUsernamePasswordInputWatcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
                    if (mUsernameInput.getText().toString().trim().length() > 0 && mPasswordInput.getText().toString().trim().length() > 0) {
                        mLoginButton.setVisibility(View.VISIBLE);
                    } else {
                        mLoginButton.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        findViewById(R.id.scroll).setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mHostUrlInput.hasFocus()) {
                    checkOcServer();
                }
            }
            return false;
        });

        /// step 4 - mark automatic check to be started when OperationsService is ready
        mPendingAutoCheck = (savedInstanceState == null &&
                (mAction != ACTION_CREATE || checkHostUrl));
    }

    /**
     * @param savedInstanceState Saved activity state, as in {{@link #onCreate(Bundle)}
     */
    private void initAuthorizationPreFragment(Bundle savedInstanceState) {

        /// step 0 - get UI elements in layout
        mUsernameInput = findViewById(R.id.account_username);
        mPasswordInput = findViewById(R.id.account_password);
        mAuthStatusView = findViewById(R.id.auth_status_text);

        /// step 1 - load and process relevant inputs (resources, intent, savedInstanceState)
        String presetUserName = null;
        boolean isPasswordExposed = false;
        if (savedInstanceState == null) {
            if (mAccount != null) {
                presetUserName =
                        com.owncloud.android.lib.common.accounts.AccountUtils.
                                getUsernameForAccount(mAccount);
            }

        } else {
            isPasswordExposed = savedInstanceState.getBoolean(KEY_PASSWORD_EXPOSED, false);
            mAuthStatusText = savedInstanceState.getString(KEY_AUTH_STATUS_TEXT);
            mAuthStatusIcon = savedInstanceState.getInt(KEY_AUTH_STATUS_ICON);
            mAuthToken = savedInstanceState.getString(KEY_AUTH_TOKEN);
        }

        /// step 2 - set properties of UI elements (text, visibility, enabled...)
        if (presetUserName != null) {
            mUsernameInput.setText(presetUserName);
        }
        if (mAction != ACTION_CREATE) {
            mUsernameInput.setEnabled(false);
            mUsernameInput.setFocusable(false);
        }
        mPasswordInput.setText(""); // clean password to avoid social hacking
        if (isPasswordExposed) {
            showPassword();
        }
        updateAuthenticationPreFragmentVisibility();
        showAuthStatus();

        if (mServerIsValid && !BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
            mLoginButton.setVisibility(View.VISIBLE);
        } else {
            mLoginButton.setVisibility(View.GONE);
        }

        /// step 3 - bind listeners
        // bindings for password input field
        mPasswordInput.setOnFocusChangeListener(this);
        mPasswordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPasswordInput.setOnEditorActionListener(this);
        mPasswordInput.setOnTouchListener(new RightDrawableOnTouchListener() {
            @Override
            public boolean onDrawableTouch(final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    AuthenticatorActivity.this.onViewPasswordClick();
                }
                return true;
            }
        });

    }

    /**
     * Changes the visibility of input elements depending on
     * the current authorization method.
     */
    private void updateAuthenticationPreFragmentVisibility() {
        if (AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType()).
                equals(mAuthTokenType)) {
            // basic HTTP authorization
            mUsernameInput.setVisibility(View.VISIBLE);
            mPasswordInput.setVisibility(View.VISIBLE);

        } else {
            mUsernameInput.setVisibility(View.GONE);
            mPasswordInput.setVisibility(View.GONE);
        }
    }

    /**
     * Saves relevant state before {@link #onPause()}
     * <p>
     * See {@link super#onSaveInstanceState(Bundle)}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Log_OC.e(TAG, "onSaveInstanceState init" );
        super.onSaveInstanceState(outState);

        /// global state
        outState.putString(KEY_AUTH_TOKEN_TYPE, mAuthTokenType);
        outState.putLong(KEY_WAITING_FOR_OP_ID, mWaitingForOpId);

        /// Server PRE-fragment state
        outState.putString(KEY_SERVER_STATUS_TEXT, mServerStatusText);
        outState.putInt(KEY_SERVER_STATUS_ICON, mServerStatusIcon);
        outState.putBoolean(KEY_SERVER_CHECKED, mServerIsChecked);
        outState.putBoolean(KEY_SERVER_VALID, mServerIsValid);
        outState.putBoolean(KEY_IS_SSL_CONN, mServerInfo.mIsSslConn);
        outState.putString(KEY_HOST_URL_TEXT, mServerInfo.mBaseUrl);
        if (mServerInfo.mVersion != null) {
            outState.putString(KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
        }

        ArrayList<String> authenticationMethodNames = new ArrayList<>();

        for (AuthenticationMethod authenticationMethod : mServerInfo.mAuthMethods) {

            authenticationMethodNames.add(authenticationMethod.name());
        }

        outState.putStringArrayList(KEY_SERVER_AUTH_METHOD, authenticationMethodNames);

        /// Authentication PRE-fragment state
        outState.putBoolean(KEY_PASSWORD_EXPOSED, isPasswordVisible());
        outState.putInt(KEY_AUTH_STATUS_ICON, mAuthStatusIcon);
        outState.putString(KEY_AUTH_STATUS_TEXT, mAuthStatusText);
        outState.putString(KEY_AUTH_TOKEN, mAuthToken);

        /// authentication
        outState.putBoolean(KEY_AUTH_IS_FIRST_ATTEMPT_TAG, mIsFirstAuthAttempt);

        /// AsyncTask (User and password)
        outState.putString(KEY_USERNAME, mUsernameInput.getText().toString().trim());
        outState.putString(KEY_PASSWORD, mPasswordInput.getText().toString());

        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, true);
        } else {
            outState.putBoolean(KEY_ASYNC_TASK_IN_PROGRESS, false);
        }
        mAsyncTask = null;

        //Log_OC.e(TAG, "onSaveInstanceState end" );
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // AsyncTask
        boolean inProgress = savedInstanceState.getBoolean(KEY_ASYNC_TASK_IN_PROGRESS);
        if (inProgress) {
            String username = savedInstanceState.getString(KEY_USERNAME);
            String password = savedInstanceState.getString(KEY_PASSWORD);
            OwnCloudCredentials credentials = null;
            if (BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
                String version = savedInstanceState.getString(KEY_OC_VERSION);
                OwnCloudVersion ocVersion = (version != null) ? new OwnCloudVersion(version) : null;
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                        username,
                        password,
                        (ocVersion != null && ocVersion.isPreemptiveAuthenticationPreferred())
                );
            } else if (OAUTH_TOKEN_TYPE.equals(mAuthTokenType)) {
                credentials = OwnCloudCredentialsFactory.newBearerCredentials(username, mAuthToken);
            }
            accessRootFolder(credentials);
        }
    }

    /**
     * The redirection triggered by the OAuth authentication server as response to the
     * GET AUTHORIZATION, and deferred in {@link #onNewIntent(Intent)}, is processed here.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // bound here to avoid spurious changes triggered by Android on device rotations
        mHostUrlInput.setOnFocusChangeListener(this);
        mHostUrlInput.addTextChangedListener(mHostUrlInputWatcher);
        mUsernameInput.addTextChangedListener(mUsernamePasswordInputWatcher);
        mPasswordInput.addTextChangedListener(mUsernamePasswordInputWatcher);
        mUsernamePasswordInputWatcher.afterTextChanged(null);

        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }
    }

    @Override
    protected void onPause() {
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }

        mUsernameInput.removeTextChangedListener(mUsernamePasswordInputWatcher);
        mPasswordInput.removeTextChangedListener(mUsernamePasswordInputWatcher);
        mHostUrlInput.removeTextChangedListener(mHostUrlInputWatcher);
        mHostUrlInput.setOnFocusChangeListener(null);

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        mHostUrlInputWatcher = null;
        mUsernamePasswordInputWatcher = null;

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        if (mCustomTabServiceConnection != null
                && mCustomTabPackageName != null) {
            unbindService(mCustomTabServiceConnection);

        }

        super.onDestroy();
    }

    /**
     * Parses the redirection with the response to the GET AUTHORIZATION request to the
     * OAuth server and requests for the access token (GET ACCESS TOKEN)
     *
     * @param capturedUriFromOAuth2Redirection Redirection after authorization code request ends
     */
    private void getOAuth2AccessTokenFromCapturedRedirection(Uri capturedUriFromOAuth2Redirection) {

        // Parse data from OAuth redirection
        String queryParameters = capturedUriFromOAuth2Redirection.getQuery();
        Map<String, String> parsedQuery = new OAuth2QueryParser().parse(queryParameters);

        if (parsedQuery.keySet().contains(OAuth2Constants.KEY_CODE)) {

            /// Showing the dialog with instructions for the user
            LoadingDialog dialog = LoadingDialog.newInstance(R.string.auth_getting_authorization, true);
            dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

            /// CREATE ACCESS TOKEN to the oAuth server
            Intent getAccessTokenIntent = new Intent();
            getAccessTokenIntent.setAction(OperationsService.ACTION_OAUTH2_GET_ACCESS_TOKEN);

            getAccessTokenIntent.putExtra(
                    OperationsService.EXTRA_SERVER_URL,
                    mServerInfo.mBaseUrl
            );

            getAccessTokenIntent.putExtra(
                    OperationsService.EXTRA_OAUTH2_AUTHORIZATION_CODE,
                    parsedQuery.get(OAuth2Constants.KEY_CODE)
            );

            if (mOperationsServiceBinder != null) {
                Log_OC.i(TAG, "Getting OAuth access token...");
                mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getAccessTokenIntent);
            }

        } else {
            // did not obtain authorization code

            if (parsedQuery.keySet().contains(OAuth2Constants.KEY_ERROR) &&
                    (OAuth2Constants.VALUE_ERROR_ACCESS_DENIED.equals(parsedQuery.get(OAuth2Constants.KEY_ERROR)))) {
                onGetOAuthAccessTokenFinish(
                        new RemoteOperationResult(ResultCode.OAUTH2_ERROR_ACCESS_DENIED)
                );

            } else {
                onGetOAuthAccessTokenFinish(
                        new RemoteOperationResult(ResultCode.OAUTH2_ERROR)
                );
            }

        }
    }

    /**
     * Handles the change of focus on the text inputs for the server URL and the password
     */
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.hostUrlInput) {
            if (!hasFocus) {
                onUrlInputFocusLost();
            } else {
                showRefreshButton(false);
            }

        } else if (view.getId() == R.id.account_password) {
            onPasswordFocusChanged(hasFocus);
        }
    }

    /**
     * Handles changes in focus on the text input for the server URL.
     * <p>
     * IMPORTANT ENTRY POINT 2: When (!hasFocus), user wrote the server URL and changed to
     * other field. The operation to check the existence of the server in the entered URL is
     * started.
     * <p>
     * When hasFocus:    user 'comes back' to write again the server URL.
     */
    private void onUrlInputFocusLost() {
        if (!mServerInfo.mBaseUrl.equals(
                normalizeUrl(mHostUrlInput.getText().toString(), mServerInfo.mIsSslConn))) {
            // check server again only if the user changed something in the field
            checkOcServer();
        } else {
            if (mServerIsValid && !BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
                mLoginButton.setVisibility(View.VISIBLE);
            } else {
                mLoginButton.setVisibility(View.GONE);
            }
            showRefreshButton(!mServerIsValid);
        }
    }

    private void checkOcServer() {
        String uri = mHostUrlInput.getText().toString().trim();
        mServerIsValid = false;
        mServerIsChecked = false;
        mLoginButton.setVisibility(View.GONE);
        mServerInfo = new GetServerInfoOperation.ServerInfo();
        showRefreshButton(false);

        if (uri.length() != 0) {
            uri = stripIndexPhpOrAppsFiles(uri, mHostUrlInput);
            uri = subdomainToLower(uri, mHostUrlInput);

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true);
            } catch (IllegalArgumentException ex) {
                // Let Owncloud library check the error of the malformed URI
            }

            mServerStatusText = getResources().getString(R.string.auth_testing_connection);
            mServerStatusIcon = R.drawable.progress_small;
            showServerStatus();

            Intent getServerInfoIntent = new Intent();
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
            getServerInfoIntent.putExtra(
                    OperationsService.EXTRA_SERVER_URL,
                    normalizeUrlSuffix(uri)
            );
            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getServerInfoIntent);
            } else {
                Log_OC.e(TAG, "Server check tried with OperationService unbound!");
            }

        } else {
            mServerStatusText = "";
            mServerStatusIcon = 0;
            showServerStatus();
        }
    }

    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     * <p>
     * When (hasFocus), the button to toggle password visibility is shown.
     * <p>
     * When (!hasFocus), the button is made invisible and the password is hidden.
     *
     * @param hasFocus 'True' if focus is received, 'false' if is lost
     */
    private void onPasswordFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            showViewPasswordButton();
        } else {
            hidePassword();
            hidePasswordButton();
        }
    }

    private void showViewPasswordButton() {
        int drawable = R.drawable.ic_view;
        if (isPasswordVisible()) {
            drawable = R.drawable.ic_hide;
        }
        mPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
    }

    private boolean isPasswordVisible() {
        return ((mPasswordInput.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) ==
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    private void hidePasswordButton() {
        mPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    private void showPassword() {
        mPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        showViewPasswordButton();
    }

    private void hidePassword() {
        mPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD |
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        showViewPasswordButton();
    }

    /**
     * Checks the credentials of the user in the root of the ownCloud server
     * before creating a new local account.
     * <p>
     * For basic authorization, a check of existence of the root folder is
     * performed.
     * <p>
     * For OAuth, starts the flow to get an access token; the credentials test
     * is postponed until it is available.
     * <p>
     * IMPORTANT ENTRY POINT 4
     */
    public void onLoginClick() {
        // this check should be unnecessary
        if (mServerInfo.mVersion == null ||
                mServerInfo.mBaseUrl == null ||
                mServerInfo.mBaseUrl.length() == 0) {
            mServerStatusIcon = R.drawable.common_error;
            mServerStatusText = getResources().getString(R.string.auth_wtf_reenter_URL);
            showServerStatus();
            mLoginButton.setVisibility(View.GONE);
            return;
        }

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).
                equals(mAuthTokenType)) {
            startOauthorization();
        } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType)) {
            startSamlBasedFederatedSingleSignOnAuthorization();
        } else {
            checkBasicAuthorization();
        }
    }

    /**
     * Tests the credentials entered by the user performing a check of existence on
     * the root folder of the ownCloud server.
     */
    private void checkBasicAuthorization() {
        /// get basic credentials entered by user
        String username = mUsernameInput.getText().toString().trim();
        String password = mPasswordInput.getText().toString();

        /// be gentle with the user
        LoadingDialog dialog = LoadingDialog.newInstance(R.string.auth_trying_to_login, true);
        dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

        /// validate credentials accessing the root folder
        OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                username,
                password,
                (mServerInfo != null &&
                        mServerInfo.mVersion != null &&
                        mServerInfo.mVersion.isPreemptiveAuthenticationPreferred()
                )
        );
        accessRootFolder(credentials);
    }

    private void accessRootFolder(OwnCloudCredentials credentials) {
        mAsyncTask = new AuthenticatorAsyncTask(this);
        Object[] params = {mServerInfo.mBaseUrl, credentials};
        mAsyncTask.execute(params);
    }

    /**
     * Starts the OAuth 'grant type' flow to get an access token, with
     * a GET AUTHORIZATION request to the BUILT-IN authorization server.
     */
    private void startOauthorization() {
        // be gentle with the user
        mAuthStatusIcon = R.drawable.progress_small;
        mAuthStatusText = getResources().getString(R.string.oauth_login_connection);
        showAuthStatus();

        // GET AUTHORIZATION CODE URI to open in WebView
        OAuth2Provider oAuth2Provider = OAuth2ProvidersRegistry.getInstance().getProvider();
        oAuth2Provider.setAuthorizationServerUri(mServerInfo.mBaseUrl);

        OAuth2RequestBuilder builder = oAuth2Provider.getOperationBuilder();
        builder.setGrantType(OAuth2GrantType.AUTHORIZATION_CODE);
        builder.setRequest(OAuth2RequestBuilder.OAuthRequest.GET_AUTHORIZATION_CODE);

        if (mCustomTabPackageName != null) {
            openUrlWithCustomTab(builder.buildUri());
        } else {
            openUrlInBrowser(builder.buildUri());
        }
    }

    private void openUrlWithCustomTab(String url) {
        TypedValue backgroundColor = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, backgroundColor, true);

        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(backgroundColor.data)
                .setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right)
                .setShowTitle(true)
                .build();

        try {
            intent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openUrlInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException ae) {
            onNoBrowserInstalled();
        }
    }

    private void onNoBrowserInstalled() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.no_borwser_installed_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    /**
     * Starts the Web Single Sign On flow to get access to the root folder
     * in the server.
     */
    private void startSamlBasedFederatedSingleSignOnAuthorization() {
        /// be gentle with the user
        mAuthStatusIcon = R.drawable.progress_small;
        mAuthStatusText = getResources().getString(R.string.auth_connecting_auth_server);
        showAuthStatus();

        /// Show SAML-based SSO web dialog
        ArrayList<String> targetUrls = new ArrayList<>();
        targetUrls.add(
                mServerInfo.mBaseUrl
                        + AccountUtils.getWebdavPath(mServerInfo.mVersion, mAuthTokenType));
        LoginWebViewDialog dialog = LoginWebViewDialog.newInstance(
                targetUrls.get(0),
                targetUrls,
                AuthenticationMethod.SAML_WEB_SSO);
        dialog.show(getSupportFragmentManager(), SAML_DIALOG_TAG);
    }

    /**
     * Callback method invoked when a RemoteOperation executed by this Activity finishes.
     * <p>
     * Dispatches the operation flow to the right method.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {

        if (operation instanceof GetServerInfoOperation) {
            if (operation.hashCode() == mWaitingForOpId) {
                onGetServerInfoFinish(result);
            }   // else nothing ; only the last check operation is considered; 
            // multiple can be started if the user amends a URL quickly

        } else if (operation instanceof OAuth2GetAccessTokenOperation) {
            onGetOAuthAccessTokenFinish(result);

        } else if (operation instanceof GetRemoteUserInfoOperation) {
            // this path is only walked by SAML authentication
            onGetUserNameFinish(result);
        }
    }

    /**
     * WARNING: this method is only called in SAML authentication; rest of authentication paths
     * get their userId in {@link AuthenticatorAsyncTask}, that calls back here via
     * {@link #onAuthenticatorTaskCallback(RemoteOperationResult)}
     *
     * @param result Result of {@link GetRemoteUserInfoOperation} performed.
     */
    private void onGetUserNameFinish(RemoteOperationResult<UserInfo> result) {
        mWaitingForOpId = Long.MAX_VALUE;
        if (result.isSuccess()) {
            boolean success = false;
            // WARNING: using mDisplayName was a path used because, in the past, mId was not avialable
            // in servers with SAML; now seems that changed, and this needs to be updated. Meanwhile,
            // for any other authentication method, please 
            String username = result.getData().mDisplayName;

            if (mAction == ACTION_CREATE) {
                mUsernameInput.setText(username);
                success = createAccount(result);

            } else {
                success = updateAccount(username);
            }

            if (success) {
                finish();
            }
        } else {
            int failedStatusText = result.getCode() == ResultCode.SERVICE_UNAVAILABLE ?
                    R.string.service_unavailable : R.string.auth_fail_get_user_name;
            updateFailedAuthStatusIconAndText(failedStatusText);
            showAuthStatus();
            Log_OC.e(TAG, "Access to user name failed: " + result.getLogMessage());
        }
    }

    /**
     * Processes the result of the server check performed when the user finishes the enter of the
     * server URL.
     *
     * @param result Result of the check.
     */
    private void onGetServerInfoFinish(RemoteOperationResult<GetServerInfoOperation.ServerInfo> result) {
        /// update activity state
        mServerIsChecked = true;
        mWaitingForOpId = Long.MAX_VALUE;

        // update server status, but don't show it yet
        updateServerStatusIconAndText(result);

        if (result.isSuccess()) {
            /// SUCCESS means:
            //      1. connection succeeded, and we know if it's SSL or not
            //      2. server is installed
            //      3. we got the server version
            //      4. we got the authentication method required by the server 
            mServerInfo = result.getData();

            mServerIsValid = true;

            // Update mAuthTokenType depending on the server info
            if (!mAuthTokenType.equals(SAML_TOKEN_TYPE)) {

                if (mServerInfo.mAuthMethods.contains(AuthenticationMethod.BEARER_TOKEN)) {

                    mAuthTokenType = OAUTH_TOKEN_TYPE; // OAuth2

                } else if (mServerInfo.mAuthMethods.contains(AuthenticationMethod.BASIC_HTTP_AUTH)) {

                    mAuthTokenType = BASIC_TOKEN_TYPE; // Basic

                } else if (mServerInfo.mAuthMethods.contains(AuthenticationMethod.SAML_WEB_SSO)) {

                    updateServerStatusIconNoRegularAuth(); // overrides updateServerStatusIconAndText()
                    mServerIsValid = false;
                }
            }

        } else {
            mServerIsValid = false;
        }

        if (mServerIsValid) {
            updateAuthenticationPreFragmentVisibility();
        }

        // refresh UI
        showRefreshButton(!mServerIsValid);
        showServerStatus();
        if (mServerIsValid && !BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
            mLoginButton.setVisibility(View.VISIBLE);
        } else {
            if (mServerIsValid && mUsernameInput.getText().length() > 0 &&
                    mPasswordInput.getText().length() > 0) {
                mLoginButton.setVisibility(View.VISIBLE);
            } else {
                mLoginButton.setVisibility(View.GONE);
            }
        }

        /// very special case (TODO: move to a common place for all the remote operations)
        if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
            showUntrustedCertDialog(result);
        }
    }

    // TODO remove, if possible
    private String normalizeUrl(String url, boolean sslWhenUnprefixed) {
        if (url != null && url.length() > 0) {
            url = url.trim();
            if (!url.toLowerCase().startsWith("http://") &&
                    !url.toLowerCase().startsWith("https://")) {
                if (sslWhenUnprefixed) {
                    url = "https://" + url;
                } else {
                    url = "http://" + url;
                }
            }

            url = normalizeUrlSuffix(url);
        }
        return (url != null ? url : "");
    }

    private String normalizeUrlSuffix(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = trimUrlWebdav(url);
        return url;
    }

    private String stripIndexPhpOrAppsFiles(String url, EditText mHostUrlInput) {
        if (url.endsWith("/index.php")) {
            url = url.substring(0, url.lastIndexOf("/index.php"));
            mHostUrlInput.setText(url);
        } else if (url.contains("/index.php/apps/")) {
            url = url.substring(0, url.lastIndexOf("/index.php/apps/"));
            mHostUrlInput.setText(url);
        }

        return url;
    }

    private String subdomainToLower(String url, EditText mHostUrlInput) {
        if (url.toLowerCase().startsWith("http://") ||
                url.toLowerCase().startsWith("https://")) {
            if (url.indexOf("/", 8) != -1) {
                url = url.substring(0, url.indexOf("/", 8)).toLowerCase()
                        + url.substring(url.indexOf("/", 8), url.length());
            } else {
                url = url.substring(0, url.length()).toLowerCase();
            }

            mHostUrlInput.setText(url);
        } else {
            if (url.contains("/")) {
                url = url.substring(0, url.indexOf("/")).toLowerCase()
                        + url.substring(url.indexOf("/"), url.length());
            } else {
                url = url.substring(0, url.length()).toLowerCase();
            }

            mHostUrlInput.setText(url);
        }

        return url;
    }

    // TODO remove, if possible
    private String trimUrlWebdav(String url) {
        if (url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_4_0_AND_LATER)) {
            url = url.substring(0, url.length() - AccountUtils.WEBDAV_PATH_4_0_AND_LATER.length());
        }
        return url;
    }

    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private void updateServerStatusIconAndText(RemoteOperationResult result) {
        mServerStatusIcon = R.drawable.common_error;    // the most common case in the switch below

        switch (result.getCode()) {
            case OK_SSL:
                mServerStatusIcon = R.drawable.ic_lock;
                mServerStatusText = getResources().getString(R.string.auth_secure_connection);
                break;

            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase().startsWith("http://")) {
                    mServerStatusText = getResources().getString(R.string.auth_connection_established);
                    mServerStatusIcon = R.drawable.ic_ok;
                } else {
                    mServerStatusText = getResources().getString(R.string.auth_nossl_plain_ok_title);
                    mServerStatusIcon = R.drawable.ic_lock_open;
                }
                break;

            case OK_REDIRECT_TO_NON_SECURE_CONNECTION:
                mServerStatusIcon = R.drawable.ic_lock_open;
                mServerStatusText = ErrorMessageAdapter.getResultMessage(result, null, getResources());
                break;
            case NO_NETWORK_CONNECTION:
                mServerStatusIcon = R.drawable.no_network;
            default:
                mServerStatusText = ErrorMessageAdapter.getResultMessage(result, null, getResources());
        }
    }

    /**
     * Chooses the right icon and text to show to the user for the received operation result.
     *
     * @param result Result of a remote operation performed in this activity
     */
    private void updateAuthStatusIconAndText(RemoteOperationResult result) {
        mAuthStatusIcon = R.drawable.common_error; // the most common case in the switch below

        switch (result.getCode()) {
            case OK_SSL:
                mAuthStatusIcon = R.drawable.ic_lock;
                mAuthStatusText = getResources().getString(R.string.auth_secure_connection);
                break;
            case OK_NO_SSL:
            case OK:
                if (mHostUrlInput.getText().toString().trim().toLowerCase().startsWith("http://")) {
                    mAuthStatusText = getResources().getString(R.string.auth_connection_established);
                    mAuthStatusIcon = R.drawable.ic_ok;
                } else {
                    mAuthStatusText = getResources().getString(R.string.auth_nossl_plain_ok_title);
                    mAuthStatusIcon = R.drawable.ic_lock_open;
                }
                break;

            case NO_NETWORK_CONNECTION:
                mAuthStatusIcon = R.drawable.no_network;
            default:
                mAuthStatusText = ErrorMessageAdapter.getResultMessage(result, null, getResources());
        }
    }

    private void updateFailedAuthStatusIconAndText(int failedStatusText) {
        mAuthStatusIcon = R.drawable.common_error;
        mAuthStatusText = getResources().getString(failedStatusText);
    }

    private void updateServerStatusIconNoRegularAuth() {
        mServerStatusIcon = R.drawable.common_error;
        mServerStatusText = getResources().getString(R.string.auth_can_not_auth_against_server);
    }

    /**
     * Processes the result of the request for an access token sent to an OAuth authorization server
     *
     * @param result Result of the operation.
     */
    private void onGetOAuthAccessTokenFinish(RemoteOperationResult<Map<String, String>> result) {
        mWaitingForOpId = Long.MAX_VALUE;
        dismissDialog(WAIT_DIALOG_TAG);

        if (result.isSuccess()) {
            /// be gentle with the user
            LoadingDialog dialog = LoadingDialog.newInstance(R.string.auth_trying_to_login, true);
            dialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);

            /// time to test the retrieved access token on the ownCloud server
            @SuppressWarnings("unchecked")
            Map<String, String> tokens = result.getData();
            mAuthToken = tokens.get(OAuth2Constants.KEY_ACCESS_TOKEN);

            mRefreshToken = tokens.get(OAuth2Constants.KEY_REFRESH_TOKEN);

            /// validate token accessing to root folder / getting session
            OwnCloudCredentials credentials = OwnCloudCredentialsFactory.newBearerCredentials(
                    tokens.get(OAuth2Constants.KEY_USER_ID),
                    mAuthToken
            );

            accessRootFolder(credentials);

        } else {
            updateAuthStatusIconAndText(result);
            showAuthStatus();
            Log_OC.d(TAG, "Access failed: " + result.getLogMessage());
        }
    }

    /**
     * Processes the result of the access check performed to try the user credentials.
     * <p>
     * Creates a new account through the AccountManager.
     *
     * @param result Result of the operation.
     */
    @Override
    public void onAuthenticatorTaskCallback(RemoteOperationResult result) {
        mWaitingForOpId = Long.MAX_VALUE;
        dismissDialog(WAIT_DIALOG_TAG);
        mAsyncTask = null;

        if (result.isSuccess()) {
            Log_OC.d(TAG, "Successful access - time to save the account");

            boolean success = false;
            String username = ((RemoteOperationResult<UserInfo>) result).getData().mId;

            if (mAction == ACTION_CREATE) {

                if (!BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {
                    mUsernameInput.setText(username);
                }

                success = createAccount(result);

            } else {

                try {

                    if (BASIC_TOKEN_TYPE.equals(mAuthTokenType)) {

                        updateAccountAuthentication();
                        success = true;

                    } else {

                        success = updateAccount(username);
                    }

                } catch (AccountNotFoundException e) {
                    Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                    Toast.makeText(this, R.string.auth_account_does_not_exist,
                            Toast.LENGTH_SHORT).show();
                    // do not use a Snackbar, finishing right now!
                    finish();
                }
            }

            if (success) {
                finish();
            }

        } else if (result.isServerFail() || result.isException()) {
            /// server errors or exceptions in authorization take to requiring a new check of 
            /// the server
            mServerIsChecked = true;
            mServerIsValid = false;
            mServerInfo = new GetServerInfoOperation.ServerInfo();

            // update status icon and text
            updateServerStatusIconAndText(result);
            showServerStatus();
            mAuthStatusIcon = 0;
            mAuthStatusText = "";
            showAuthStatus();

            // update input controls state
            showRefreshButton(true);
            mLoginButton.setVisibility(View.GONE);

            // very special case (TODO: move to a common place for all the remote operations)
            if (result.getCode() == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) {
                showUntrustedCertDialog(result);
            }

        } else {    // authorization fail due to client side - probably wrong credentials
            updateAuthStatusIconAndText(result);
            showAuthStatus();
            Log_OC.d(TAG, "Access failed: " + result.getLogMessage());
        }
    }

    /**
     * Creates a new account through the Account Authenticator that started this activity.
     * <p>
     * This makes the account permanent.
     * <p>
     * TODO Decide how to name the OAuth accounts
     */
    private boolean createAccount(RemoteOperationResult<UserInfo> authResult) {
        /// create and save new ownCloud account
        boolean isOAuth = AccountTypeUtils.
                getAuthTokenTypeAccessToken(MainApp.getAccountType()).equals(mAuthTokenType);
        boolean isSaml = AccountTypeUtils.
                getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).equals(mAuthTokenType);

        String lastPermanentLocation = authResult.getLastPermanentLocation();
        if (lastPermanentLocation != null) {
            mServerInfo.mBaseUrl = AccountUtils.trimWebdavSuffix(lastPermanentLocation);
        }

        Uri uri = Uri.parse(mServerInfo.mBaseUrl);
        String username = mUsernameInput.getText().toString().trim();
        String accountName = com.owncloud.android.lib.common.accounts.AccountUtils.
                buildAccountName(uri, username);
        Account newAccount = new Account(accountName, MainApp.getAccountType());
        if (AccountUtils.exists(newAccount.name, getApplicationContext())) {
            // fail - not a new account, but an existing one; disallow
            RemoteOperationResult result = new RemoteOperationResult(ResultCode.ACCOUNT_NOT_NEW);
            updateAuthStatusIconAndText(result);
            showAuthStatus();
            Log_OC.d(TAG, result.getLogMessage());
            return false;

        } else {
            mAccount = newAccount;

            if (isOAuth || isSaml) {
                // with external authorizations, the password is never input in the app
                mAccountMgr.addAccountExplicitly(mAccount, "", null);
            } else {
                mAccountMgr.addAccountExplicitly(
                        mAccount, mPasswordInput.getText().toString(), null
                );
            }

            // include account version with the new account
            mAccountMgr.setUserData(
                    mAccount,
                    Constants.KEY_OC_ACCOUNT_VERSION,
                    Integer.toString(AccountUtils.ACCOUNT_VERSION)
            );

            /// add the new account as default in preferences, if there is none already
            Account defaultAccount = AccountUtils.getCurrentOwnCloudAccount(this);
            if (defaultAccount == null) {
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.commit();
            }

            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same 
            //  effect as addAccountExplicitly, but it's not well done
            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, MainApp.getAccountType());
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
            intent.putExtra(AccountManager.KEY_USERDATA, username);
            if (isOAuth || isSaml) {
                mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);
            }
            /// add user data to the new account; TODO probably can be done in the last parameter 
            //      addAccountExplicitly, or in KEY_USERDATA
            mAccountMgr.setUserData(
                    mAccount, Constants.KEY_OC_VERSION, mServerInfo.mVersion.getVersion()
            );
            mAccountMgr.setUserData(
                    mAccount, Constants.KEY_OC_BASE_URL, mServerInfo.mBaseUrl
            );
            if (authResult.getData() != null) {
                try {
                    UserInfo userInfo = (UserInfo) authResult.getData();
                    mAccountMgr.setUserData(
                            mAccount, Constants.KEY_DISPLAY_NAME, userInfo.mDisplayName
                    );
                } catch (ClassCastException c) {
                    Log_OC.w(TAG, "Couldn't get display name for " + username);
                }
            } else {
                Log_OC.w(TAG, "Couldn't get display name for " + username);
            }

            if (isSaml) {
                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "TRUE");
            } else if (isOAuth) {
                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2, "TRUE");
                mAccountMgr.setUserData(mAccount, Constants.KEY_OAUTH2_REFRESH_TOKEN, mRefreshToken);
            }

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            // Notify login to Document Provider
            String authority = getResources().getString(R.string.document_provider_authority);
            Uri rootsUri = DocumentsContract.buildRootsUri(authority);
            getContentResolver().notifyChange(rootsUri, null);

            return true;
        }
    }

    /**
     * Update an existing account
     * <p>
     * Check if the username of the account to update is the same as the username in the current
     * account, calling {@link #updateAccountAuthentication()} if so and showing an error otherwise
     *
     * @param username in the server
     * @return true if account is properly update, false otherwise
     */
    private boolean updateAccount(String username) {

        boolean success = false;

        RemoteOperationResult result;

        if (!mUsernameInput.getText().toString().trim().equals(username)) {
            // fail - not a new account, but an existing one; disallow
            result = new RemoteOperationResult(ResultCode.ACCOUNT_NOT_THE_SAME);
            mAuthToken = "";
            updateAuthStatusIconAndText(result);
            showAuthStatus();
            Log_OC.d(TAG, result.getLogMessage());

        } else {
            try {
                updateAccountAuthentication();
                success = true;

            } catch (AccountNotFoundException e) {
                Log_OC.e(TAG, "Account " + mAccount + " was removed!", e);
                Toast.makeText(this, R.string.auth_account_does_not_exist,
                        Toast.LENGTH_SHORT).show();
                // don't use a Snackbar, finishing right now
                finish();
            }
        }
        return success;
    }

    /**
     * Updates the authentication token.
     * <p>
     * Sets the proper response so that the AccountAuthenticator that started this activity
     * saves a new authorization token for mAccount.
     * <p>
     * Kills the session kept by OwnCloudClientManager so that a new one will be created with
     * the new credentials when needed.
     */
    private void updateAccountAuthentication() throws AccountNotFoundException {

        Bundle response = new Bundle();
        response.putString(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
        response.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccount.type);

        String OAuthSupported = mAccountMgr.getUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2);
        String SAMLSupported = mAccountMgr.getUserData(mAccount, Constants.
                KEY_SUPPORTS_SAML_WEB_SSO);

        if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.getAccountType()).
                equals(mAuthTokenType)) { // OAuth
            response.putString(AccountManager.KEY_AUTHTOKEN, mAuthToken);
            // the next line is necessary, notifications are calling directly to the
            // AuthenticatorActivity to update, without AccountManager intervention
            mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);

            if (OAuthSupported == null || OAuthSupported != null && OAuthSupported.equals("FALSE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2, "TRUE");
            }

            if (SAMLSupported != null && SAMLSupported.equals("TRUE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "FALSE");
            }

        } else if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType)) { // SAML

            if (SAMLSupported == null || SAMLSupported != null && SAMLSupported.equals("FALSE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "TRUE");
            }

            if (OAuthSupported != null && OAuthSupported.equals("TRUE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2, "FALSE");
            }

            response.putString(AccountManager.KEY_AUTHTOKEN, mAuthToken);
            // the next line is necessary; by now, notifications are calling directly to the
            // AuthenticatorActivity to update, without AccountManager intervention
            mAccountMgr.setAuthToken(mAccount, mAuthTokenType, mAuthToken);

        } else { // BASIC

            if (SAMLSupported != null && SAMLSupported.equals("TRUE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "FALSE");
            }

            if (OAuthSupported != null && OAuthSupported.equals("TRUE")) {

                mAccountMgr.setUserData(mAccount, Constants.KEY_SUPPORTS_OAUTH2, "FALSE");
            }

            response.putString(AccountManager.KEY_AUTHTOKEN, mPasswordInput.getText().toString());
            mAccountMgr.setPassword(mAccount, mPasswordInput.getText().toString());
        }

        // remove managed clients for this account to enforce creation with fresh credentials
        OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, this);
        OwnCloudClientManagerFactory.getDefaultSingleton().removeClientFor(ocAccount);

        setAccountAuthenticatorResult(response);
        final Intent intent = new Intent();
        intent.putExtras(response);
        setResult(RESULT_OK, intent);
    }

    /**
     * Starts and activity to open the 'new account' page in the ownCloud web site
     *
     * @param view 'Account register' button
     */
    public void onRegisterClick(View view) {
        Intent register = new Intent(
                ACTION_VIEW, Uri.parse(getString(R.string.welcome_link_url))
        );
        setResult(RESULT_CANCELED);
        startActivity(register);
    }

    /**
     * Updates the content and visibility state of the icon and text associated
     * to the last check on the ownCloud server.
     */
    private void showServerStatus() {
        if (mServerStatusIcon == 0 && (mServerStatusText == null || mServerStatusText.length() == 0)) {
            mServerStatusView.setVisibility(View.INVISIBLE);

        } else {
            mServerStatusView.setText(mServerStatusText);
            mServerStatusView.setCompoundDrawablesWithIntrinsicBounds(mServerStatusIcon, 0, 0, 0);
            mServerStatusView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Updates the content and visibility state of the icon and text associated
     * to the interactions with the OAuth authorization server.
     */
    private void showAuthStatus() {
        if (mAuthStatusIcon == 0 && (mAuthStatusText == null || mAuthStatusText.length() == 0)) {
            mAuthStatusView.setVisibility(View.INVISIBLE);

        } else {
            mAuthStatusView.setText(mAuthStatusText);
            mAuthStatusView.setCompoundDrawablesWithIntrinsicBounds(mAuthStatusIcon, 0, 0, 0);
            mAuthStatusView.setVisibility(View.VISIBLE);
        }
    }

    private void showRefreshButton(boolean show) {
        if (show) {
            mCheckServerButton.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.VISIBLE);
        } else {
            mRefreshButton.setVisibility(View.GONE);
            mCheckServerButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when the eye icon in the password field is clicked.
     * <p>
     * Toggles the visibility of the password in the field.
     */
    public void onViewPasswordClick() {
        int selectionStart = mPasswordInput.getSelectionStart();
        int selectionEnd = mPasswordInput.getSelectionEnd();
        if (isPasswordVisible()) {
            hidePassword();
        } else {
            showPassword();
        }
        mPasswordInput.setSelection(selectionStart, selectionEnd);
    }

    /**
     * Called when the 'action' button in an IME is pressed ('enter' in software keyboard).
     * <p>
     * Used to trigger the authentication check when the user presses 'enter' after writing the
     * password, or to throw the server test when the only field on screen is the URL input field.
     */
    @Override
    public boolean onEditorAction(TextView inputField, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && inputField != null &&
                inputField.equals(mPasswordInput)) {
            if (mLoginButton.isEnabled()) {
                mLoginButton.performClick();
            }

        } else if (actionId == EditorInfo.IME_ACTION_NEXT && inputField != null &&
                inputField.equals(mHostUrlInput)) {
            if (!AccountTypeUtils.getAuthTokenTypePass(MainApp.getAccountType()).
                    equals(mAuthTokenType)) {
                checkOcServer();
            }
        }
        return false;   // always return false to grant that the software keyboard is hidden anyway
    }

    private abstract static class RightDrawableOnTouchListener implements OnTouchListener {

        private int fuzz = 75;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Drawable rightDrawable = null;
            if (view instanceof TextView) {
                Drawable[] drawables = ((TextView) view).getCompoundDrawables();
                if (drawables.length > 2) {
                    rightDrawable = drawables[2];
                }
            }
            if (rightDrawable != null) {
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                final Rect bounds = rightDrawable.getBounds();
                if (x >= (view.getRight() - bounds.width() - fuzz) &&
                        x <= (view.getRight() - view.getPaddingRight() + fuzz) &&
                        y >= (view.getPaddingTop() - fuzz) &&
                        y <= (view.getHeight() - view.getPaddingBottom()) + fuzz) {

                    return onDrawableTouch(event);
                }
            }
            return false;
        }

        public abstract boolean onDrawableTouch(final MotionEvent event);
    }

    private void getRemoteUserNameOperation(String sessionCookie) {
        Intent getUserNameIntent = new Intent();
        getUserNameIntent.setAction(OperationsService.ACTION_GET_USER_NAME);
        getUserNameIntent.putExtra(OperationsService.EXTRA_SERVER_URL, mServerInfo.mBaseUrl);
        getUserNameIntent.putExtra(OperationsService.EXTRA_COOKIE, sessionCookie);

        if (mOperationsServiceBinder != null) {
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUserNameIntent);
        }
    }

    @Override
    public void onSsoFinished(String sessionCookie) {
        if (sessionCookie != null && sessionCookie.length() > 0) {
            Log_OC.d(TAG, "Successful SSO - time to save the account");
            mAuthToken = sessionCookie;
            getRemoteUserNameOperation(sessionCookie);
            Fragment fd = getSupportFragmentManager().findFragmentByTag(SAML_DIALOG_TAG);
            if (fd != null && fd instanceof DialogFragment) {
                Dialog d = ((DialogFragment) fd).getDialog();
                if (d != null && d.isShowing()) {
                    d.dismiss();
                }
            }

        } else {
            // TODO - show fail
            Log_OC.d(TAG, "SSO failed");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(MainApp.getAccountType()).
                equals(mAuthTokenType) &&
                mHostUrlInput.hasFocus() && event.getAction() == MotionEvent.ACTION_DOWN) {
            checkOcServer();
        }
        return super.onTouchEvent(event);
    }

    /**
     * Show untrusted cert dialog
     */
    public void showUntrustedCertDialog(
            X509Certificate x509Certificate, SslError error, SslErrorHandler handler
    ) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog;
        if (x509Certificate == null) {
            dialog = SslUntrustedCertDialog.newInstanceForEmptySslError(error, handler);
        } else {
            dialog = SslUntrustedCertDialog.
                    newInstanceForFullSslError(x509Certificate, error, handler);
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG);
    }

    /**
     * Show untrusted cert dialog
     */
    private void showUntrustedCertDialog(RemoteOperationResult result) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog = SslUntrustedCertDialog.
                newInstanceForFullSslError((CertificateCombinedException) result.getException());
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG);

    }

    public void onSavedCertificate() {
        Fragment fd = getSupportFragmentManager().findFragmentByTag(SAML_DIALOG_TAG);
        if (fd == null) {
            // if SAML dialog is not shown, 
            // the SslDialog was shown due to an SSL error in the server check
            checkOcServer();
        }
    }

    @Override
    public void onFailedSavingCertificate() {
        dismissDialog(SAML_DIALOG_TAG);
        showSnackMessage(R.string.ssl_validator_not_saved);
    }

    @Override
    public void onCancelCertificate() {
        dismissDialog(SAML_DIALOG_TAG);
    }

    private void doOnResumeAndBound() {
        //Log_OC.e(TAG, "registering to listen for operation callbacks" );
        mOperationsServiceBinder.addOperationListener(AuthenticatorActivity.this, mHandler);
        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int) mWaitingForOpId, this);
        }

        if (mPendingAutoCheck) {
            checkOcServer();
            mPendingAutoCheck = false;
        }
    }

    private void dismissDialog(String dialogTag) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(dialogTag);
        if (frag != null && frag instanceof DialogFragment) {
            DialogFragment dialog = (DialogFragment) frag;
            dialog.dismiss();
        }
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(
                    new ComponentName(AuthenticatorActivity.this, OperationsService.class)
            )) {
                mOperationsServiceBinder = (OperationsServiceBinder) service;

                doOnResumeAndBound();

            }

        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(
                    new ComponentName(AuthenticatorActivity.this, OperationsService.class)
            )) {
                Log_OC.e(TAG, "Operations service crashed");
                mOperationsServiceBinder = null;
            }
        }
    }

    /**
     * Create and show dialog for request authentication to the user
     *
     * @param webView Web view to embed into the authentication dialog.
     * @param handler Object responsible for catching and recovering HTTP authentication fails.
     */
    public void createAuthenticationDialog(WebView webView, HttpAuthHandler handler) {

        // Show a dialog with the certificate info
        CredentialsDialogFragment dialog =
                CredentialsDialogFragment.newInstanceForCredentials(webView, handler);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog.setCancelable(false);
        dialog.show(ft, CREDENTIALS_DIALOG_TAG);

        if (!mIsFirstAuthAttempt) {
            showSnackMessage(R.string.saml_authentication_wrong_pass);

        } else {
            mIsFirstAuthAttempt = false;
        }
    }

    /**
     * For retrieving the clicking on authentication cancel button
     */
    public void doNegativeAuthenticatioDialogClick() {
        mIsFirstAuthAttempt = true;
    }

    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }
}