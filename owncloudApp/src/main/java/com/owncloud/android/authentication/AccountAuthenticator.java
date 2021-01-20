/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.oauth.OAuthUtils;
import com.owncloud.android.domain.UseCaseResult;
import com.owncloud.android.domain.authentication.oauth.OIDCDiscoveryUseCase;
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase;
import com.owncloud.android.domain.authentication.oauth.model.OIDCServerConfiguration;
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest;
import com.owncloud.android.domain.authentication.oauth.model.TokenResponse;
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.presentation.ui.authentication.AuthenticatorConstants;
import com.owncloud.android.presentation.ui.authentication.LoginActivity;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.File;

import static com.owncloud.android.data.authentication.AuthenticationConstantsKt.KEY_OAUTH2_REFRESH_TOKEN;
import static com.owncloud.android.presentation.ui.authentication.AuthenticatorConstants.KEY_AUTH_TOKEN_TYPE;
import static org.koin.java.KoinJavaComponent.inject;

/**
 * Authenticator for ownCloud accounts.
 *
 * Controller class accessed from the system AccountManager, providing integration of ownCloud accounts with the
 * Android system.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    /**
     * Is used by android system to assign accounts to authenticators. Should be
     * used by application and all extensions.
     */
    private static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_ACCOUNT = "account";

    private Context mContext;

    private Handler mHandler;

    AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
        mHandler = new Handler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) {
        Timber.i("Adding account with type " + accountType + " and auth token " + authTokenType);

        final Bundle bundle = new Bundle();

        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccountsByType(MainApp.Companion.getAccountType());

        if (mContext.getResources().getBoolean(R.bool.multiaccount_support) || accounts.length < 1) {
            try {
                validateAccountType(accountType);
            } catch (AuthenticatorException e) {
                Timber.e(e, "Failed to validate account type %s", accountType);
                return e.getFailureBundle();
            }

            final Intent intent = new Intent(mContext, LoginActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
            intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
            intent.putExtra(AuthenticatorConstants.EXTRA_ACTION, AuthenticatorConstants.ACTION_CREATE);

            setIntentFlags(intent);

            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        } else {
            // Return an error
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            final String message = String.format(mContext.getString(R.string.auth_unsupported_multiaccount),
                    mContext.getString(R.string.app_name));
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, message);

            mHandler.post(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
        }

        return bundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account, Bundle options) {
        try {
            validateAccountType(account.type);
        } catch (AuthenticatorException e) {
            Timber.e(e, "Failed to validate account type %s", account.type);
            return e.getFailureBundle();
        }
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);

        setIntentFlags(intent);

        Bundle resultBundle = new Bundle();
        resultBundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return resultBundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
                                 String accountType) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                               Account account, String authTokenType, Bundle options) {
        /// validate parameters
        try {
            validateAccountType(account.type);
            validateAuthTokenType(authTokenType);
        } catch (AuthenticatorException e) {
            Timber.e(e, "Failed to validate account type %s", account.type);
            return e.getFailureBundle();
        }

        String accessToken;

        /// check if required token is stored
        final AccountManager accountManager = AccountManager.get(mContext);
        if (authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(MainApp.Companion.getAccountType()))) {
            // Basic
            accessToken = accountManager.getPassword(account);
        } else {
            // OAuth, gets an auth token from the AccountManager's cache. If no auth token is cached for
            // this account, null will be returned
            accessToken = accountManager.peekAuthToken(account, authTokenType);
            if (accessToken == null && canBeRefreshed(authTokenType)) {
                refreshToken(accountAuthenticatorResponse, account, authTokenType, accountManager, options);
                return null;  // We return null because of the callbacks used within refreshToken method
            }
        }

        if (accessToken != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.Companion.getAccountType());
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
            return result;
        }

        /// if not stored, return Intent to access the LoginActivity and UPDATE the token for the account
        return prepareBundleToAccessLoginActivity(accountAuthenticatorResponse, account, authTokenType, options);
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle options) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        setIntentFlags(intent);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAccountRemovalAllowed(
            AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        return super.getAccountRemovalAllowed(response, account);
    }

    private void setIntentFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
    }

    private void validateAccountType(String type)
            throws UnsupportedAccountTypeException {
        if (!type.equals(MainApp.Companion.getAccountType())) {
            throw new UnsupportedAccountTypeException();
        }
    }

    private void validateAuthTokenType(String authTokenType)
            throws UnsupportedAuthTokenTypeException {
        if (!authTokenType.equals(MainApp.Companion.getAuthTokenType()) &&
                !authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(MainApp.Companion.getAccountType())) &&
                !authTokenType.equals(AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.Companion.getAccountType())) &&
                !authTokenType.equals(AccountTypeUtils.getAuthTokenTypeRefreshToken(MainApp.Companion.getAccountType()))
        ) {
            throw new UnsupportedAuthTokenTypeException();
        }
    }

    public static class AuthenticatorException extends Exception {
        private static final long serialVersionUID = 1L;
        private Bundle mFailureBundle;

        AuthenticatorException(int code, String errorMsg) {
            mFailureBundle = new Bundle();
            mFailureBundle.putInt(AccountManager.KEY_ERROR_CODE, code);
            mFailureBundle
                    .putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg);
        }

        Bundle getFailureBundle() {
            return mFailureBundle;
        }
    }

    public static class UnsupportedAccountTypeException extends
            AuthenticatorException {
        private static final long serialVersionUID = 1L;

        UnsupportedAccountTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported account type");
        }
    }

    public static class UnsupportedAuthTokenTypeException extends
            AuthenticatorException {
        private static final long serialVersionUID = 1L;

        UnsupportedAuthTokenTypeException() {
            super(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION,
                    "Unsupported auth token type");
        }
    }

    private boolean canBeRefreshed(String authTokenType) {
        return (authTokenType.equals(AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.Companion.
                getAccountType())));
    }

    private void refreshToken(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String authTokenType,
            AccountManager accountManager,
            Bundle options) {

        // Prepare everything to perform the token request
        String refreshToken = accountManager.getUserData(account, KEY_OAUTH2_REFRESH_TOKEN);

        if (refreshToken == null || refreshToken.isEmpty()) {
            Timber.w("No refresh token stored for silent renewal of access token");
            return;
        }

        Timber.d("Ready to exchange for new tokens. Account: [ %s ], Refresh token: [ %s ]", account.name,
                refreshToken);

        String baseUrl = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL);

        // OIDC Discovery
        @NotNull Lazy<OIDCDiscoveryUseCase> oidcDiscoveryUseCase = inject(OIDCDiscoveryUseCase.class);
        OIDCDiscoveryUseCase.Params oidcDiscoveryUseCaseParams = new OIDCDiscoveryUseCase.Params(baseUrl);
        UseCaseResult<OIDCServerConfiguration> oidcServerConfigurationUseCaseResult =
                oidcDiscoveryUseCase.getValue().execute(oidcDiscoveryUseCaseParams);

        String tokenEndpoint;
        if (oidcServerConfigurationUseCaseResult.isSuccess()) {
            Timber.d("OIDC Discovery success. Server discovery info: [ %s ]",
                    oidcServerConfigurationUseCaseResult.getDataOrNull());

            // Use token endpoint retrieved from oidc discovery
            tokenEndpoint = oidcServerConfigurationUseCaseResult.getDataOrNull().getToken_endpoint();

        } else {
            Timber.d("OIDC Discovery failed. Server discovery info: [ %s ]",
                    oidcServerConfigurationUseCaseResult.getThrowableOrNull().toString());
            tokenEndpoint = baseUrl + File.separator + mContext.getString(R.string.oauth2_url_endpoint_access);
        }

        String clientAuth = OAuthUtils.Companion.getClientAuth(mContext.getString(R.string.oauth2_client_secret),
                mContext.getString(R.string.oauth2_client_id));

        TokenRequest oauthTokenRequest = new TokenRequest.RefreshToken(
                baseUrl,
                tokenEndpoint,
                clientAuth,
                refreshToken
        );

        // Token exchange
        @NotNull Lazy<RequestTokenUseCase> requestTokenUseCase = inject(RequestTokenUseCase.class);
        RequestTokenUseCase.Params requestTokenParams = new RequestTokenUseCase.Params(oauthTokenRequest);
        UseCaseResult<TokenResponse> tokenResponseResult = requestTokenUseCase.getValue().execute(requestTokenParams);

        if (tokenResponseResult.isSuccess()) {
            handleSuccessfulRefreshToken(tokenResponseResult.getDataOrNull(), accountAuthenticatorResponse,
                    account, authTokenType, accountManager, refreshToken);
        } else {
            handleFailedRefreshToken(tokenResponseResult.getThrowableOrNull(), accountAuthenticatorResponse,
                    account, authTokenType, options);
        }
    }

    private void handleSuccessfulRefreshToken(
            TokenResponse tokenResponse,
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String authTokenType,
            AccountManager accountManager,
            String oldRefreshToken
    ) {
        if (tokenResponse != null) {
            String newAccessToken = tokenResponse.getAccessToken();
            accountManager.setAuthToken(account, authTokenType, newAccessToken);

            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.Companion.getAccountType());
            result.putString(AccountManager.KEY_AUTHTOKEN, newAccessToken);
            accountAuthenticatorResponse.onResult(result);

            String refreshTokenToUseFromNowOn;

            if (tokenResponse.getRefreshToken() != null) {
                refreshTokenToUseFromNowOn = tokenResponse.getRefreshToken();
            } else {
                refreshTokenToUseFromNowOn = oldRefreshToken;
            }

            Timber.d("Token refreshed successfully. New access token: [ %s ]. New refresh token: [ %s ]",
                    newAccessToken, refreshTokenToUseFromNowOn);
            accountManager.setUserData(account, KEY_OAUTH2_REFRESH_TOKEN, refreshTokenToUseFromNowOn);
        }
    }

    private void handleFailedRefreshToken(
            Throwable exception,
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String authTokenType,
            Bundle options
    ) {
        Timber.e(exception, "OAuth request to refresh access token failed. Preparing to access Login Activity");
        Bundle result = prepareBundleToAccessLoginActivity(accountAuthenticatorResponse, account, authTokenType, options);
        accountAuthenticatorResponse.onResult(result);
    }

    /**
     * Return bundle with intent to access LoginActivity and UPDATE the token for the account
     */
    private Bundle prepareBundleToAccessLoginActivity(
            AccountAuthenticatorResponse accountAuthenticatorResponse,
            Account account,
            String authTokenType,
            Bundle options
    ) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                accountAuthenticatorResponse);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(AuthenticatorConstants.EXTRA_ACCOUNT, account);
        intent.putExtra(
                AuthenticatorConstants.EXTRA_ACTION,
                AuthenticatorConstants.ACTION_UPDATE_EXPIRED_TOKEN
        );

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
}
