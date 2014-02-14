/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
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

import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.content.Context;

public class AccountUtils {
    public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
    public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    private static final String ODAV_PATH = "/remote.php/odav";
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
     * @param context
     * @param account
     * @return url or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    public static String constructFullURLForAccount(Context context, Account account) throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context);
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        String strver  = ama.getUserData(account, Constants.KEY_OC_VERSION);
        boolean supportsOAuth = (ama.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2) != null);
        boolean supportsSamlSso = (ama.getUserData(account, Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null);
        OwnCloudVersion ver = new OwnCloudVersion(strver);
        String webdavpath = getWebdavPath(ver, supportsOAuth, supportsSamlSso);

        if (baseurl == null || webdavpath == null) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl + webdavpath;
    }
    
    /**
     * Extracts url server from the account
     * @param context
     * @param account
     * @return url server or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    public static String constructBasicURLForAccount(Context context, Account account) throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context);
        String baseurl = ama.getUserData(account, Constants.KEY_OC_BASE_URL);
        
        if (baseurl == null ) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl;
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
	     * {@link com.owncloud.android.lib.resources.status.utils.OwnCloudVersion}
	     * 
	     * @deprecated
	     */
	    public static final String KEY_OC_URL = "oc_url";
	    /**
	     * Version should be 3 numbers separated by dot so it can be parsed by
	     * {@link com.owncloud.android.lib.resources.status.utils.OwnCloudVersion}
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
	    */
	    public static final String KEY_SUPPORTS_SHARE_API = "oc_supports_share_api";
	}
}
