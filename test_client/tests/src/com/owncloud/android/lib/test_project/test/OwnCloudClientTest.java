/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.lib.test_project.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import junit.framework.AssertionFailedError;

import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;


/**
 * Unit test for OwnCloudClient
 * 
 * @author David A. Velasco
 */
public class OwnCloudClientTest extends AndroidTestCase {
	
	private static final String TAG = OwnCloudClientTest.class.getSimpleName();
	
	private Uri mServerUri;
	private String mUsername;
	private String mPassword;

	public OwnCloudClientTest() {
		super();
		
		Protocol pr = Protocol.getProtocol("https");
		if (pr == null || !(pr.getSocketFactory() instanceof SelfSignedConfidentSslSocketFactory)) {
			try {
				ProtocolSocketFactory psf = new SelfSignedConfidentSslSocketFactory();
				Protocol.registerProtocol(
						"https",
						new Protocol("https", psf, 443));
				
			} catch (GeneralSecurityException e) {
				throw new AssertionFailedError(
						"Self-signed confident SSL context could not be loaded");
			}
		}
	}
	

	@Override
	protected void setUp() throws Exception {
	    super.setUp();
		mServerUri = Uri.parse(getContext().getString(R.string.server_base_url));
		mUsername = getContext().getString(R.string.username);
		mPassword = getContext().getString(R.string.password);
	}
	
	
	public void testConstructor() {
		try {
			new OwnCloudClient(null, NetworkUtils.getMultiThreadedConnManager());
			throw new AssertionFailedError("Accepted NULL parameter");
			
		} catch(Exception e) {
			assertTrue("Unexpected exception passing NULL baseUri", 
					(e instanceof IllegalArgumentException));
		}
		
		try {
			new OwnCloudClient(mServerUri, null);
			throw new AssertionFailedError("Accepted NULL parameter");
			
		} catch(Exception e) {
			assertTrue("Unexpected exception passing NULL connectionMgr", 
					(e instanceof IllegalArgumentException));
		}
		
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		assertNotNull("OwnCloudClient instance not built", client);
	}

	
	public void testGetSetCredentials() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		
		assertNotNull("Returned NULL credentials", client.getCredentials());
		assertEquals("Not instanced without credentials", 
				client.getCredentials(), OwnCloudCredentialsFactory.getAnonymousCredentials());
		
		OwnCloudCredentials credentials = 
				OwnCloudCredentialsFactory.newBasicCredentials("user", "pass");
		client.setCredentials(credentials);
		assertEquals("Basic credentials not set", credentials, client.getCredentials());
		
		credentials = OwnCloudCredentialsFactory.newBearerCredentials("bearerToken");
		client.setCredentials(credentials);
		assertEquals("Bearer credentials not set", credentials, client.getCredentials());

