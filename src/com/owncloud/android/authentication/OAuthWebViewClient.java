/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;


/**
 * Custom {@link WebViewClient} client aimed to catch the end of a single-sign-on process 
 * running in the {@link WebView} that is attached to.
 * 
 * Assumes that the single-sign-on is kept thanks to an authentication code appended to the
 * query
 */
public class OAuthWebViewClient extends BaseWebViewClient {

    public interface OAuthWebViewClientListener {
        void onGetCapturedUriFromOAuth2Redirection (Uri capturedUriFromOAuth2Redirection);
        void onOAuthWebViewDialogFragmentDetached();
    }

    private WeakReference<OAuthWebViewClientListener> mListenerRef;
    private boolean mCapturedUriFromOAuth2Redirection;


    public OAuthWebViewClient(Context context, Handler listenerHandler,
                              OAuthWebViewClientListener listener) {
        super(context, listenerHandler);
        mListenerRef = new WeakReference<>(listener);
        mCapturedUriFromOAuth2Redirection = false;
    }


    @Override
    protected void onTargetUrlFinished(WebView view, String url) {
        if (!mCapturedUriFromOAuth2Redirection) {

            view.setVisibility(View.GONE);

            // Flag to avoid unneeded calls to the listener method once uri from oauth has been
            // captured, which mess up the login process
            mCapturedUriFromOAuth2Redirection = true;

            final Uri uri = Uri.parse(url);

            if (mListenerHandler != null && mListenerRef != null) {
                // this is good idea because onPageFinished is not running in the UI thread
                mListenerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OAuthWebViewClientListener listener = mListenerRef.get();
                        if (listener != null) {
                            // Send Cookies to the listener
                            listener.onGetCapturedUriFromOAuth2Redirection(uri);
                        }
                    }
                });
            }
        }
    }

}