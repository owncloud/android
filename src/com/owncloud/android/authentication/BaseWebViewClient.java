/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.authentication;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 * Custom {@link WebViewClient} client aimed to catch the end of an authentication
 * process running in the {@link WebView} that is attached to.
 *
 * Assumes that the authentication process is successful when the {@link WebView} loads
 * a URL matching a target URL set initially.
 */
public abstract class BaseWebViewClient extends WebViewClient {

    private static final String TAG = SAMLWebViewClient.class.getSimpleName();

    private Context mContext;
    Handler mListenerHandler;
    private String mTargetUrl;
    private String mLastReloadedUrlAtError;

    public BaseWebViewClient (Context context, Handler listenerHandler) {
        mContext = context;
        mListenerHandler = listenerHandler;
        mTargetUrl = "fake://url.to.be.set";
        mLastReloadedUrlAtError = null;
    }

    public String getTargetUrl() {
        return mTargetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        mTargetUrl = targetUrl;
    }

    @Override
    public void onPageStarted (WebView view, String url, Bitmap favicon) {
        Log_OC.d(TAG, "onPageStarted : " + url);
        view.clearCache(true);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onFormResubmission (WebView view, Message dontResend, Message resend) {
        Log_OC.d(TAG, "onFormResubMission ");

        // necessary to grant reload of last page when device orientation is changed after sending a form
        resend.sendToTarget();
    }

    @Override @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override @RequiresApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    @Override @SuppressWarnings("deprecation")
    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
        Log_OC.e(TAG, "onReceivedError : " + failingUrl + ", code " + errorCode + ", description: " + description);
        if (!failingUrl.equals(mLastReloadedUrlAtError)) {
            view.reload();
            mLastReloadedUrlAtError = failingUrl;
        } else {
            mLastReloadedUrlAtError = null;
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    @Override @RequiresApi(Build.VERSION_CODES.M)
    public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error) {
        String failingUrl = request.getUrl().toString();
        Log_OC.e(TAG, "onReceivedError : " + failingUrl + ", code " + error.getErrorCode() + ", description: " + error.getDescription());
        if (!failingUrl.equals(mLastReloadedUrlAtError)) {
            view.reload();
            mLastReloadedUrlAtError = failingUrl;
        } else {
            mLastReloadedUrlAtError = null;
            super.onReceivedError(view, request, error);
        }
    }

    @Override
    public void onReceivedSslError (final WebView view, final SslErrorHandler handler, SslError error) {
        Log_OC.e(TAG, "onReceivedSslError : " + error);
        // Test 1
        X509Certificate x509Certificate = getX509CertificateFromError(error);
        boolean isKnownServer = false;

        if (x509Certificate != null) {
            try {
                isKnownServer = NetworkUtils.isCertInKnownServersStore(x509Certificate, mContext);
            } catch (Exception e) {
                Log_OC.e(TAG, "Exception: " + e.getMessage());
            }
        }

        if (isKnownServer) {
            handler.proceed();
        } else {
            ((AuthenticatorActivity)mContext).showUntrustedCertDialog(x509Certificate, error, handler);
        }
    }

    /**
     * Obtain the X509Certificate from SslError
     * @param   error     SslError
     * @return  X509Certificate from error
     */
    public X509Certificate getX509CertificateFromError (SslError error) {
        Bundle bundle = SslCertificate.saveState(error.getCertificate());
        X509Certificate x509Certificate;
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            x509Certificate = null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                x509Certificate = (X509Certificate) cert;
            } catch (CertificateException e) {
                x509Certificate = null;
            }
        }
        return x509Certificate;
    }

    @Override
    public void onReceivedHttpAuthRequest (WebView view, HttpAuthHandler handler, String host, String realm) {
        Log_OC.d(TAG, "onReceivedHttpAuthRequest : " + host);

        ((AuthenticatorActivity)mContext).createAuthenticationDialog(view, handler);
    }


    @Override
    public void onPageFinished (WebView view, String url) {
        Log_OC.d(TAG, "onPageFinished : " + url);
        mLastReloadedUrlAtError = null;
        if (url.startsWith(getTargetUrl())) {
            onTargetUrlFinished(view, url);
        }
    }

    protected abstract void onTargetUrlFinished(WebView view, String url);

}