		credentials = OwnCloudCredentialsFactory.newSamlSsoCredentials("user", "samlSessionCookie=124");
		client.setCredentials(credentials);
		assertEquals("SAML2 session credentials not set", credentials, client.getCredentials());
		
	}
    
	public void testExecuteMethodWithTimeouts() throws HttpException, IOException {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		int connectionTimeout = client.getConnectionTimeout();
		int readTimeout = client.getDataTimeout();
		
        HeadMethod head = new HeadMethod(client.getWebdavUri() + "/");
        try {
            client.executeMethod(head, 1, 1000);
			throw new AssertionFailedError("Completed HEAD with impossible read timeout");
            
        } catch (Exception e) {
            Log.e("OwnCloudClientTest", "EXCEPTION", e);
            assertTrue("Unexcepted exception " + e.getLocalizedMessage(), 
            		(e instanceof ConnectTimeoutException) || 
            		(e instanceof SocketTimeoutException));
            
        } finally {
            head.releaseConnection();
        }
        
        assertEquals("Connection timeout was changed for future requests", 
        		connectionTimeout, client.getConnectionTimeout());
        assertEquals("Read timeout was changed for future requests", 
        		readTimeout, client.getDataTimeout());
        
        try {
            client.executeMethod(head, 1000, 1);
			throw new AssertionFailedError("Completed HEAD with impossible connection timeout");
            
        } catch (Exception e) {
            Log.e("OwnCloudClientTest", "EXCEPTION", e);
            assertTrue("Unexcepted exception " + e.getLocalizedMessage(), 
            		(e instanceof ConnectTimeoutException) || 
            		(e instanceof SocketTimeoutException));
            
        } finally {
            head.releaseConnection();
        }
        
        assertEquals("Connection timeout was changed for future requests", 
        		connectionTimeout, client.getConnectionTimeout());
        assertEquals("Read timeout was changed for future requests", 
        		readTimeout, client.getDataTimeout());
        
	}
    
    
	public void testExecuteMethod() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
        HeadMethod head = new HeadMethod(client.getWebdavUri() + "/");
        int status = -1;
        try {
            status = client.executeMethod(head);
            assertTrue("Wrong status code returned: " + status, 
            		status > 99 && status < 600);
            
        } catch (IOException e) {
        	Log.e(TAG, "Exception in HEAD method execution", e);
        	// TODO - make it fail? ; try several times, and make it fail if none
        	//			is right?
            
        } finally {
            head.releaseConnection();
        }
	}

	
	public void testExhaustResponse() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());

		PropFindMethod propfind = null;
		try {
			propfind = new PropFindMethod(client.getWebdavUri() + "/",
					DavConstants.PROPFIND_ALL_PROP,
					DavConstants.DEPTH_0);
			client.executeMethod(propfind);
			InputStream responseBody = propfind.getResponseBodyAsStream();
			if (responseBody != null) {
				client.exhaustResponse(responseBody);

				try {
					int character = responseBody.read();
					assertEquals("Response body was not fully exhausted", 
							character, -1);		// -1 is acceptable
					
				} catch (IOException e) {
					// this is the preferred result
				}
				
			} else {
	        	Log.e(TAG, "Could not test exhaustResponse due to wrong response");
	        	// TODO - make it fail? ; try several times, and make it fail if none
	        	//			is right?
			}
	            
        } catch (IOException e) {
        	Log.e(TAG, "Exception in PROPFIND method execution", e);
        	// TODO - make it fail? ; try several times, and make it fail if none
        	//			is right?
            
        } finally {
            propfind.releaseConnection();
        }
		
		client.exhaustResponse(null);	// must run with no exception
	}

	
	public void testGetSetDefaultTimeouts() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		
		int oldDataTimeout = client.getDataTimeout();
		int oldConnectionTimeout = client.getConnectionTimeout();
		
		client.setDefaultTimeouts(oldDataTimeout + 1000, oldConnectionTimeout + 1000);
		assertEquals("Data timeout not set", 
				oldDataTimeout + 1000, client.getDataTimeout());
		assertEquals("Connection timeout not set", 
				oldConnectionTimeout + 1000, client.getConnectionTimeout());
		
		client.setDefaultTimeouts(0, 0);
		assertEquals("Zero data timeout not set", 
				0, client.getDataTimeout());
		assertEquals("Zero connection timeout not set", 
				0, client.getConnectionTimeout());
			
		client.setDefaultTimeouts(-1, -1);
		assertEquals("Negative data timeout not ignored", 
				0, client.getDataTimeout());
		assertEquals("Negative connection timeout not ignored", 
				0, client.getConnectionTimeout());
		
		client.setDefaultTimeouts(-1, 1000);
		assertEquals("Negative data timeout not ignored", 
				0, client.getDataTimeout());
		assertEquals("Connection timeout not set", 
				1000, client.getConnectionTimeout());
		
		client.setDefaultTimeouts(1000, -1);
		assertEquals("Data timeout not set", 
				1000, client.getDataTimeout());
		assertEquals("Negative connection timeout not ignored", 
				1000, client.getConnectionTimeout());
			
	}
	
	
	public void testGetWebdavUri() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		client.setCredentials(OwnCloudCredentialsFactory.newBearerCredentials("fakeToken"));
		Uri webdavUri = client.getWebdavUri();
		assertTrue("WebDAV URI does not point to the right entry point for OAuth2 " +
				"authenticated servers",
				webdavUri.getPath().endsWith(AccountUtils.ODAV_PATH));
		assertTrue("WebDAV URI is not a subpath of base URI", 
				webdavUri.getAuthority().equals(mServerUri.getAuthority()) &&
				webdavUri.getPath().startsWith(mServerUri.getPath()));
		
		client.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(
				mUsername, mPassword));
		webdavUri = client.getWebdavUri();
		assertTrue("WebDAV URI does not point to the right entry point",
				webdavUri.getPath().endsWith(AccountUtils.WEBDAV_PATH_4_0));
		PropFindMethod propfind = null;
		try {
			propfind = new PropFindMethod(webdavUri + "/",
					DavConstants.PROPFIND_ALL_PROP,
					DavConstants.DEPTH_0);
			int status = client.executeMethod(propfind);
			assertEquals("WebDAV request did not work on WebDAV URI", 
					HttpStatus.SC_MULTI_STATUS, status);
			
        } catch (IOException e) {
        	Log.e(TAG, "Exception in PROPFIND method execution", e);
        	// TODO - make it fail? ; try several times, and make it fail if none
        	//			is right?
            
        } finally {
            propfind.releaseConnection();
        }
		
	}
	
    
	public void testGetSetBaseUri() {
		OwnCloudClient client = 
				new OwnCloudClient(mServerUri, NetworkUtils.getMultiThreadedConnManager());
		assertEquals("Returned base URI different that URI passed to constructor", 
				mServerUri, client.getBaseUri());
		
		Uri otherUri = Uri.parse("https://whatever.com/basePath/here");
		client.setBaseUri(otherUri);
		assertEquals("Returned base URI different that URI passed to constructor", 
				otherUri, client.getBaseUri());
		
		try {
			client.setBaseUri(null);
			throw new AssertionFailedError("Accepted NULL parameter");
			
		} catch(Exception e) {
			assertTrue("Unexpected exception passing NULL base URI", 
					(e instanceof IllegalArgumentException));
		}
	}

	
	public void testGetCookiesString() {
		// TODO implement test body
		/*public String getCookiesString(){
			Cookie[] cookies = getState().getCookies(); 
			String cookiesString ="";
			for (Cookie cookie: cookies) {
				cookiesString = cookiesString + cookie.toString() + ";";
				
				logCookie(cookie);
			}
			
			return cookiesString;
			
		}
		*/
	}


	public void testSetFollowRedirects() {
		// TODO - to implement this test we need a redirected server
	}

    
}
