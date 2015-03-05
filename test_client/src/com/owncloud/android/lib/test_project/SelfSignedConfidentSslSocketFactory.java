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

package com.owncloud.android.lib.test_project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import com.owncloud.android.lib.common.network.AdvancedSslSocketFactory;


/**
 * SelfSignedConfidentSslSocketFactory allows to create SSL {@link Socket}s 
 * that accepts self-signed server certificates.
 * 
 * WARNING: this SHOULD NOT be used in productive environments.
 * 
 * @author David A. Velasco
 */

public class SelfSignedConfidentSslSocketFactory implements SecureProtocolSocketFactory {

	
	//private SSLContext mSslContext = null;
	private AdvancedSslSocketFactory mWrappedSslSocketFactory = null;
	
	
	/**
	 * Constructor for SelfSignedConfidentSslSocketFactory.
	 * @throws GeneralSecurityException 
	 */
	public SelfSignedConfidentSslSocketFactory() throws GeneralSecurityException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(
				null, 
				new TrustManager[] { new SelfSignedConfidentX509TrustManager() }, 
				null
		);
        mWrappedSslSocketFactory = new AdvancedSslSocketFactory(sslContext, null, null);		
	}

	
	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	 */
	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return mWrappedSslSocketFactory.createSocket(host, port);
	}
	
	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	 */
	@Override
	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
			throws IOException, UnknownHostException {
		return mWrappedSslSocketFactory.createSocket(host, port, clientHost, clientPort);
	}
	
	/**
	 * Attempts to get a new socket connection to the given host within the given time limit.
	 * 
	 * @param host 			The host name/IP
	 * @param port 			The port on the host
	 * @param clientHost 	The local host name/IP to bind the socket to
	 * @param clientPort 	The port on the local machine
	 * @param params 		{@link HttpConnectionParams} HTTP connection parameters.
	 * 
	 * @return Socket 		A new socket
	 * 
	 * @throws IOException if an I/O error occurs while creating the socket
	 * @throws UnknownHostException if the IP address of the host cannot be determined
	 */
	@Override
	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
			HttpConnectionParams params) throws IOException, UnknownHostException,
			ConnectTimeoutException {
		
		return mWrappedSslSocketFactory.createSocket(host, port, localAddress, localPort, params);
	}

	/**
	  * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	  */
	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
			throws IOException, UnknownHostException {
		return mWrappedSslSocketFactory.createSocket(socket, host, port, autoClose);
	}
	
	
	public static class SelfSignedConfidentX509TrustManager implements X509TrustManager {

	    private X509TrustManager mStandardTrustManager = null;

		public SelfSignedConfidentX509TrustManager() 
				throws NoSuchAlgorithmException, KeyStoreException, CertStoreException {
			super();
			TrustManagerFactory factory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore)null);
			mStandardTrustManager = findX509TrustManager(factory);
		}

		/**
		 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],String authType)
		 */
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			mStandardTrustManager.checkClientTrusted(chain, authType);
		}

		/**
		 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
		 *      String authType)
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			if (chain != null && chain.length == 1) {
				chain[0].checkValidity();
			} else {
				mStandardTrustManager.checkServerTrusted(chain, authType);
			}
		}

		/**
		 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
		 */
		public X509Certificate[] getAcceptedIssuers() {
			return mStandardTrustManager.getAcceptedIssuers();
		}
	
		/**
		 * Locates the first X509TrustManager provided by a given TrustManagerFactory
		 * @param factory               TrustManagerFactory to inspect in the search for a X509TrustManager
		 * @return                      The first X509TrustManager found in factory.
		 * @throws CertStoreException   When no X509TrustManager instance was found in factory
		 */
		private X509TrustManager findX509TrustManager(TrustManagerFactory factory) 
				throws CertStoreException {
			TrustManager tms[] = factory.getTrustManagers();
			for (int i = 0; i < tms.length; i++) {
				if (tms[i] instanceof X509TrustManager) {
					return (X509TrustManager) tms[i];
				}
			}
			return null;
		}
	}
	

}
