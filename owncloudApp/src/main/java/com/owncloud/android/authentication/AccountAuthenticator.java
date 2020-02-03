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
import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.oauth.OAuthConnectionBuilder;
import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import timber.log.Timber;

/**
 * Authenticator for ownCloud accounts.
 *
 * Controller class accessed from the system AccountManager, providing integration of ownCloud accounts with the
 * Android system.
 *
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    /**
     * Is used by android system to assign accounts to authenticators. Should be
     * used by application and all extensions.
     */
    static final String KEY_AUTH_TOKEN_TYPE = "authTokenType";
    private static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    private static final String KEY_LOGIN_OPTIONS = "loginOptions";
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

            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
            intent.putExtra(KEY_REQUIRED_FEATURES, requiredFeatures);
            intent.putExtra(KEY_LOGIN_OPTIONS, options);
            intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE);

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
        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);

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

        String accessToken = "";

        /// check if required token is stored
        final AccountManager accountManager = AccountManager.get(mContext);
        if (authTokenType.equals(AccountTypeUtils.getAuthTokenTypePass(MainApp.Companion.getAccountType()))) {
            accessToken = accountManager.getPassword(account);
        } else {
            // Gets an auth token from the AccountManager's cache. If no auth token is cached for
            // this account, null will be returned
            accessToken = accountManager.peekAuthToken(account, authTokenType);
            if (accessToken == null && canBeRefreshed(authTokenType)) {
                refreshToken(account, authTokenType, accountManager, accountAuthenticatorResponse);
                return null;  // We return null because of the callbacks used within refreshToken methodqq
            }
        }

        if (accessToken != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.Companion.getAccountType());
            result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
            return result;
        }

        /// if not stored, return Intent to access the AuthenticatorActivity and UPDATE the token for the account
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
        intent.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
        intent.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
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
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);
        intent.putExtra(KEY_ACCOUNT, account);
        intent.putExtra(KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_LOGIN_OPTIONS, options);
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

    private void refreshToken(Account account, String authTokenType, AccountManager accountManager,
                              AccountAuthenticatorResponse accountAuthenticatorResponse) {
        String refreshToken = accountManager.getUserData(
                account,
                AccountUtils.Constants.KEY_OAUTH2_REFRESH_TOKEN
        );

        if (refreshToken == null || refreshToken.length() <= 0) {
            Timber.w("No refresh token stored for silent renewal of access token");
            return;
        }

        AppAuthConfiguration.Builder appAuthConfigurationBuilder = new AppAuthConfiguration.Builder();
        appAuthConfigurationBuilder.setConnectionBuilder(new OAuthConnectionBuilder());
        AuthorizationService authService = new AuthorizationService(mContext,
                appAuthConfigurationBuilder.build());

        AuthStateManager authStateManager = AuthStateManager.getInstance(mContext);
        AuthState authState = authStateManager.getCurrent();

        ClientAuthentication clientAuth =
                new ClientSecretBasic(mContext.getString(R.string.oauth2_client_secret));

        authService.performTokenRequest(
                authState.createTokenRefreshRequest(),
                clientAuth,
                (tokenResponse, authorizationException) -> {
                    String newAccessToken = "";
                    String newRefreshToken = "";

                    if (tokenResponse != null || authorizationException != null) {  // Save authorization state
                        authStateManager.updateAfterTokenResponse(tokenResponse, authorizationException);
                    }

                    if (tokenResponse != null && tokenResponse.accessToken != null &&
                            tokenResponse.refreshToken != null) {
                        authStateManager.updateAfterTokenResponse(tokenResponse, authorizationException);

                        newAccessToken = tokenResponse.accessToken;
                        Timber.d("Set OAuth2 new access token in account: %s", newAccessToken);
                        accountManager.setAuthToken(account, authTokenType, newAccessToken);

                        final Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, MainApp.Companion.getAccountType());
                        result.putString(AccountManager.KEY_AUTHTOKEN, newAccessToken);
                        accountAuthenticatorResponse.onResult(result);

                        newRefreshToken = tokenResponse.refreshToken;
                        Timber.d("Set OAuth2 new refresh token in account: %s", newRefreshToken);
                        accountManager.setUserData(
                                account,
                                AccountUtils.Constants.KEY_OAUTH2_REFRESH_TOKEN,
                                refreshToken
                        );

                    } else if (authorizationException != null) {
                        Timber.e(authorizationException, "OAuth request to refresh access token failed");
                        accountAuthenticatorResponse.onError(authorizationException.code, authorizationException.error);
                    }
                });
    }
}
