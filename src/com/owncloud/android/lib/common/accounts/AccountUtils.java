/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2012  Bartek Przybylski
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.accounts;

import java.io.IOException;

import org.apache.commons.httpclient.Cookie;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

public class AccountUtils {
	
	private static final String TAG = AccountUtils.class.getSimpleName();
	
    public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
    public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    public static final String ODAV_PATH = "/remote.php/odav";
    private static final String SAML_SSO_PATH = "/remote.php/webdav";
    public static final String CARDDAV_PATH_2_0 = "/apps/contacts/carddav.php";
    public static final String CARDDAV_PATH_4_0 = "/remote/carddav.php";
    public static final String STATUS_PATH = "/status.php";

    /**
     * Returns the proper URL path to access the WebDAV interface of an ownCloud server,
     * according to its version and the authorization method used.
     * 
     * @param	version         	Version of ownCloud server.
     * @param 	supportsOAuth		If true, access with OAuth 2 authorization is considered. 
     * @param 	supportsSamlSso		If true, and supportsOAuth is false, access with SAML-based single-sign-on is considered.
     * @return 						WebDAV path for given OC version, null if OC version unknown
     */
    public static String getWebdavPath(OwnCloudVersion version, boolean supportsOAuth, boolean supportsSamlSso) {
        if (version != null) {
            if (supportsOAuth) {
                return ODAV_PATH;
            }
            if (supportsSamlSso) {
                return SAML_SSO_PATH;
            }
            if (version.compareTo(OwnCloudVersion.owncloud_v4) >= 0)
                return WEBDAV_PATH_4_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v3) >= 0
                    || version.compareTo(OwnCloudVersion.owncloud_v2) >= 0)
                return WEBDAV_PATH_2_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v1) >= 0)
                return WEBDAV_PATH_1_2;
        }
        return null;
    }
    
    /**
     * Constructs full url to host and webdav resource basing on host version
     * 
     * @deprecated 		To be removed in release 1.0. 
     * 
     * @param context
     * @param account
     * @return url or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    @Deprecated
    public static String constructFullURLForAccount(Context context, Account account) throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context);
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        String version  = ama.getUserData(account, Constants.KEY_OC_VERSION);
        boolean supportsOAuth = (ama.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2) != null);
        boolean supportsSamlSso = (ama.getUserData(account, Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null);
        OwnCloudVersion ver = new OwnCloudVersion(version);
        String webdavpath = getWebdavPath(ver, supportsOAuth, supportsSamlSso);

        if (baseurl == null || webdavpath == null) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl + webdavpath;
    }
    
    /**
     * Extracts url server from the account
     * 
     * @deprecated 	This method will be removed in version 1.0.
     *  			Use {@link #getBaseUrlForAccount(Context, Account)}
     *  		 	instead.   
     * 
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    @Deprecated
    public static String constructBasicURLForAccount(Context context, Account account) 
    		throws AccountNotFoundException {
    	return getBaseUrlForAccount(context, account);
    }

    /**
     * Extracts url server from the account
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    public static String getBaseUrlForAccount(Context context, Account account) 
    		throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        
        if (baseurl == null ) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl;
    }
    

    /**
     * 
     * @return
     * @throws IOException 
     * @throws AuthenticatorException 
     * @throws OperationCanceledException 
     */
	public static OwnCloudCredentials getCredentialsForAccount(Context context, Account account) 
			throws OperationCanceledException, AuthenticatorException, IOException {
		
		OwnCloudCredentials credentials = null;
        AccountManager am = AccountManager.get(context);
        
        boolean isOauth2 = am.getUserData(
        		account, 
        		AccountUtils.Constants.KEY_SUPPORTS_OAUTH2) != null;
        
        boolean isSamlSso = am.getUserData(
        		account, 
        		AccountUtils.Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null;

        String username = account.name.substring(0, account.name.lastIndexOf('@'));

        if (isOauth2) {    
            String accessToken = am.blockingGetAuthToken(
            		account, 
            		AccountTypeUtils.getAuthTokenTypeAccessToken(account.type), 
            		false);
            
            credentials = OwnCloudCredentialsFactory.newBearerCredentials(accessToken);
        
        } else if (isSamlSso) {
            String accessToken = am.blockingGetAuthToken(
            		account, 
            		AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type), 
            		false);
            
            credentials = OwnCloudCredentialsFactory.newSamlSsoCredentials(username, accessToken);

        } else {
            String password = am.blockingGetAuthToken(
            		account, 
            		AccountTypeUtils.getAuthTokenTypePass(account.type), 
            		false);
            
            credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password);
        }
        
        return credentials;
        
	}

	
    public static String buildAccountNameOld(Uri serverBaseUrl, String username) {
    	if (serverBaseUrl.getScheme() == null) {
    		serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString()); 
    	}
        String accountName = username + "@" + serverBaseUrl.getHost();
        if (serverBaseUrl.getPort() >= 0) {
            accountName += ":" + serverBaseUrl.getPort();
        }
        return accountName;
    }

    public static String buildAccountName(Uri serverBaseUrl, String username) {
    	if (serverBaseUrl.getScheme() == null) {
    		serverBaseUrl = Uri.parse("https://" + serverBaseUrl.toString());
    	}

        // Remove http:// or https://
        String url = serverBaseUrl.toString();
        if (url.contains("://")) {
            url = url.substring(serverBaseUrl.toString().indexOf("://") + 3);
        }
        String accountName = username + "@" + url;

        return accountName;
    }

	public static void saveClient(OwnCloudClient client, Account savedAccount, Context context) {

		// Account Manager
		AccountManager ac = AccountManager.get(context.getApplicationContext());

		if (client != null) {
			String cookiesString = client.getCookiesString();
			if (!"".equals(cookiesString)) {
				ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString); 
				// Log_OC.d(TAG, "Saving Cookies: "+ cookiesString );
			}
		}

	}
	
	
  /**
  * Restore the client cookies
  * @param account
  * @param client 
  * @param context
  */
	public static void restoreCookies(Account account, OwnCloudClient client, Context context) {

		Log_OC.d(TAG, "Restoring cookies for " + account.name);

		// Account Manager
		AccountManager am = AccountManager.get(context.getApplicationContext());

		Uri serverUri = (client.getBaseUri() != null)? client.getBaseUri() : client.getWebdavUri();

		String cookiesString = am.getUserData(account, Constants.KEY_COOKIES);
		if (cookiesString !=null) {
			String[] cookies = cookiesString.split(";");
			if (cookies.length > 0) {
				for (int i=0; i< cookies.length; i++) {
					Cookie cookie = new Cookie();
					int equalPos = cookies[i].indexOf('=');
					cookie.setName(cookies[i].substring(0, equalPos));
					cookie.setValue(cookies[i].substring(equalPos + 1));
					cookie.setDomain(serverUri.getHost());	// VERY IMPORTANT 
					cookie.setPath(serverUri.getPath());	// VERY IMPORTANT

					client.getState().addCookie(cookie);
				}
			}
		}
	}
	
	/**
	 * Restore the client cookies from accountName
	 * @param accountName
	 * @param client
	 * @param context
	 */
	public static void restoreCookies(String accountName, OwnCloudClient client, Context context) {
		Log_OC.d(TAG, "Restoring cookies for " + accountName);

		// Account Manager
		AccountManager am = AccountManager.get(context.getApplicationContext());
		
		// Get account
		Account account = null;
		Account accounts[] = am.getAccounts();
		for (Account a : accounts) {
			if (a.name.equals(accountName)) {
				account = a;
				break;
			}
		}
		
		// Restoring cookies
		if (account != null) {
			restoreCookies(account, client, context);
		}
	}
	
    public static class AccountNotFoundException extends AccountsException {
        
		/** Generated - should be refreshed every time the class changes!! */
		private static final long serialVersionUID = -1684392454798508693L;
        
        private Account mFailedAccount; 
                
        public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
            super(message, cause);
            mFailedAccount = failedAccount;
        }
        
        public Account getFailedAccount() {
            return mFailedAccount;
        }
    }


	public static class Constants {
	    /**
	     * Value under this key should handle path to webdav php script. Will be
	     * removed and usage should be replaced by combining
	     * {@link com.owncloud.android.authentication.AuthenticatorActivity.KEY_OC_BASE_URL} and
	     * {@link com.owncloud.android.lib.resources.status.OwnCloudVersion}
	     * 
	     * @deprecated
	     */
	    public static final String KEY_OC_URL = "oc_url";
	    /**
	     * Version should be 3 numbers separated by dot so it can be parsed by
	     * {@link com.owncloud.android.lib.resources.status.OwnCloudVersion}
	     */
	    public static final String KEY_OC_VERSION = "oc_version";
	    /**
	     * Base url should point to owncloud installation without trailing / ie:
	     * http://server/path or https://owncloud.server
	     */
	    public static final String KEY_OC_BASE_URL = "oc_base_url";
	    /**
	     * Flag signaling if the ownCloud server can be accessed with OAuth2 access tokens.
	     */
	    public static final String KEY_SUPPORTS_OAUTH2 = "oc_supports_oauth2";
	    /**
	     * Flag signaling if the ownCloud server can be accessed with session cookies from SAML-based web single-sign-on.
	     */
	    public static final String KEY_SUPPORTS_SAML_WEB_SSO = "oc_supports_saml_web_sso";
	    /**
	    * Flag signaling if the ownCloud server supports Share API"
        * @deprecated
        */
	    public static final String KEY_SUPPORTS_SHARE_API = "oc_supports_share_api";
	    /**
	     * OC account cookies
	     */
	    public static final String KEY_COOKIES = "oc_account_cookies";

        /**
         * OC account version
         */
        public static final String KEY_OC_ACCOUNT_VERSION = "oc_account_version";
    }

}
