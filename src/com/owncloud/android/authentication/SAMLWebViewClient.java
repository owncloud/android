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

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 * Custom {@link WebViewClient} client aimed to catch the end of a single-sign-on process 
 * running in the {@link WebView} that is attached to.
 * 
 * Assumes that the single-sign-on is kept thanks to a cookie set at the end of the
 * authentication process.
 */
public class SAMLWebViewClient extends BaseWebViewClient {
        
    public interface SsoWebViewClientListener {
        void onSsoFinished(String sessionCookie);
    }
    
    private WeakReference<SsoWebViewClientListener> mListenerRef;

    
    public SAMLWebViewClient(Context context, Handler listenerHandler, SsoWebViewClientListener listener) {
        super(context, listenerHandler);
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    protected void onTargetUrlFinished(WebView view, String url) {
        view.setVisibility(View.GONE);
        CookieManager cookieManager = CookieManager.getInstance();
        final String cookies = cookieManager.getCookie(url);
        if (mListenerHandler != null && mListenerRef != null) {
            // this is good idea because onPageFinished is not running in the UI thread
            mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    SsoWebViewClientListener listener = mListenerRef.get();
                    if (listener != null) {
                        // Send Cookies to the listener
                        listener.onSsoFinished(cookies);
                    }
                }
            });
        }
    }

}
